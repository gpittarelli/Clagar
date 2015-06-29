(ns clagar.gui.game
  (:require [seesaw.core :refer [border-panel frame menubar
                                 menu invoke-later show!
                                 listen]])
  (:import  [javax.swing JOptionPane]
            [java.awt Canvas Graphics]
            [org.lwjgl LWJGLException]
            [org.lwjgl.opengl Display DisplayMode GL11]))

(defn- draw [w h]
  (GL11/glMatrixMode GL11/GL_PROJECTION)
  (GL11/glLoadIdentity)
  (GL11/glViewport 0 0 w h)
  (GL11/glOrtho 0 w 0 h 1 1)

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
