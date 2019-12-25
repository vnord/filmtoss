(defproject filmtoss "0.1.0-SNAPSHOT"
  :description "Helps you select your next film"
  :url "https://vnord.net"
  :license {:name "GNU GPL v3+"
            :url "http://www.gnu.org/licenses/gpl-3.0.en.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [enlive "1.1.1"]
                 [ring "1.2.0"]
                 [net.cgrand/moustache "1.2.0-alpha2"]]
  :repl-options {:init-ns filmtoss.core})
