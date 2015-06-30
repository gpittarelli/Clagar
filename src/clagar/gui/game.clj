(ns clagar.gui.game
  (:require [seesaw.core :refer [border-panel frame menubar
                                 menu invoke-later show!
                                 listen]])
  (:import  [javax.swing JOptionPane]
            [java.awt Canvas Graphics]
            [org.lwjgl LWJGLException]
            [org.lwjgl.opengl Display DisplayMode GL11]))



(defn- create-game []
;;  (def game-running (atom true))
;;  (def game-canvas (new Canvas))
;;  (def game-panel (border-panel :center game-canvas))

  ;;  (listen game-panel :component-resized  (fn [e] (prn e)))
;;   (listen game-window :window-closing (fn [e] (reset! game-running false)))

;;   (show! game-window)

;;   (Display/setParent game-canvas)

;;   (Display/setInitialBackground 0 0 0.4)

;;   (Display/setResizable true)
;;   (Display/create)

;;   (prn "resizable" (Display/isResizable))

;;   (while (true? (deref game-running))
;;     (let [w (.getWidth game-canvas) h (.getHeight game-canvas)]
;;       (draw w h))

;;     (Display/update)
;;     (Thread/sleep 10))

;;   (Display/destroy)
;;  (dispose! game-window)
  )
