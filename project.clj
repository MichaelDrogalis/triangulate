(defproject triangulate "0.1.0-SNAPSHOT"
  :description "Web service to calculating exact coordinates on the Rush Hour platform"
  :url "https://github.com/MichaelDrogalis/triangulate"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [ring/ring-jetty-adapter "1.2.0"]
                 [compojure "1.1.5"]
                 [clj-http "0.7.7"]
                 [cheshire "5.2.0"]]
  :main triangulate.core)

