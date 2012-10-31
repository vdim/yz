(defproject jpa-em "0.0.1"
  :description "Implementation of the ru.petrsu.nest.yz.core.ElementManager for JPA 2.0."
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.hibernate.javax.persistence/hibernate-jpa-2.0-api "1.0.1.Final"]]
  :aot [ru.petrsu.nest.yz.jpa-em.core 
        ru.petrsu.nest.yz.jpa-em.mom]
  :source-paths ["../../src" "src/" "../../target/classes"]
  :compile-path "target/classes"
  :jar-exclusions [#"\.swp$|\.java$"])
