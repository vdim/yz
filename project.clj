(defproject ru.petrsu.nest/yz "0.0.1-alpha8"
  :description "Implementation of the YZ language."
  :url "https://github.com/vdim/yz"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.hibernate/hibernate-entitymanager "3.6.1.Final"]
                 [org.apache.derby/derby "10.7.1.1"]
                 [com.h2database/h2 "1.3.160"]
                 [org.clojure/algo.monads "0.1.0"]]
  :aot [ru.petrsu.nest.yz.core 
        ru.petrsu.nest.yz.hb-utils 
        ru.petrsu.nest.yz.functions 
        ru.petrsu.nest.yz.yz]
  :java-source-path "test"
  :jar-exclusions [#"\.swp$|\.clj$"]
  :dev-dependencies [[lein-clojars "0.7.0"]]
  :warn-on-reflection false)
