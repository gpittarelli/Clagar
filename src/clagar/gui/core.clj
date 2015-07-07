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
             :refer [>! >!! <! <!! chan go go-loop]])
  (:import [javax.swing JOptionPane]
           [java.awt Canvas Graphics Font]
           [org.lwjgl LWJGLException]
           [org.lwjgl BufferUtils]
           [org.lwjgl.opengl Display DisplayMode GL11 AWTGLCanvas]
           [org.newdawn.slick UnicodeFont Color Image]
           [org.newdawn.slick.font HieroSettings]
           [org.newdawn.slick.font.effects ColorEffect OutlineEffect]))


(let [circle-angles
      (memoize (fn [n]
                 (map (juxt #(Math/cos %) #(Math/sin %))
                      (range 0 (* 2 (Math/PI))
                             (/ (* 2 (Math/PI)) n)))))]
  (defn render-circle
    "Render a circle in the current OpenGL context. Note that color
  should be an r/g/b sequence where each color ranges from 0.0-1.0."
    [x y r color & {:keys [spiky? filled? points]
                    :or {spiky? false filled? true points 50}}]

    (let [vertex-buf (BufferUtils/createFloatBuffer (* points 2))]

      (loop [[[cos sin] & tail] (circle-angles points)
             i 0]
        (let [offset (if spiky? (* 3 (mod i 2)) 0)]
          (.put vertex-buf (float (+ x (* cos (+ r offset)))))
          (.put vertex-buf (float (+ y (* sin (+ r offset)))))
          (when tail (recur tail (inc i)))))

      (.flip vertex-buf)

      (GL11/glColor3f (color 0) (color 1) (color 2))
      (GL11/glEnableClientState GL11/GL_VERTEX_ARRAY)

      (GL11/glVertexPointer 2 0 vertex-buf)
      (GL11/glDrawArrays (if filled? GL11/GL_TRIANGLE_FAN GL11/GL_LINE_LOOP)
                         0 points)

      (GL11/glDisableClientState GL11/GL_COLOR_ARRAY)
      (GL11/glDisableClientState GL11/GL_VERTEX_ARRAY))))

(letfn [(load-font []
          (let [hs (doto (new HieroSettings)
                     (.setFontSize 35)
                     (.setGlyphPageHeight 512)
                     (.setGlyphPageWidth 512)
                     (.setItalic false)
                     (.setPaddingAdvanceX -5)
                     (.setPaddingAdvanceY 0)
                     (.setPaddingBottom 3)
                     (.setPaddingLeft 3)
                     (.setPaddingRight 3)
                     (.setPaddingTop 3))
                f (new UnicodeFont (new Font "Ubuntu" Font/PLAIN 35) hs)]
             (.addAsciiGlyphs f)
             (.add (.getEffects f) (new OutlineEffect 3 java.awt.Color/black))
             (.add (.getEffects f) (new ColorEffect java.awt.Color/white))
             (.loadGlyphs f)
             f))]
  (def get-font (memoize load-font)))


;; (def ^:private font (volatile! nil))
(def ^:private frame-count (volatile! 0))
(def ^:private frame-time (volatile! 0))
(def ^:private fps (volatile! 0))
(def ^:private prev-fps-time (volatile! 0))
(def ^:private display-size (volatile! [1 1]))
(defn- render [w h state]
  (when-not (= @display-size [w h])
    (GL11/glMatrixMode GL11/GL_PROJECTION)
    (GL11/glLoadIdentity)
    (GL11/glViewport 0 0 w h)
    (GL11/glOrtho 0 w h 0 1 -1)

    (vreset! display-size [w h]))

  (let [t (System/nanoTime)
        dt (- t @frame-time)]
    (when (> dt 1e9)
      (vreset! fps (/ @frame-count (/ dt 1e9)))
      (vreset! frame-count 0)
      (vreset! frame-time t)))
  (vreset! frame-count (inc @frame-count))

  (GL11/glClear (bit-or GL11/GL_COLOR_BUFFER_BIT GL11/GL_DEPTH_BUFFER_BIT))

  (GL11/glEnable GL11/GL_TEXTURE_2D)
  (GL11/glEnable GL11/GL_BLEND)
  (GL11/glBlendFunc GL11/GL_SRC_ALPHA GL11/GL_ONE_MINUS_SRC_ALPHA)
  (.drawString (get-font) (- w 100) (- h 100) (format "%.1f" @fps))
  (GL11/glDisable GL11/GL_TEXTURE_2D)
  (GL11/glDisable GL11/GL_BLEND)

  (when state
    (let [{:keys [min-x max-x]} (:world state)
          map-width (- max-x min-x)
          f (get-font)
          adj-coord (fn [n dim] (* (- n min-x) (/ dim map-width)))]
      (doseq [blob (sort-by :size (vals (:blobs state)))
              :let
              [color (->> blob :color ((juxt :r :g :b)) (mapv #(/ % 255)))]]

        (render-circle (adj-coord (:x blob) w)
                       (adj-coord (:y blob) h)
                       (* (:size blob) (/ w map-width))
                       color
                       :spiky? (:virus? (:flags blob))
                       :points (if (> (:size blob) 20) 50 5))

;;        (when (> (:size blob) 2 0)
;;           (GL11/glLineWidth 20)
;;           (render-circle (adj-coord (:x blob) w)
;;                          (adj-coord (:y blob) h)
;;                          (+ (* (:size blob) (/ w map-width)) 1)
;;                          [1 1 1]
;;                          :filled? false
;;                          :points (if (> (:size blob) 20) 50 5))
;;           (GL11/glLineWidth 1))

        (when-let [name (:name blob)]
          (GL11/glEnable GL11/GL_TEXTURE_2D)
          (GL11/glEnable GL11/GL_BLEND)
          (GL11/glBlendFunc GL11/GL_SRC_ALPHA GL11/GL_ONE_MINUS_SRC_ALPHA)
          (let [s (/ (:size blob) 500)
                inv-s (/ 1 s)]
            (GL11/glScalef s s 1)
            (.drawString f
                         (-
                          (adj-coord (:x blob) (/ w s))
                          (/ (.getWidth f name) 2))

                         (-
                          (adj-coord (:y blob) (/ h s))
                          (/ (.getLineHeight f) 2))
                         name)
            (GL11/glScalef inv-s inv-s 1))
          (GL11/glDisable GL11/GL_TEXTURE_2D)
          (GL11/glDisable GL11/GL_BLEND))))))


(defn create-game-window []
  (let [;; atom -> future -> GameConnection
        game (atom nil)

        game-canvas (proxy [AWTGLCanvas] []
                      (initGL []
                        (GL11/glClearColor 0.2 0.2 0.2 1))
                      (paintGL []
                        (let [state-ref @game
                              state (when state-ref @(.state state-ref))
                              w (proxy-super getWidth)
                              h (proxy-super getHeight)]
                          (render w h state))
                        (proxy-super swapBuffers)
                        (proxy-super repaint)))

        regions-menu (menu :text "Regions" :items [(action :name "Loading...")])
        region-buttons (button-group)

        gamemode-menu (menu :text "Gamemode")
        gamemode-buttons (button-group)

        game-options (menu :text "Options" :items [(action :name "Exit")])
        connect-button (button :text "Connect")
        spectate-button (button :text "Spectate")

        game-menu (menubar :items [regions-menu
                                   gamemode-menu
                                   game-options
                                   (text :text "connect to")
                                   connect-button
                                   spectate-button])

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
              (let [g @game] (when g (.close g)))

              ;; Break reference to the game, should allow  all the
              ;; resources (including the connection) to be cleaned
              ;; up.
              (reset! game nil)
              (dispose! game-window)))

    (listen connect-button
            :action-performed
            (fn [e]
              (let [region (config (selection region-buttons) :text)
                    gamemode (config (selection gamemode-buttons) :text)]
                (go
                  (reset! game (<! (connect region gamemode)))))))

    (listen spectate-button
            :action-performed
            (fn [e] (let [g @game] (when g (.spectate g)))))

    (invoke-later (show! game-window))))
