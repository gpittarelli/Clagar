(defproject clagar "0.1.0-SNAPSHOT"
  :description "Clojure agar.io client"
  :url "http://example.com/FIXME"
  :license {:name "GNU General Purpose License version 3.0"
            :url "http://www.gnu.org/licenses/gpl-3.0.en.html"
            :distribution :repo}
  :plugins [[lein-git-deps "0.0.1-SNAPSHOT"]]
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [seesaw "1.4.5"]
                 [aleph "0.4.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [org.clojure/data.json "0.2.6"]
                 [org.lwjgl.lwjgl/lwjgl "2.9.3"]
                 [org.lwjgl.lwjgl/lwjgl_util "2.9.3"]
                 [org.lwjgl.lwjgl/lwjgl_util_applet "2.9.3"]
                 [stylefruits/gniazdo "0.4.0"]
                 [slick2d "2013.01.05"]
                 [smee/binary "0.5.1"]
                 [org.lwjgl.lwjgl/lwjgl-platform "2.9.3"
                  :classifier "natives-linux"
                  :native-prefix ""]]
  :git-dependencies [["https://github.com/jolby/colors.git"]]
  :main ^:skip-aot clagar.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
