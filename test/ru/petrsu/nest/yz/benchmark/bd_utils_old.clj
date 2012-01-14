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
    :doc "Code for generating nest DB (JPA variant)."}
  (:require [ru.petrsu.nest.util.utils :as f] [ru.petrsu.nest.yz.benchmark.bd-utils :as bu])
  (:import (ru.petrsu.nest.son.jpa Building Floor Room
                                   Occupancy SimpleOU CompositeOU Device 
                                   Network IPNetwork EthernetInterface LinkInterface 
                                   IPv4Interface NetworkInterface VLANInterface SON)
           (javax.persistence Persistence)
           (org.hibernate.tool.hbm2ddl SchemaExport)
           (org.hibernate.cfg Configuration)
           (java.util Random)))


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


(defn init-model
  "Inits model."
  [state]
  (do
    (.addBuilding (:son state) (:building state))
    (.setRootDevice (:son state) (:device state))
    (.setRootOU (:son state) (:cou state))

    (.addFloor (:building state) (:floor state))
    (.addRoom (:floor state) (:room state))

    (let [o (:occupancy state)]
      (.setRoom o (:room state))
      (.addDevice o (:device state))
      (.setOU o (:sou state)))

    (.addOU (:cou state) (:sou state))
    (let [d (:device state)]
      (.addLinkInterface d (:li state))
      (.addLinkInterface d (:ei state))
      (.addLinkInterface d (:vlan state)))

    (.addNetworkInterface (:li state) (:ni state))
    (.addNetworkInterface (:li state) (:ipv4 state))
    (.addNetworkInterface (:ipn state) (:ni state))
    (.addNetworkInterface (:ipn state) (:ipv4 state))
    state))


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
  "Takes number of elements in BD, generates BD 
  and returns SON."
  [n]
  (let [r (Random.)
        clc (count classes)
        cn (count bu/names)
        cd (count bu/descs)
        se #(let [[cl k] (classes (.nextInt r clc))
                  cl (if (nil? %) cl %)
                  se (doto (.newInstance cl)
                       (.setName (str (.getSimpleName cl) "_" (bu/names (.nextInt r cn))))
                       (.setDescription (bu/descs (.nextInt r cd))))
                  se (if (instance? Floor se) (doto se (.setNumber (Integer. (.nextInt r 100)))) se) 
                  se (if (instance? Room se) (doto se (.setNumber (str (.nextInt r 100)))) se) 
                  se (if (instance? IPv4Interface se) 
                       (doto se (.setInetAddress (f/ip2b (bu/gen-ip r))))
                       se)
                  se (if (instance? IPNetwork se) 
                       (doto se 
                         (.setAddress (f/ip2b (bu/gen-ip r))) 
                         (.setMask (f/ip2b (bu/gen-mask r))))
                       se)
                  se (if (instance? EthernetInterface se) 
                       (doto se (.setMACAddress (f/mac2b (bu/gen-mac r))))
                       se)]
              [se k])
        sm (init-model {:building ((se Building) 0)
                        :floor ((se Floor) 0)
                        :room ((se Room) 0)
                        :occupancy ((se Occupancy) 0)
                        :sou ((se SimpleOU) 0)
                        :cou ((se CompositeOU) 0)
                        :device ((se Device) 0)
                        :network ((se Network) 0)
                        :ni ((se NetworkInterface) 0)
                        :ei ((se EthernetInterface) 0)
                        :li ((se LinkInterface) 0)
                        :ipn ((se IPNetwork) 0)
                        :ipv4 ((se IPv4Interface) 0)
                        :vlan ((se VLANInterface) 0)
                        :son (SON.)})]
    (loop [sm- sm, n- n]
      (if (<= n- 0)
        (:son sm-)
        (recur (change-model sm- (se nil)) (dec n-))))))


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

