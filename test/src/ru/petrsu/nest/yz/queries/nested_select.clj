;;
;; Copyright 2011-2013 Vyacheslav Dimitrov <vyacheslav.dimitrov@gmail.com>
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
            [ru.petrsu.nest.yz.parsing :as p] 
            [ru.petrsu.nest.util.utils :as f]
            [ru.petrsu.nest.yz.queries.bd :as bd]
            [ru.petrsu.nest.yz.init :as init])
  (:import (ru.petrsu.nest.son 
             SON, Building, Room, Floor, Network,
             Device, IPNetwork, EthernetInterface, NetworkInterface,
             LinkInterface, IPv4Interface, UnknownLinkInterface)
           (ru.petrsu.nest.yz NotFoundElementException NotFoundPathException)))

;; Define entity manager.

(use-fixtures :once (tc/setup-son bd/son))


;; Define tests

(deftest select-b-and-f
         ^{:doc "Selects all Building and its Floor objects."}
         (is (tc/qstruct? "building (floor)"
                          [Building [Floor []]])))

(deftest select-b-and-r
         ^{:doc "Selects all Building and its Room objects."}
         (is (tc/qstruct? "building (room)"
                          [Building [Room []]])))

(deftest select-r-and-b
         ^{:doc "Selects all Room and its Building objects."}
         (is (tc/qstruct? "room (building)"
                          [Room [Building []]])))

(deftest select-f-and-b
         ^{:doc "Selects all Floor and its Building objects."}
         (is (tc/qstruct? "floor (building)"
                          [Floor [Building []]])))

(deftest select-bn-and-f
         ^{:doc "Selects all Building's name and its Floor objects."}
         (let [q (flatten (tc/rows-query "building[name] (floor)"))]
           (is (tc/eq-colls q ["MB" "MB" "MB" "TK" bd/f1_b1 bd/f2_b1 bd/f3_b1 bd/f1_b2]))))

;(deftest select-b-f-r
;         ^{:doc "Selects all Building, its Floor and its Room objects."}
;         (is (tc/qstruct? "building (floor (room))"
;                          [Building [Floor [Room []]]])))

(deftest select-b-r-f
         ^{:doc "Selects all Building, its Room and its Floor objects."}
         (is (tc/qstruct? "building (room (floor))"
                          [Building [Room [Floor []]]])))

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
         (is (tc/qstruct? "device (network)" [Device [IPNetwork []]]))
         (is (tc/qstruct? "device (ipnetwork)" [Device [IPNetwork []]]))
         (is (tc/qstruct? "ipnetwork (device)" [IPNetwork [Device []]]))
         (is (tc/qstruct? "device (ethernetinterface)" [Device [EthernetInterface []]]))
         (is (tc/qstruct? "ethernetinterface (device)" [EthernetInterface [Device []]])))


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




(deftest neg-parse-nest-queries
         ^{:doc "Contains tests which are thrown exceptions."}
         (is (thrown? NotFoundElementException (tc/qparse "somelem")))
         (is (thrown? NotFoundElementException (tc/qparse "building (somelem)")))
         (is (thrown? NotFoundElementException (tc/qparse "building (room, somelem)")))
         (is (thrown? NotFoundElementException (tc/qparse "building (somelem, room)")))
         (is (thrown? NotFoundElementException (tc/qparse "building (room, floor, somelem)")))
         (is (thrown? NotFoundElementException (tc/qparse "building (room, floor, device, somelem)")))
         (is (thrown? NotFoundElementException (tc/qparse "building (room (somelem))")))
         (is (thrown? NotFoundElementException (tc/qparse "building (room (floor (somelem)))")))
         (is (thrown? NotFoundElementException (tc/qparse "building (somelem)")))
         (is (thrown? NotFoundElementException (tc/qparse "building, somelem")))
         (is (thrown? NotFoundElementException (tc/qparse "somelem, building"))))


(deftest dp-in-nest
         ^{:doc "Checks selecting default property and self object."}
         (let [f #(distinct (flatten (tc/rows-query %)))]
           (is (tc/eq-colls (f "building (floor[&])") [bd/b1 bd/b2 bd/f1_b1 bd/f2_b1 bd/f3_b1 bd/f1_b2]))
           (is (tc/eq-colls (f "building (floor[&.])") [bd/b1 bd/b2 1 2 3]))))


(deftest late-binding
         ^{:doc "Tests for late binding (in case there is no path from
                cl-source to cl-target and paths from children of cl-source
                to cl-target is used.)"}
         (is (tc/qparse "aou (room)"))
         (is (tc/eq-colls (tc/rows-query "aou (room)") 
                          [[bd/rootCompositeOU]
                           [bd/cou_d2 bd/r1001_f1_b2]
                           [bd/cou_d1 bd/r102_f1_b1]
                           [bd/sou2_d2 bd/r1001_f1_b2]
                           [bd/sou1_d2]
                           [bd/sou1_d1 bd/r102_f1_b1]
                           [bd/sou2_d1]]))
         )


(deftest prop-medium
         ^{:doc "Tests queries with prop as medium link."}
         (let [sou (doto (ru.petrsu.nest.son.SimpleOU.) (.setName "S_TE"))
               _ (.addOU bd/rootCompositeOU sou)]
           (is (tc/eq-colls (tc/rows-query "cou.parent.sou") [[sou] [sou]]))))


(deftest abs-and-ints
         ^{:doc "Tests queries with abstract or interface classes
                (in fact paths to child)."}
         (is (tc/eq-colls (tc/rows-query "oe (device)") 
                          [[bd/sou1_d1 bd/d1]
                           [bd/sou2_d1]
                           [bd/sou1_d2]
                           [bd/sou2_d2 bd/rootDevice]
                           [bd/cou_d1 bd/d1]
                           [bd/cou_d2 bd/rootDevice]
                           [bd/rootCompositeOU]]
                          ))
         (is (tc/eq-colls (tc/rows-query "device (oe)") 
                          [[bd/d1 bd/sou1_d1]
                           [bd/rootDevice bd/sou2_d2]
                           [bd/d1 bd/cou_d1]
                           [bd/rootDevice bd/cou_d2]]
                          ))
         (is (tc/eq-colls (tc/rows-query "spatialelement (oe)") 
                          [[bd/b1 bd/sou1_d1] 
                           [bd/b1 bd/cou_d1]
                           [bd/b2 bd/sou2_d2] 
                           [bd/b2 bd/cou_d2]
                           [bd/f1_b1 bd/sou1_d1]
                           [bd/f1_b1 bd/cou_d1]
                           [bd/f1_b2 bd/sou2_d2]
                           [bd/f1_b2 bd/cou_d2]
                           [bd/f2_b1]
                           [bd/f3_b1]
                           [bd/r1001_f1_b2 bd/sou2_d2]
                           [bd/r1001_f1_b2 bd/cou_d2]
                           [bd/r1002_f1_b2]
                           [bd/r101_f1_b1]
                           [bd/r201_f2_b1]
                           [bd/r202_f2_b1]
                           [bd/r102_f1_b1 bd/sou1_d1]
                           [bd/r102_f1_b1 bd/cou_d1]]
                          ))
         )


(deftest not-found-paths
         ^{:doc "Tests queries where path must be not found."}
         (is (thrown? NotFoundPathException (tc/qparse "aou (son)")))
         (is (thrown? NotFoundPathException (p/parse "room (nse)" init/ex-mom)))
         (is (thrown? NotFoundPathException (p/parse "building (building)" init/ex-mom)))
         )

(deftest abs-and-ints
         ^{:doc "Tests queries with abstract or interface classes
                (in fact paths to child)."}
         (is (tc/eq-colls (tc/rows-query "building (room.floor)") 
                          [[bd/b1 bd/f1_b1]
                           [bd/b1 bd/f1_b1]
                           [bd/b1 bd/f2_b1]
                           [bd/b1 bd/f2_b1]
                           [bd/b2 bd/f1_b2]
                           [bd/b2 bd/f1_b2]]))
         (is (tc/eq-colls (tc/rows-query "building (room.floor (building))") 
                          [[bd/b1 bd/f1_b1 bd/b1]
                           [bd/b1 bd/f1_b1 bd/b1]
                           [bd/b1 bd/f2_b1 bd/b1]
                           [bd/b1 bd/f2_b1 bd/b1]
                           [bd/b2 bd/f1_b2 bd/b2]
                           [bd/b2 bd/f1_b2 bd/b2]]))
         (let [old-rooms (.getRooms bd/f3_b1)
               _ (.addRoom bd/f3_b1 (doto (Room.) (.setNumber "333")))
               r (tc/eq-colls (tc/rows-query "building (room.floor)") 
                              [[bd/b1 bd/f1_b1]
                               [bd/b1 bd/f1_b1]
                               [bd/b1 bd/f2_b1]
                               [bd/b1 bd/f2_b1]
                               [bd/b1 bd/f3_b1]
                               [bd/b2 bd/f1_b2]
                               [bd/b2 bd/f1_b2]])
               _ (.setRooms bd/f3_b1 old-rooms)]
           (is r))
         (is (tc/eq-colls (tc/rows-query "building (room.floor)") 
                          [[bd/b1 bd/f1_b1]
                           [bd/b1 bd/f1_b1]
                           [bd/b1 bd/f2_b1]
                           [bd/b1 bd/f2_b1]
                           [bd/b2 bd/f1_b2]
                           [bd/b2 bd/f1_b2]]))
         )
