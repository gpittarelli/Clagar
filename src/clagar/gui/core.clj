(ns clagar.gui.core
  (:require [seesaw.core :refer :all]
            [clagar.gui.game :refer :all])
  (:import  [javax.swing JOptionPane]
            [java.awt Canvas Graphics]
            [org.lwjgl LWJGLException]
            [org.lwjgl.opengl Display DisplayMode GL11]))


(def game-running (atom true))
(def game-canvas (new Canvas))

(def regions-menu
  (menu :text "Regions" :items [(action :name "Loading...")]))

(def game-options
  (menu :text "Options" :items [(action :name "Exit")]))

(def connect-button
  (button :text "Connect" :listen [:action #(alert % "NEXT!")]))

(def game-menu
  (menubar :items [regions-menu
                   game-options
                   (text :text "connect to")
                   connect-button]))

(def game-window
  (frame :title "Clagar"
         :size [640 :by 480]
         :on-close :nothing
         :content (border-panel :center game-canvas)
         :menubar game-menu
         ))

(defn show-game-window []
  (invoke-later (show! game-window))

  (listen game-window
          :window-closing
          (fn [e]
            (reset! game-running false)
            (dispose! game-window))))


;; [border-panel frame menubar
;;                                  menu invoke-later show! action
;;                                  listen dispose! alert button]

;; (def fetching-diag
;;   (.createDialog
;;    (new JOptionPane
;;         "Fetching regions list..."
;;         JOptionPane/DEFAULT_OPTION
;;         JOptionPane/INFORMATION_MESSAGE
;;         nil
;;         (into-array ["Cancel"])
;;         "Cancel")
;;    nil "Clajar"))

;;(invoke-later (show! fetching-diag))
;; (comment
;;   (dispose! region-chooser)
;;   (dispose! fetching-diag)

;;   (hide! fetching-diag))

;; (def region-buttons
;;   (mapv #(action :name % :handler (fn [e] (prn %)
;;                                     (dispose! region-chooser)))
;;         region-names))

;; (def region-chooser
;;   (pack! (custom-dialog :title "HI!"
;;                         :content
;;                         (vertical-panel
;;                          :items region-buttons))))
