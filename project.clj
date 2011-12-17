(defproject ru.petrsu.nest/yz "0.0.1-alpha11"
  :description "Implementation of the YZ language."
  :url "https://github.com/vdim/yz"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.hibernate/hibernate-entitymanager "3.6.1.Final"]
                 [org.apache.derby/derby "10.7.1.1"]
                 [com.h2database/h2 "1.3.160"]
                 [org.clojure/algo.monads "0.1.0"] 
                 [incanter/incanter-core "1.3.0-SNAPSHOT"]
                 [org.codehaus.jsr166-mirror/jsr166y "1.7.0"]]
  :aot [ru.petrsu.nest.yz.core 
        ru.petrsu.nest.yz.hb-utils 
        ru.petrsu.nest.yz.yz
        ru.petrsu.nest.yz.yz-factory]
  :java-source-path "."
  :jar-exclusions [#"\.swp$|\.clj$|son"]
  :dev-dependencies [[lein-clojars "0.7.0"]]
  :warn-on-reflection false
  :omit-source true
  :repl-init ru.petrsu.nest.yz.init)
