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

(ns ru.petrsu.nest.yz.queries.nested-select
  ^{:author "Vyacheslav Dimitrov"
    :doc "Processes queries nested statements like this: 
         object1 (object2 (object3))."}
  (:use ru.petrsu.nest.yz.core 
        clojure.test)
  (:require [ru.petrsu.nest.yz.queries.core :as tc]
            [ru.petrsu.nest.util.utils :as f]
            [ru.petrsu.nest.yz.queries.bd :as bd])
  (:import (ru.petrsu.nest.son 
             SON, Building, Room, Floor, Network,
             Device, IPNetwork, EthernetInterface, NetworkInterface,
             LinkInterface, IPv4Interface, UnknownLinkInterface)))

;; Define model

(def f1_b1 (doto (Floor.) 
             (.setNumber (Integer. 1))
             (.addRoom (doto (Room.) (.setNumber "101"))) 
             (.addRoom (doto (Room.) (.setNumber "102")))))

(def f2_b1 (doto (Floor.) 
             (.setNumber (Integer. 2))
             (.addRoom (doto (Room.) (.setNumber "201"))) 
             (.addRoom (doto (Room.) (.setNumber "202")))))

(def f1_b2 (doto (Floor.) 
             (.setNumber (Integer. 1))
             (.addRoom (doto (Room.) (.setNumber "1001"))) 
             (.addRoom (doto (Room.) (.setNumber "1002")))))

(def b1 (doto (Building.) (.setName "building") (.addFloor f1_b1) (.addFloor f2_b1)))
(def b2 (doto (Building.) (.setName "building") (.addFloor f1_b2)))

(def son (doto (SON.)
           (.addBuilding b1) 
           (.addBuilding b2)
           (.setRootDevice bd/rootDevice)))

;; Define entity manager.

(use-fixtures :once (tc/setup-son son))


;; Define tests

(deftest select-b-and-f
         ^{:doc "Selects all Building and its Floor objects."}
         (is (tc/qstruct? "building (floor)"
                          [[Building [[Floor []]]]])))

(deftest select-b-and-r
         ^{:doc "Selects all Building and its Room objects."}
         (is (tc/qstruct? "building (room)"
                          [[Building [[Room []]]]])))

(deftest select-r-and-b
         ^{:doc "Selects all Room and its Building objects."}
         (is (tc/qstruct? "room (building)"
                          [[Room [[Building []]]]])))

(deftest select-f-and-b
         ^{:doc "Selects all Floor and its Building objects."}
         (is (tc/qstruct? "floor (building)"
                          [[Floor [[Building []]]]])))

(deftest select-bn-and-f
         ^{:doc "Selects all Building's name and its Floor objects."}
         (let [q (tc/r-query "building.name (floor)")]
           (is (= ((q 0) 0) '("building")))
           (is (or (= (count (((q 0) 1) 0)) 4) (= (count (((q 0) 1) 0)) 2)))
           (is (or (= (count (((q 0) 3) 0)) 4) (= (count (((q 0) 3) 0)) 2)))
           (is (= (class ((((q 0) 1) 0) 0)) Floor))
           (is (= (class ((((q 0) 3) 0) 0)) Floor))
           (is (= ((q 0) 2) '("building")))))

(deftest select-b-f-r
         ^{:doc "Selects all Building, its Floor and its Room objects."}
         (is (tc/qstruct? "building (floor (room))"
                          [[Building [[Floor [[Room []]]]]]])))

(deftest select-b-r-f
         ^{:doc "Selects all Building, its Room and its Floor objects."}
         (is (tc/qstruct? "building (room (floor))"
                          [[Building [[Room [[Floor []]]]]]])))
(comment
(deftest select-empty-links
         ^{:doc "Tests emptiness links."}
         (is (tc/qstruct? "building (device)"
                          [[Building [[nil []]]]]))
         (is (tc/qstruct? "building (device (network))"
                          [[Building [[nil [[nil []]]]]]])))
)

(deftest select-inheritance
         ^{:doc "Tests inheritance queries."}
         (is (tc/qstruct? "device (network)" [[Device [[IPNetwork []]]]]))
         (is (tc/qstruct? "device (ipnetwork)" [[Device [[IPNetwork []]]]]))
         (is (tc/qstruct? "ipnetwork (device)" [[IPNetwork [[Device []]]]]))
         (is (tc/qstruct? "device (ethernetinterface)" [[Device [[EthernetInterface []]]]]))
         (is (tc/qstruct? "ethernetinterface (device)" [[EthernetInterface [[Device []]]]])))


(deftest select-inheritance-2
         ^{:doc "Tests query like this device (ei). (Problem with inheritance.)"}
         (let [f (fn [cl, query, n] 
                   (every? true? (map #(if (< (count %) (inc n)) 
                                         true 
                                         (= cl (.getClass (% n)))) 
                                      (tc/rows-query query))))]
           (is (f EthernetInterface "device (ei)" 1))
           (is (f Device "ei (device)" 1))
           (is (f EthernetInterface "ei (device)" 0))
           (is (f UnknownLinkInterface "device (uli)" 1))
           (is (f IPv4Interface "device (ip4i)" 1))
           (is (f IPNetwork "device (ipnetwork)" 1)))
         (let [f (fn [cl, query] 
                   (every? true? (map #(if (< (count %) 2) 
                                         true 
                                         (instance? cl (% 1))) 
                                      (tc/rows-query query))))]
           (is (f LinkInterface "device (li)"))
           (is (f NetworkInterface "device (ni)"))
           (is (f Network "device (network)"))))


