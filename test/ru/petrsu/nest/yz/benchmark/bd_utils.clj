;;
;; Copyright 2011-2012 Vyacheslav Dimitrov <vyacheslav.dimitrov@gmail.com>
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
  (:require [ru.petrsu.nest.util.utils :as f])
  (:import (ru.petrsu.nest.son Building Floor Room
                               Occupancy SimpleOU CompositeOU
                               Device UnknownNetwork UnknownNetworkInterface EthernetInterface
                               UnknownLinkInterface IPv4Interface IPNetwork VLANInterface SON)
           (javax.persistence Persistence)
           (java.util Random)))


(defmacro btime
  "Like Clojure's macros time, but doesn't have side effect 
  (something prints) and returns time which is taken for
  evaluating an expr."
  [expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr]
     (/ (double (- (. System (nanoTime)) start#)) 1000000.0)))


(defmacro brtime
  "Like Clojure's macros time, but doesn't have side effect 
  (something prints) and returns vector where firs element is 
  time which is taken for evaluating an expr and second
  element is result of the expression."
  [expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr]
     [(/ (double (- (. System (nanoTime)) start#)) 1000000.0) ret#]))


(def names
  "List of names for elements from the SON model."
  ["MB" "TK" "UK1" "UK2" "UK9" "GT" "RT" "VI" "MN" "CRT"])

(def descs
  "List of descriptions for elements from the SON model."
  ["Description." 
   "Simple description." 
   "Long.... description." 
   "Integer" 
   "Double" 
   "String" 
   "Lang" 
   "Clojure" 
   "YZ" 
   "Scals"])

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
                        [UnknownNetwork :network] 5
                        [UnknownNetworkInterface :ni] 170 
                        [EthernetInterface :ei] 150
                        [UnknownLinkInterface :li] 150
                        [IPv4Interface :ipv4] 170
                        [IPNetwork :ipn] 5
                        [VLANInterface :vlan] 20}))))


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
          (instance? UnknownLinkInterface o) (.addLinkInterface (:device sm) o)
          (instance? EthernetInterface o) (.addLinkInterface (:device sm) o)
          (instance? VLANInterface o) (.addLinkInterface (:device sm) o)
          (instance? UnknownNetworkInterface o) (do (.addNetworkInterface (:li sm) o) 
                                                  (.setNetwork o (:network sm)))
          (instance? IPv4Interface o) (do (.addNetworkInterface (:ei sm) o) 
                                        (.setNetwork o (:ipn sm))))
    (assoc sm k o)))


(defn ^String gen-ip
  "Takes an random and generates casual IPv4 address."
  [^Random r]
  (str (.nextInt r 255) "."
       (.nextInt r 255) "."
       (.nextInt r 255) "."
       (.nextInt r 255)))


(defn ^String gen-mask
  "Takes an random and generates casual mask: 255.255.?.?."
  [^Random r]
  (str "255.255."
       (.nextInt r 255) "."
       (.nextInt r 255)))


(defn ^String gen-mac
  "Takes an random and generates casual MAC address."
  [^Random r]
  (let [v [0 1 2 3 4 5 6 7 8 9 \a \b \c \d \e \f]
        cv (count v)]
    (loop [n 12 res ""]
      (if (= n 0)
        (->> res (partition 2) (interpose \:) flatten (reduce str))
        (recur (dec n) (str res (v (.nextInt r cv))))))))


(defn gen-bd
  "Takes number of elements in BD, generates BD 
  and returns SON."
  [n]
  (let [r (Random.)
        clc (count classes)
        cn (count names)
        cd (count descs)
        se #(let [[cl k] (classes (.nextInt r clc))
                  cl (if (nil? %) cl %)
                  se (doto (.newInstance cl)
                       (.setName (str (.getSimpleName cl) "_" (names (.nextInt r cn))))
                       (.setDescription (descs (.nextInt r cd))))
                  se (if (instance? Floor se) (doto se (.setNumber (Integer. (.nextInt r 100)))) se) 
                  se (if (instance? Room se) (doto se (.setNumber (str (.nextInt r 100)))) se) 
                  se (if (instance? IPv4Interface se) 
                       (doto se (.setInetAddress (f/ip2b (gen-ip r))))
                       se)
                  se (if (instance? IPNetwork se) 
                       (doto se 
                         (.setAddress (f/ip2b (gen-ip r))) 
                         (.setMask (f/ip2b (gen-mask r))))
                       se)
                  se (if (instance? EthernetInterface se) 
                       (doto se (.setMACAddress (f/mac2b (gen-mac r))))
                       se)]
              [se k])
        sm (init-model {:building ((se Building) 0)
                        :floor ((se Floor) 0)
                        :room ((se Room) 0)
                        :occupancy ((se Occupancy) 0)
                        :sou ((se SimpleOU) 0)
                        :cou ((se CompositeOU) 0)
                        :device ((se Device) 0)
                        :network ((se UnknownNetwork) 0)
                        :ni ((se UnknownNetworkInterface) 0)
                        :ei ((se EthernetInterface) 0)
                        :li ((se UnknownLinkInterface) 0)
                        :ipn ((se IPNetwork) 0)
                        :ipv4 ((se IPv4Interface) 0)
                        :vlan ((se VLANInterface) 0)
                        :son (SON.)})]
    (loop [sm- sm, n- n]
      (if (<= n- 0)
        (:son sm-)
        (recur (change-model sm- (se nil)) (dec n-))))))

