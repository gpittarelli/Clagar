(defproject clagar "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [seesaw "1.4.5"]
                 [aleph "0.4.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/data.json "0.2.6"]
                 [org.lwjgl.lwjgl/lwjgl "2.9.3"]
                 [org.lwjgl.lwjgl/lwjgl_util "2.9.3"]
                 [org.lwjgl.lwjgl/lwjgl_util_applet "2.9.3"]
                 [org.lwjgl.lwjgl/lwjgl-platform "2.9.3"
                  :classifier "natives-linux"
                  :native-prefix ""]]
  :main ^:skip-aot clagar.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
