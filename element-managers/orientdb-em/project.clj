(defproject orientdb-em "0.0.1"
  :description "Implementation of the ru.petrsu.nest.yz.core.ElementManager for OrientDB."
  :url ""
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [com.orientechnologies/orient-commons "1.1.0"]
                 [com.orientechnologies/orientdb-core "1.1.0"]
                 [com.orientechnologies/orientdb-object "1.1.0"]]
  :aot [ru.petrsu.nest.yz.orientdb-em.core]
  :source-paths ["../../src" "src/" "../../target/classes"]
  :compile-path "target/classes")
