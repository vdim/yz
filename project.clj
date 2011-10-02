(defproject yz "0.0.1"
  :description "Implementation of YZ language."
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [fnparse "2.2.7"]]
  :aot [ru.petrsu.nest.yz.core 
        ru.petrsu.nest.yz.hb-utils 
        ru.petrsu.nest.yz.functions]
  :jar-exclusions [#"\.swp$|\.clj$"]
  :dev-dependencies [[lein-clojars "0.5.0"]])

