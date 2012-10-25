(defproject ru.petrsu.nest/yz "0.0.1-alpha16"
  :description "Tests for YZ's project."
  :url "https://github.com/vdim/yz"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.hibernate/hibernate-entitymanager "3.6.1.Final"]
                 [org.apache.derby/derby "10.7.1.1"]
                 [com.h2database/h2 "1.3.160"]
                 [org.hsqldb/hsqldb "2.2.4"]
                 [org.clojure/algo.monads "0.1.0"] 
                 [incanter/incanter-charts "1.3.0"]
                 [incanter/incanter-core "1.3.0"]
                 [incanter/incanter-pdf "1.3.0"]
                 [criterium "0.3.0"]]
  :java-source-paths ["src/"]
  :dev-dependencies [[lein-clojars "0.7.0"]]
  :warn-on-reflection false
  :omit-source true
  :test-paths ["src/"]
  :source-paths ["../src" ; YZ source
                 "../element-managers/hibernate-em/src" ; JPA Element Manager source.
                 "src/"]
  :compile-path "../target/classes"
  :resource-paths ["test-resource/"]
  :repl-options {:init-ns ru.petrsu.nest.yz.init}
  :test-selectors {:default (fn [v] (not (:laborious v)))
                   :laborious :laborious
                   :all (fn [_] true)})
