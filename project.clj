(defproject ru.petrsu.nest/yz "0.0.1-alpha16"
  :description "YZ[ˈiːzi] is simple (easy), laconic, string-based, object query language."
  :url "https://github.com/vdim/yz"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/algo.monads "0.1.0"] ]
  :aot [ru.petrsu.nest.yz.core 
        ru.petrsu.nest.yz.yz
        ru.petrsu.nest.yz.yz-factory 
        ru.petrsu.nest.yz.pquery]
  :compile-path "target/classes"
  :java-source-paths ["src/"]
  :jar-exclusions [#"\.swp$|\.java$"]
  :dev-dependencies [[lein-clojars "0.7.0"]]
  :warn-on-reflection false)
