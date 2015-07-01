(ns clagar.proto.game
  (:require [clagar.proto.meta.agar :refer [ws-connect get-token]]
            [aleph.http :as http]
            [byte-streams :as bs]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [clojure.string :as str]
            [clojure.core.async
             :as async
             :refer [>! >!! <! <!! chan go]]))

(deftype GameConnection [state chan])

(defn connect [region gamemode]
  (let [state (atom {})
        c (chan)]
    (go
      (let [[url token] (<! (get-token :region region
                                       :gamemode gamemode))
            ws (ws-connect url token)
            ]
        (prn url token)))
    (GameConnection. state c)))
