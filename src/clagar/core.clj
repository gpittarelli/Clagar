(ns clagar.core
  (:gen-class)
  (:require [aleph.http :as http]
            [byte-streams :as bs]
            [manifold.deferred :as d]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [clagar.gui.core :refer :all]))


(defn -main
  "Run primary Clagar client"
  [& args]
  (show-game-window))
