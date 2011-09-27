(ns ru.petrsu.nest.yz.benchmark.bd-utils
  ^{:author Vyacheslav Dimitrov
    :doc "Usefull functions for working with bd for benchmark."}
  (:import (ru.petrsu.nest.son Building Floor Room
                               Occupancy SimpleOU CompositeOU
                               Device Network NetworkInterface EthernetInterface
                               LinkInterface IPv4Interface IPNetwork VLANInterface SON)
           (javax.persistence Persistence)
           (org.hibernate.tool.hbm2ddl SchemaExport)
           (org.hibernate.cfg Configuration)
           (java.util Random)))

(def hibcfg
  ^{:doc "Defines name of file with hibernate config relatively classpath."}
  (identity "/hibench.cfg.xml"))

(def classes
  ^{:doc "Defines all classes of SON model."}
  [Building Floor Room
   Occupancy SimpleOU CompositeOU
   Device Network NetworkInterface EthernetInterface
   LinkInterface IPv4Interface IPNetwork VLANInterface SON])

(defn schema-export
  "Clean databases due to Hibernate Schema Export."
  []
  (let [cfg (doto (Configuration.) (.configure hibcfg))
        just-drop false
        just-create false
        script false
        export true
        _ (doto (SchemaExport. cfg) 
            (.setOutputFile "ddl.sql")
            (.setDelimiter ";")
            (.execute script export just-drop just-create))]))


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
  "Takes number of elements in BD, generate BD 
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
  "Creates BD which has n elements."
  [n]
  (let [em (.createEntityManager (Persistence/createEntityManagerFactory "bench"))
        son (gen-bd n)]
    (do (.. em getTransaction begin) 
      (.persist em son)
      (.. em getTransaction commit))))

