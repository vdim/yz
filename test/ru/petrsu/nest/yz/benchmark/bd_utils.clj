;;
;; Copyright 2011 Vyacheslav Dimitrov <vyacheslav.dimitrov@gmail.com>
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

(ns ru.petrsu.nest.yz.benchmark.bd-utils
  ^{:author "Vyacheslav Dimitrov"
    :doc "Usefull functions for working with bd for benchmark."}
  (:import (ru.petrsu.nest.son Building Floor Room
                               Occupancy SimpleOU CompositeOU
                               Device Network NetworkInterface EthernetInterface
                               LinkInterface IPv4Interface IPNetwork VLANInterface SON)
           (javax.persistence Persistence)
           (org.hibernate.tool.hbm2ddl SchemaExport)
           (org.hibernate.cfg Configuration)
           (java.util Random)))


(defmacro btime
  "Like Clojure's macros time, but doesn't have side effect 
  (something prints) and returns time which is taken for
  evaluating an expr."
  [expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr]
     (/ (double (- (. System (nanoTime)) start#)) 1000000.0)))


(def hibcfg
  ^{:doc "Defines name of file with hibernate config relatively classpath."}
  (identity "/META-INF/hibernate.cfg.xml"))

(def ^{:dynamic true} *url* (identity "jdbc:derby:db1;create=true"))
(def ^{:dynamic true} *dialect* (identity "org.hibernate.dialect.DerbyDialect"))
(def ^{:dynamic true} *driver* (identity "org.apache.derby.jdbc.EmbeddedDriver"))

(def classes
  ^{:doc "Defines all classes of SON model."}
  (vec (flatten (map (fn [[k v]] (repeat v k)) 
                     {Building 1
                      Floor 5
                      Room 50
                      Occupancy 5
                      SimpleOU 20
                      CompositeOU 5
                      Device 300
                      Network 5
                      NetworkInterface 170 
                      EthernetInterface 150
                      LinkInterface 150
                      IPv4Interface 170
                      IPNetwork 5
                      VLANInterface 20
                      SON 1}))))

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
  [sm o]
  (cond (instance? Building o) (do (.addBuilding (:son sm) o) (assoc sm :building o))
        (instance? Floor o) (do (.addFloor (:building sm) o) (assoc sm :floor o))
        (instance? Room o) (do (.addRoom (:floor sm) o) 
                             (if (nil? (.getRoom (:occupancy sm)))
                               (.setRoom (:occupancy sm) o))
                             (assoc sm :room o))
        (instance? Occupancy o) (do (.setRoom o (:room sm)) 
                                  (.setOU o (:sou sm))
                                  (if (nil? (.getOccupancy (:device sm)))
                                    (.addDevice o (:device sm)))
                                  (assoc sm :occupancy o))
        (instance? SimpleOU o) (do (.addOU (:cou sm) o)
                                 (if (nil? (.getOU (:occupancy sm)))
                                   (.setOU (:occupancy sm) o))
                                 (assoc sm :sou o))
        (instance? CompositeOU o) (do (.addOU (:cou sm) o) (assoc sm :cou o))
        (instance? Device o) (do (.addDevice (:occupancy sm) o) (assoc sm :device o))
        (instance? LinkInterface o) (do (.addLinkInterface (:device sm) o) (assoc sm :li o))
        (instance? EthernetInterface o) (do (.addLinkInterface (:device sm) o) (assoc sm :ei o))
        (instance? VLANInterface o) (do (.addLinkInterface (:device sm) o) (assoc sm :vlan o))
        (instance? NetworkInterface o) (do (.addNetworkInterface (:li sm) o) (assoc sm :ni o))
        (instance? IPv4Interface o) (do (.addNetworkInterface (:ei sm) o) (assoc sm :ipv4 o))
        (instance? Network o) (do 
                                 (if (nil? (.getNetwork (:ni sm)))
                                   (.setNetwork (:ni sm) o)
                                   (if (nil? (.getNetwork (:ipv4 sm)))
                                     (.setNetwork (:ipv4 sm))))
                                 (assoc sm :network o))
        (instance? IPNetwork o) (do 
                                 (if (nil? (.getNetwork (:ni sm)))
                                   (.setNetwork (:ni sm) o)
                                   (if (nil? (.getNetwork (:ip4v sm)))
                                     (.setNetwork (:ip4v sm))))
                                 (assoc sm :ipn o))
        :else sm))


(defn gen-bd
  "Takes number of elements in BD, generates BD 
  and returns SON."
  [n]
  (let [sm (init-model {:building (Building.)
                        :floor (Floor.)
                        :room (Room.)
                        :occupancy (Occupancy.)
                        :sou (SimpleOU.)
                        :cou (CompositeOU.)
                        :device (Device.)
                        :network (Network.)
                        :ni (NetworkInterface.)
                        :ei (EthernetInterface.)
                        :li (LinkInterface.)
                        :ipn (IPNetwork.)
                        :ipv4 (IPv4Interface.)
                        :vlan (VLANInterface.)
                        :son (SON.)})
        r (Random.)
        clc (count classes)]
    (loop [sm- sm, n- n]
      (if (<= n- 0)
        (:son sm)
        (recur (change-model sm- (.newInstance (classes (.nextInt r clc)))) 
               (dec n-))))))


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

