(ns clagar.proto.meta.agar
  (:require [aleph.http :as http]
            [byte-streams :as bs]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [clojure.set :refer [map-invert]]
            [gniazdo.core :as ws]
            [org.clojars.smee.binary.core :as b]
            [clojure.core.async
             :as async
             :refer [>! >!! <! <!! chan go go-loop close!]])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]))

(def agar-headers { "Origin" "http://agar.io"
                    "Referer" "http://agar.io" })

(defn get-regions []
  (let [out-chan (chan)
        req (d/chain' (http/get "http://m.agar.io/info")
                      :body
                      bs/to-string
                      #(json/read-str % :keywordize? false)
                      #(get % "regions")
                      keys)]
    (s/connect req out-chan)
    out-chan))

(def ^:private token-proto-ver 154669603)
(defn get-token
  ([region] (get-token region "FFA"))
  ([region gamemode]
   (let [gamemode-str (if (= gamemode "FFA") "" (str ":" gamemode))
         payload (str region gamemode-str "\n" token-proto-ver)
         out-chan (chan)
         req (d/chain' (http/post "http://m.agar.io/"
                                  {:headers agar-headers
                                   :body payload})
                       :body
                       bs/to-string
                       #(str/split % #"\n"))]
     (s/connect req out-chan)
     out-chan)))

;; copied graciously from binary-dsl library's png example
(defn skip [len]
  (reify org.clojars.smee.binary.core.BinaryIO
    (read-data  [_ big-in little-in]
      (.skipBytes big-in len) :skipped)
    (write-data [_ big-out little-out script])))

;; modification of c-string from binary-dsl library
(def ^:private agar-string
  "Zero-terminated string, where each character (and the null
  terminator) are 2 bytes.."
  (b/compile-codec
   (b/repeated :short-le :separator 0)
   (fn string2bytes [^String s] (.getBytes s))
   #(String. (byte-array %) "UTF-8")))

(def ^:private blob-info
  (reify org.clojars.smee.binary.core.BinaryIO
    (read-data  [_ big-in little-in]
      (let [id (b/read-data :uint-le big-in little-in)]
        (when (pos? id)
          (let [r1 (b/read-data
                    (b/ordered-map
                     :x :short-le
                     :y :short-le
                     :size :short-le
                     :color (b/ordered-map :r :ubyte :g :ubyte :b :ubyte)
                     :flags (b/bits [:virus? 4 8 16 :agitated?]))
                    big-in
                    little-in)
                skip-n (reduce + (filter number? (:flags r1)))]
            (when (pos? skip-n) (b/read-data (skip skip-n) big-in little-in))
            (assoc r1
                   :name (b/read-data agar-string big-in little-in)
                   :id id)))))

    (write-data [_ big-out little-out val])))

(def ^:private type->opcode
  {:blob-updates 16
   :view-update 17
   :reset 20
   :draw-debug-line 21
   :owns-blob 32
   :ffa-leaders 49
   :team-leaders 50
   :game-area 64

   :spectate 1
   :spawn 0 })
(def ^:private opcode->type (map-invert type->opcode))

(def ^:private type->format
  {:blob-updates
   (b/ordered-map
    :eats (b/repeated (b/ordered-map :attacker-id :uint-le
                                     :victim-id :uint-le)
                      :prefix :ushort-le)

    :updates (b/repeated blob-info :separator nil)

    :deletes (b/repeated (b/ordered-map :id :uint-le)
                         :prefix :uint-le))

   :view-update (b/ordered-map :x :float-le :y :float-le :zoom :float-le)
   :reset []
   :draw-debug-line (b/ordered-map :x :short-le :y :short-le)
   :owns-blob (b/ordered-map :id :uint-le)
   :ffa-leaders (b/ordered-map :players
                               (b/repeated
                                (b/ordered-map :id :uint-le
                                               :name agar-string)
                                :prefix :uint-le))
   :team-leaders []
   :game-area (b/ordered-map :min-x :double-le :min-y :double-le
                             :max-x :double-le :max-y :double-le)

   :spectate [] })

(def agar-codec
  (reify org.clojars.smee.binary.core.BinaryIO
    (read-data [_ big-in little-in]
      (let [opcode (b/read-data :ubyte big-in little-in)
            type (opcode->type opcode)
            body-codec (type->format type)
            body (b/read-data body-codec big-in little-in)]
        (assoc body :type type)))

    (write-data [_ big-out little-out val]
      (let [opcode (type->opcode (:type val))
            body-codec (type->format (:type val))]
        (b/write-data [:ubyte body-codec] big-out little-out [opcode val])))))


(defn decode-transducer [xf]
  (fn
    ([] (xf))
    ([result] (xf result))
    ([result input-stream]
     (let [decoded (b/decode agar-codec input-stream)]
       (when decoded (xf result decoded))))))

;; Having to use a separate library here (aleph for the normal http vs
;; gniazdo for websockets, despite aleph having websocket support
;; is sad, but it's the easiest way around the fact that aleph can't
;; connect to agario games because it lowercases all headers, which
;; agario's servers don't properly respect.
;;
;; Returns 2 channels:
(defn ws-connect [url token]
  (let [received (chan) ; from ws -> parser
        incoming (chan) ; from parser -> user

        outgoing (chan) ; from user -> encoder -> ws

        control (chan) ; messages from user to control the socket

        closed? (atom false)
        s (ws/connect (str "ws://" url)
                :headers (assoc agar-headers "Host" url)
                :on-binary #(>!! received (new ByteArrayInputStream %1 %2 %3))
                :on-close #(reset! closed? true))]

    (let [os (new ByteArrayOutputStream)]
      (go-loop []
        (let [m (<! outgoing)]
          (b/encode agar-codec os m)
          (ws/send-msg s (.toByteArray os))
          (.reset os))
        (when (not @closed?) (recur))))

;;     (go-loop []
;;       (let [input-stream (<! received)
;;             m (b/decode agar-codec input-stream)]
;;         (when m (>! incoming m)))
    ;;      (when (not @closed?) (recur)))
    (async/pipeline 1 incoming decode-transducer received)

    (go-loop []
      (let [m (<! control)]
        (if (= m :close)
          (do
            (ws/close s)
            (map close! [incoming outgoing received control]))
          (recur))))

    (let [os (new ByteArrayOutputStream)]
      (b/encode [:ubyte :uint-le] os [254 4])
      (ws/send-msg s (.toByteArray os))
      (.reset os)

      (b/encode [:ubyte :uint-le] os [255 154669603])
      (ws/send-msg s (.toByteArray os))
      (.reset os)

      (b/encode [:ubyte (b/blob)] os [80 (.getBytes token)])
      (ws/send-msg s (.toByteArray os)))
    [incoming outgoing control]))
