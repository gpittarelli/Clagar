(ns clagar.proto.meta.agar)

(def servers
  (-> @(http/get "http://m.agar.io/info")
      :body
      bs/to-string
      (json/read-str :keywordize? false)
      (get "regions")
      keys
      ((partial map #(str/split % #":")))))

(def region-names (set (map first servers)))
(def gamemodes
  ;; get a list of [region gamemode] pairs. Some pairs will be missing
  ;; the gamemode value (for the default gamemode, FFA)
  (->> servers
       (map second)
       set
       (remove nil?)))


  ;;   (def agar-headers { "Origin" "http://agar.io"
  ;;                       "Referer" "http://agar.io" })

  ;;   (let [[url ticket] (-> @(http/post "http://m.agar.io/"
  ;;                                      {:body "US-Atlanta"
  ;;                                       :headers agar-headers })
  ;;                          :body
  ;;                          bs/to-string
  ;;                          (str/split #"\n"))]
  ;;     (println (str "connect(\"ws://" url "\",\""
  ;;                   (str/replace ticket "\\" "\\\\")
  ;;                   "\")")))
