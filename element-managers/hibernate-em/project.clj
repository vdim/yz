(defproject hibernate-em "0.0.1"
  :description "Implementation of the ru.petrsu.nest.yz.core.ElementManager for Hibernate tool."
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.hibernate.javax.persistence/hibernate-jpa-2.0-api "1.0.1.Final"]
                 [ru.petrsu.nest/yz "0.0.1-alpha16"]]
  :aot [ru.petrsu.nest.yz.hibernate-em.core]
  :java-source-path "./src/"
  :jar-exclusions [#"\.swp$|\.java$"])
