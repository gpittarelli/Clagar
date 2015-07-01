(ns clagar.proto.meta.agar
  (:require [aleph.http :as http]
            [byte-streams :as bs]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [clojure.core.async
             :as async
             :refer [>! >!! <! <!! chan go]]))

(def agar-headers { "Origin" "http://agar.io"
                    "Referer" "http://agar.io" })

(def ws-headers
  {
"User-Agent" "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:38.0) Gecko/20100101 Firefox/38.0"
"Accept" "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
"Accept-Language" "en-US,en;q=0.5"
"Accept-Encoding" "gzip, deflate"
"Sec-WebSocket-Version" "13"
"Origin" "http://agar.io"
"Sec-WebSocket-Extensions" "permessage-deflate"
"Connection" "keep-alive, Upgrade"
"Pragma" "no-cache"
"Cache-Control" "no-cache"
"Upgrade" "websocket"
})
;; "Sec-WebSocket-Key": "HVbz2J3Da1eVT+Xmxrd6+g==",

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
(defn get-token [& {:keys [region gamemode]
                    :or {gamemode "FFA"}}]
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
    out-chan))

(defn ws-connect [url token]
  (http/websocket-client (str "ws://" url) {:headers (assoc
                                                      ws-headers
                                                      "Host"
                                                      url)}))

;; (def region-names (set (map first servers)))
;; (def gamemodes
;;   ;; get a list of [region gamemode] pairs. Some pairs will be missing
;;   ;; the gamemode value (for the default gamemode, FFA)
;;   (->> servers
;;        (map second)
;;        set
;;        (remove nil?)))

  ;;   (let [[url ticket] (-> @(http/post "http://m.agar.io/"
  ;;                                      {:body "US-Atlanta"
  ;;                                       :headers agar-headers })
  ;;                          :body
  ;;                          bs/to-string
  ;;                          (str/split #"\n"))]
  ;;     (println (str "connect(\"ws://" url "\",\""
  ;;                   (str/replace ticket "\\" "\\\\")
  ;;                   "\")")))
