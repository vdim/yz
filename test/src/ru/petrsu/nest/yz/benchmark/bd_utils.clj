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
    :doc "Usefull functions for working with db for benchmark."}
  (:require [ru.petrsu.nest.util.utils :as f])
  (:import (ru.petrsu.nest.son Building Floor Room
                               Occupancy SimpleOU CompositeOU
                               Device UnknownNetwork UnknownNetworkInterface EthernetInterface
                               UnknownLinkInterface IPv4Interface IPNetwork VLANInterface SON)
           (java.lang.management ManagementFactory)
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
  (something prints) and returns vector where first element is 
  time which is taken for evaluating an expr and second
  element is result of the expression."
  [expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr]
     [(/ (double (- (. System (nanoTime)) start#)) 1000000.0) ret#]))


(defn current-thread-time
  "Returns current thread time (cpu or user). type is keyword which
  defines type of time (:cpu or :user)."
  [type]
  (if (= type :user)
    (.getCurrentThreadUserTime (ManagementFactory/getThreadMXBean))
    (.getCurrentThreadCpuTime (ManagementFactory/getThreadMXBean))))


(defmacro thread-time
  "Returns vector where first element is value of current
  thread time (cpu or user; in fact difference between current
  thread time before evaluation of expr and after it; time is
  measurement in milliseconds) before evaluation of expr and 
  second element is result of expr's evaluation."
  [expr type]
  `(let [s-time# (current-thread-time ~type)
         ret# ~expr
         e-time# (current-thread-time ~type)]
     [(/ (double (- e-time# s-time#)) 1000000.0) ret#]))


(defmacro thread-memory
  "Returns vector where first element is used memory (in KBytes)
  after evaluation expression and running gc and second 
  element is result of expr's evaluation."
  [expr]
  `(let [ret# ~expr 
         _# (System/gc)
         e-mem# (.getUsed (.getHeapMemoryUsage (ManagementFactory/getMemoryMXBean)))]
     [(/ (double e-mem#) 1024.0) ret#]))


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


(def son-keywords
  "Keywords are common for both son models (lsm and jpa)."
  [:building :floor :room :occupancy :sou :cou 
   :device :network :ni :ei :li :ipv4 :ipn :vlan :son])


(def weights
  "List of weights of SON's element in model."
  [1 ; Building
   5 ; Floor
   50 ; Room
   5 ; Occupancy
   20 ; SimpleOU
   5 ; CompositeOU
   300 ; Device
   5 ; Network
   170 ; NetworkInterface
   150 ; EthernetInterface
   150 ; LinkInterface
   170 ; IPv4Interface
   5 ; IPNetwork
   20 ; VLANInterface
   0 ; SON
   ]) 


(defn classes
  "For each class from specified list of classes ('cls')
  creates sequence of this classes due to its wieghts from
  the bu/weights vector."
  [cls]
  (vec (concat (reduce (fn [r [k v]] (concat r (repeat v k)))
                       [] 
                       (partition 2 (interleave cls weights))))))


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


(defn instance
  "Creates instance of specified class 'cl' (class must be 
  from SON model) and then fills properties by random values."
  [cl]
  (let [r (Random.)
        n (.getSimpleName cl)
        ; Fills common properties for all sonelements.
        se (doto (.newInstance cl)
             (.setName (str n "_" (names (.nextInt r (count names)))))
             (.setDescription (descs (.nextInt r (count descs)))))
        ; Fills specified properties.
        ; We use simple name of class for comparing due to there are different
        ; models (ru.petrsu.nest.son.* for LocalSonManager 
        ; and ru.petrsu.nest.son.jpa.* for JPA EntityManager) with same names of classes.
        se (case n
             "Floor" (doto se (.setNumber (Integer. (.nextInt r 100))))
             "Room" (doto se (.setNumber (str (.nextInt r 100))))
             "IPv4Interface" (doto se (.setInetAddress (f/ip2b (gen-ip r))))
             "IPNetwork" (doto se (.setAddress (f/ip2b (gen-ip r))) (.setMask (f/ip2b (gen-mask r))))
             "EthernetInterface" (doto se (.setMACAddress (f/mac2b (gen-mac r))))
             ; Default value is self object.
             se)]
    se))


(defn init-model
  "Inits model."
  [cls]
  (let [state (zipmap son-keywords (map instance cls))]
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
      state)))


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


(defn gen-element
  "Selects element from vector classes due to a random number, 
  creates instance of this class and fills its properties due to the instance 
  function, and returns vector where first element is created 
  instance and second element is its keyword. 
  
  Example of a returned value: 
    [#<Device Device Device_TK> :device]"
  [classes cls]
  (let [r (Random.)
        cl (classes (.nextInt r (count classes)))
        k (nth son-keywords (.indexOf cls cl))
        se (instance cl)]
    [se k]))


(defn gen-bd-
  "Generates object graph of SON model due to specfied 
  amount of elements. Returns an instance of SON."
  [n f-change-model cls]
  (let [sm (init-model cls)
        a-sm (atom sm)
        lcls (classes cls) ; list with repeating classes (due to weights)
        _ (dorun (repeatedly n #(swap! a-sm f-change-model (gen-element lcls cls))))]
    (:son @a-sm)))

;;
;; Specific code for the LSM SON model.
;;

(def cls
  "List of lsm type of son model."
  [Building Floor Room Occupancy SimpleOU CompositeOU 
   Device UnknownNetwork UnknownNetworkInterface EthernetInterface 
   UnknownLinkInterface IPv4Interface IPNetwork VLANInterface SON])


(defn gen-bd
  "Takes number of elements in BD, creates an initial state for
  the SON model and passes its to the gen-bd- function."
  [n]
  (gen-bd- n change-model cls))
