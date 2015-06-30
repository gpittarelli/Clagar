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

(def ^{:private} token-proto-ver 154669603)
(defn get-token [& {:keys [region gamemode]
                    :or {gamemode "FFA"}}]
  (let [gamemode-str (if (= gamemode "FFA") "" (str ":" gamemode))
        payload (str region gamemode_str "\n" token-proto-ver)
        out-chan (chan)
        req (d/chain' (http/post "http://m.agar.io/"
                                 {:headers agar-headers
                                  :body payload})
                      :body
                      bs/to-string
                      #(str/split #"\n" %))]
    (s/connect req out-chan)
    out-chan))

;; (def servers
;;   (-> @(http/get "http://m.agar.io/info")
;;       :body
;;       bs/to-string
;;       (json/read-str :keywordize? false)
;;       (get "regions")
;;       keys
;;       ((partial map #(str/split % #":")))))

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
