(defproject ru.petrsu.nest/yz "0.0.1-alpha11"
  :description "Implementation of the YZ language."
  :url "https://github.com/vdim/yz"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.hibernate/hibernate-entitymanager "3.6.1.Final"]
                 [org.clojure/algo.monads "0.1.0"] ]
  :aot [ru.petrsu.nest.yz.core 
        ru.petrsu.nest.yz.hb-utils 
        ru.petrsu.nest.yz.yz
        ru.petrsu.nest.yz.yz-factory]
  :java-source-path "./src/"
  :jar-exclusions [#"\.swp$"]
  :dev-dependencies [[lein-clojars "0.7.0"]]
  :warn-on-reflection false)
