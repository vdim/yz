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

(ns ru.petrsu.nest.yz.queries.simple-select
  ^{:author "Vyacheslav Dimitrov"
    :doc "Processes simple select queries."}
  (:use clojure.test)
  (:require [ru.petrsu.nest.yz.queries.core :as tc] 
            [ru.petrsu.nest.yz.core :as yz]
            [ru.petrsu.nest.yz.queries.bd :as bd]
            [ru.petrsu.nest.yz.hb-utils :as hb])
  (:import (ru.petrsu.nest.son SON Building Room Floor NetworkInterface IPv4Interface)))


;; Define model

(def f1_b2 (doto (Floor.) (.addRoom (doto (Room.) (.setNumber "1")))))
(def f1_b1 (Floor.))
(def f2_b1 (Floor.))
(def b1 (doto (Building.) (.setName "building1") (.addFloor f1_b1) (.addFloor f2_b1)))
(def b2 (doto (Building.) (.setName "building2") (.addFloor f1_b2)))

(def son (doto (SON.) (.addBuilding b1) (.addBuilding b2)))


;; Define entity manager.

(use-fixtures :once (tc/setup-son son))


;; Define tests

(deftest select-buildings
         ^{:doc "Selects all Building objects.
                Result should be (for our son) like this: [[#<Building Building> [], #<Building Building> []]]"}
         (is (tc/check-query "building" [Building [], Building []])))

(deftest select-floors
         ^{:doc "Selects all Floor objects.
                Result should be like this: 
                [#<Floor Floor 0> [], #<Floor Floor 0> [], #<Floor Floor 0> []]]"}
         (is (tc/check-query "floor" [Floor [], Floor[], Floor[]])))

(deftest select-devices
         ^{:doc "Selects all Device objects. Result should be empty."}
         (is (tc/check-query "device" [])))

(deftest select-b-names
         ^{:doc "Selects building's names"}
         (let [q (tc/r-query "building.name")]
           (is (or (= q ['("building1") [] '("building2") []])
                   (= q ['("building2") [] '("building1") []])))))


(deftest select-prop
         ^{:doc "Checks props"}
         (is (= (tc/r-query "floor.number") ['(nil) [] '(nil) [] '(nil) []]))
         (is (= (tc/r-query "floor.name") ['(nil) [] '(nil) [] '(nil) []]))
         (is (= (tc/r-query "building.floor.number") ['(nil) [] '(nil) [] '(nil) []])))


(deftest select-props
         ^{:doc "Checks props"}
         (is (= (tc/r-query "floor[number name]") ['(nil nil) [] '(nil nil) [] '(nil nil) []]))
         (is (= (tc/r-query "floor[name number]") ['(nil nil) [] '(nil nil) [] '(nil nil) []]))
         (is (tc/eq-colls (flatten (tc/rows-query "building[name]")) ["building1" "building2"]))
         (is (= (tc/r-query "room[number]") ['("1") []])))


(deftest select-self-and-dp
         ^{:doc "Checks selecting default property and self object."}
         (is (= (tc/qstruct? "floor[&]" [Floor []])))
         (is (= (tc/r-query "room[&.]") ['("1") []])))


(deftest select-by-short-name
         ^{:doc "Selects object by short name"}
         (is (tc/qstruct? "b" [Building []]))
         (is (tc/qstruct? "f" [Floor []]))
         (is (tc/check-query "ni" []))
         (is (tc/check-query "ip4i" []))
         (is (tc/qstruct? "f (b)" [Floor [Building []]]))
         (is (tc/qstruct? "floor (b)" [Floor [Building []]]))
         (is (tc/qstruct? "f (building)" [Floor [Building []]])))


(deftest not-found-property
         ^{:doc "Tests :not-found returned value of 
                the get-fv function using bd/mem database."}
         (let [f #(let [r (yz/pquery %1 (hb/mom-from-file "nest.mom") bd/mem)]
                    (if (:error r)
                      (throw (:thrwable r))
                      (:rows r)))]
           (is (= (count (f "li")) 6))
           (is (= (count (f "li[MACAddress]")) 5))
           (is (= (count (f "li[& MACAddress]")) 5))
           (is (= (count (f "li[MACAddress &]")) 5))
           (is (tc/eq-colls (f "cou.room") [[bd/r1001_f1_b2] [bd/r102_f1_b1]]))
           (is (tc/eq-colls (f "cou (room)") [[bd/rootCompositeOU] 
                                              [bd/cou_d2 bd/r1001_f1_b2] 
                                              [bd/cou_d1 bd/r102_f1_b1]]))))
