(defproject datomic-em "0.0.1"
  :description "Implementation of the ru.petrsu.nest.yz.core.ElementManager for Datomic."
  :dependencies [
                 [com.google.code.simple-spring-memcached/spymemcached "2.8.1"]
                 [spy/spymemcached "2.8.1"]
                 [org.clojure/clojure "1.4.0"]
                 ;[com.datomic/datomic-free "0.8.3563"]
                 ]
  :aot [ru.petrsu.nest.yz.datomic-em.core]
  :source-paths ["../../src" "src/" "../../target/classes"]
  :compile-path "target/classes"
  :jar-exclusions [#"\.swp$|\.java$"])
