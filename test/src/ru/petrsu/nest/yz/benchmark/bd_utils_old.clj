;;
;; Copyright 2012 Vyacheslav Dimitrov <vyacheslav.dimitrov@gmail.com>
;;
;; This file is part of YZ.
;;
;; YZ is free software: you can redistribute it and/or modify it
;; under the terms of the GNU Lesser General Public License version 3
;; only, as published by the Free Software Foundation.
;;
;; YZ is distributed in the hope that it will be useful, but
;; WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
;; Lesser General Public License for more details.
;;
;; You should have received a copy of the GNU Lesser General Public
;; License along with YZ.  If not, see
;; <http://www.gnu.org/licenses/>.
;;

(ns ru.petrsu.nest.yz.benchmark.bd-utils-old
  ^{:author "Vyacheslav Dimitrov"
    :doc "Code for generating nest DB (JPA (old) variant)."}
  (:require [ru.petrsu.nest.yz.benchmark.bd-utils :as bu])
  (:import (ru.petrsu.nest.son.jpa Building Floor Room
                                   Occupancy SimpleOU CompositeOU Device 
                                   Network IPNetwork EthernetInterface LinkInterface 
                                   IPv4Interface NetworkInterface VLANInterface SON)
           (javax.persistence Persistence)
           (org.hibernate.tool.hbm2ddl SchemaExport)
           (org.hibernate.cfg Configuration)))


(def hibcfg
  ^{:doc "Defines name of file with hibernate config relatively classpath."}
  (identity "/META-INF/hibernate.cfg.xml"))

(def ^{:dynamic true} *url* (identity "jdbc:derby:db1;create=true"))
(def ^{:dynamic true} *dialect* (identity "org.hibernate.dialect.DerbyDialect"))
(def ^{:dynamic true} *driver* (identity "org.apache.derby.jdbc.EmbeddedDriver"))


(defn schema-export
  "Cleans database due to Hibernate Schema Export."
  ([]
   (schema-export *url* *dialect* *driver*))
  ([url dialect driver]
   (let [cfg (doto (Configuration.) 
               (.configure hibcfg)
               (.setProperty "hibernate.connection.url" url)
               (.setProperty "hibernate.dialect" dialect)
               (.setProperty "hibernate.connection.driver_class" driver))
         just-drop false
         just-create false
         script false
         export true
         _ (doto (SchemaExport. cfg) 
             (.setOutputFile "ddl.sql")
             (.setDelimiter ";")
             (.execute script export just-drop just-create))])))


(def cls
  "List of jpa type of son model."
  [Building Floor Room Occupancy SimpleOU CompositeOU 
   Device Network NetworkInterface EthernetInterface 
   LinkInterface IPv4Interface IPNetwork VLANInterface SON])


(defn create-bd
  "Creates BD for specified EntityManager 
  and with specified n elements."
  [n, em]
  (let [son (bu/gen-bd n cls)]
    (do (.. em getTransaction begin) 
      (.persist em son)
      (.flush em)
      (.. em getTransaction commit))))


(defn- do-cr
  "Takes a number of query from 'queries array' and a name of the persistence unit,
  executes query, ant returns time of executing query."
  [nums, n, url dialect driver]
  (let [_ (schema-export url dialect driver)
        m {"hibernate.connection.url" url, 
           "hibernate.dialect" dialect, 
           "hibernate.connection.driver_class" driver}
        em (.createEntityManager (javax.persistence.Persistence/createEntityManagerFactory n m))]
    (create-bd (Integer/parseInt nums) em)))


(defn -main
  "Takes a number of elements and name of the persistence unit 
  ant creates database. If name of persistence unit is not supplied then \"bench\" is used."
  ([nums]
   (do-cr nums, "bench", *url* *dialect* *driver*))
  ([nums, n]
   (do-cr nums, n, *url* *dialect* *driver*))
  ([nums, n, url dialect driver]
   (do-cr nums, n, url, dialect, driver)))

