(defproject ru.petrsu.nest/yz "0.0.1-alpha15"
  :description "YZ[ˈiːzi] is simple (easy), laconic, string-based, object query language."
  :url "https://github.com/vdim/yz"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.hibernate.javax.persistence/hibernate-jpa-2.0-api "1.0.1.Final"]
                 [org.clojure/algo.monads "0.1.0"] ]
  :aot [ru.petrsu.nest.yz.core 
        ru.petrsu.nest.yz.yz
        ru.petrsu.nest.yz.yz-factory]
  :java-source-path "./src/"
  :jar-exclusions [#"\.swp$|\.java$"]
  :dev-dependencies [[lein-clojars "0.7.0"]]
  :warn-on-reflection false)
