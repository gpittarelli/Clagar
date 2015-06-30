(ns clagar.core
  (:gen-class)
  (:require [clagar.gui.core :refer :all]))

(defn -main
  "Run primary Clagar client"
  [& args]
  (create-game-window))
