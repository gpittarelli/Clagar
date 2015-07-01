(ns clagar.gui.core
  (:require [seesaw.core :refer [menu action button menubar text alert frame
                                 show! hide! dispose! border-panel remove!
                                 add! listen invoke-later radio-menu-item
                                 button-group selection selection! config]]
            [clagar.gui.game :refer :all]
            [clagar.proto.game :refer [connect]]
            [clagar.proto.meta.agar :refer [get-regions]]
            [clojure.core.async
             :as async
             :refer [>! >!! <! <!! chan go]])
  (:import  [javax.swing JOptionPane]
            [java.awt Canvas Graphics]
            [org.lwjgl LWJGLException]
            [org.lwjgl.opengl Display DisplayMode GL11 AWTGLCanvas]))


(defn- render [w h state]
  (GL11/glMatrixMode GL11/GL_PROJECTION)
  (GL11/glLoadIdentity)
  (GL11/glViewport 0 0 w h)
  (GL11/glOrtho 0 w 0 h 1 -1)

  (GL11/glClear (bit-or GL11/GL_COLOR_BUFFER_BIT GL11/GL_DEPTH_BUFFER_BIT))
  (GL11/glColor3f 0.5 0.5 1.0)

  (GL11/glBegin GL11/GL_QUADS)
  (GL11/glVertex2f 0 0)
  (GL11/glVertex2f 300 100)
  (GL11/glVertex2f 300 300)
  (GL11/glVertex2f 100 300)
  (GL11/glEnd)

  (GL11/glColor3f 1.0 0.0 0.0)
  (doseq [i (range -2000 2000 50)]
    (GL11/glBegin GL11/GL_QUADS)
    (GL11/glVertex2f (- i 10) (- i 10))
    (GL11/glVertex2f (+ i 10) (- i 10))
    (GL11/glVertex2f (+ i 10) (+ i 10))
    (GL11/glVertex2f (- i 10) (+ i 10))
    (GL11/glEnd))

  (GL11/glBegin GL11/GL_LINE_LOOP)
  (GL11/glVertex2f 5 5)
  (GL11/glVertex2f (- w 5) 5)
  (GL11/glVertex2f (- w 5) (- h 5))
  (GL11/glVertex2f 5 (- h 5))
  (GL11/glEnd))

(defn create-game-window []
  (let [game-running (atom true)
        game (atom nil)

        regions-menu (menu :text "Regions" :items [(action :name "Loading...")])
        region-buttons (button-group)

        gamemode-menu (menu :text "Gamemode")
        gamemode-buttons (button-group)

        game-options (menu :text "Options" :items [(action :name "Exit")])
        connect-button (button :text "Connect")

        game-menu (menubar :items [regions-menu
                                   gamemode-menu
                                   game-options
                                   (text :text "connect to")
                                   connect-button])

        game-canvas (proxy
                        [AWTGLCanvas] []
                      (paintGL []
                        (render (proxy-super getWidth)
                                (proxy-super getHeight)
                                {})
                        (proxy-super swapBuffers)))

        game-window (frame :title "Clagar"
                           :size [640 :by 480]
                           :on-close :nothing
                           :content (border-panel :center game-canvas)
                           :menubar game-menu)]

    ;; Fill in regions menu
    ;; TODO: error handling
    (go
      (let [regions (<! (get-regions))]
        (.removeAll regions-menu)
        (doseq [r regions]
          (.add regions-menu
                (radio-menu-item :group region-buttons :text r)))
        (let [selected (first (config region-buttons :buttons))]
          (selection! region-buttons selected))))

    (doseq [gm (map (comp #(subs % 1) str) [:FFA :Team :Experimental])]
      (.add gamemode-menu (radio-menu-item :text gm :group gamemode-buttons)))
    (selection! gamemode-buttons (first (config gamemode-buttons :buttons)))

    (listen game-window
            :window-closing
            (fn [e]
              (reset! game-running false)
              (dispose! game-window)))

    (listen connect-button
            :action-performed
            (fn [e]
              (let [region (config (selection region-buttons) :text)
                    gamemode (config (selection gamemode-buttons) :text)]
                (reset! game (connect region gamemode)))))

    (invoke-later (show! game-window))))


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
