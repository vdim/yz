(defproject mom-editor "0.0.1-alpha"
  :description "MOM's editor."
  :dependencies [[org.clojure/clojure "1.3.0"] 
                 [org.hibernate/hibernate-entitymanager "3.6.1.Final"]
                 [com.google.guava/guava "11.0.1"]]
  :aot [net.kryshen.indyvon.core
        net.kryshen.indyvon.async
        net.kryshen.indyvon.layers
        net.kryshen.indyvon.component
        net.kryshen.indyvon.demo]
  :java-source-path "src/"
  :repl-init ru.petrsu.nest.yz.momeditor.init-repl)
