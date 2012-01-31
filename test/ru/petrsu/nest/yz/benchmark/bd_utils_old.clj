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

(def classes
  ^{:doc "Defines all classes of SON model with its weights."}
  (vec (concat (reduce (fn [r [k v]] (concat r (repeat v k)))
                       []
                       {[Building :building] 1
                        [Floor :floor] 5
                        [Room :room] 50
                        [Occupancy :occupancy]5
                        [SimpleOU :sou] 20
                        [CompositeOU :cou] 5
                        [Device :device] 300
                        [Network :network] 5
                        [NetworkInterface :ni] 170 
                        [EthernetInterface :ei] 150
                        [LinkInterface :li] 150
                        [IPv4Interface :ipv4] 170
                        [IPNetwork :ipn] 5
                        [VLANInterface :vlan] 20}))))


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


(defn change-model
  "Changes model: takes model and object of 
  model and inserts object into the model."
  [sm [o k]]
  (do 
    (cond (instance? Building o) (.addBuilding (:son sm) o)
          (instance? Floor o) (.addFloor (:building sm) o)
          (instance? Room o) (do (.addRoom (:floor sm) o) 
                               (if (nil? (.getRoom (:occupancy sm)))
                                 (.setRoom (:occupancy sm) o)))
          (instance? Occupancy o) (do (.setRoom o (:room sm)) 
                                    (.setOU o (:sou sm))
                                    (if (nil? (.getOccupancy (:device sm)))
                                      (.addDevice o (:device sm))))
          (instance? SimpleOU o) (do (.addOU (:cou sm) o)
                                   (if (nil? (.getOU (:occupancy sm)))
                                     (.setOU (:occupancy sm) o)))
          (instance? CompositeOU o) (.addOU (:cou sm) o)
          (instance? Device o) (.addDevice (:occupancy sm) o)
          (instance? LinkInterface o) (.addLinkInterface (:device sm) o)
          (instance? EthernetInterface o) (.addLinkInterface (:device sm) o)
          (instance? VLANInterface o) (.addLinkInterface (:device sm) o)
          (instance? NetworkInterface o) (do (.addNetworkInterface (:li sm) o) 
                                                  (.setNetwork o (:network sm)))
          (instance? IPv4Interface o) (do (.addNetworkInterface (:ei sm) o) 
                                        (.setNetwork o (:ipn sm))))
    (assoc sm k o)))


(defn gen-bd
  "Takes number of elements in DB and generates DB. Returns SON."
  [n]
  (let [sm (bu/init-model {:building ((bu/gen-element Building classes) 0)
                        :floor ((bu/gen-element Floor classes) 0)
                        :room ((bu/gen-element Room classes) 0)
                        :occupancy ((bu/gen-element Occupancy classes) 0)
                        :sou ((bu/gen-element SimpleOU classes) 0)
                        :cou ((bu/gen-element CompositeOU classes) 0)
                        :device ((bu/gen-element Device classes) 0)
                        :network ((bu/gen-element Network classes) 0)
                        :ni ((bu/gen-element NetworkInterface classes) 0)
                        :ei ((bu/gen-element EthernetInterface classes) 0)
                        :li ((bu/gen-element LinkInterface classes) 0)
                        :ipn ((bu/gen-element IPNetwork classes) 0)
                        :ipv4 ((bu/gen-element IPv4Interface classes) 0)
                        :vlan ((bu/gen-element VLANInterface classes) 0)
                        :son (SON.)}) 
        _ (dorun (repeatedly n #(change-model sm (bu/gen-element nil classes))))]
    (:son sm)))


(defn create-bd
  "Creates BD for specified EntityManager 
  and with specified n elements."
  [n, em]
  (let [son (gen-bd n)]
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

