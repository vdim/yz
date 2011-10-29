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

(ns ru.petrsu.nest.yz.queries.simple-select
  ^{:author "Vyacheslav Dimitrov"
    :doc "Processes simple select queries."}
  (:use clojure.test)
  (:require [ru.petrsu.nest.yz.queries.core :as tc])
  (:import (ru.petrsu.nest.son SON Building Room Floor NetworkInterface IPv4Interface)))


;; Define model

(def son (doto (SON.)
           (.addBuilding (doto (Building.) (.setName "building1") 
                           (.addFloor (Floor.)) (.addFloor (Floor.)))) 
           (.addBuilding (doto (Building.) (.setName "building2") 
                           (.addFloor (doto (Floor.) (.addRoom (doto (Room.) (.setNumber "1")))))))))


;; Define entity manager.

(use-fixtures :once (tc/setup-son son))


;; Define tests

(deftest select-buildings
         ^{:doc "Selects all Building objects.
                Result should be (for our son) like this: [[#<Building Building> [], #<Building Building> []]]"}
         (is (tc/check-query "building" [[Building [], Building []]])))

(deftest select-floors
         ^{:doc "Selects all Floor objects.
                Result should be like this: 
                [#<Floor Floor 0> [], #<Floor Floor 0> [], #<Floor Floor 0> []]]"}
         (is (tc/check-query "floor" [[Floor [], Floor[], Floor[]]])))

(deftest select-devices
         ^{:doc "Selects all Device objects. Result should be empty."}
         (is (tc/check-query "device" [[]])))

(deftest select-b-names
         ^{:doc "Selects building's names"}
         (let [q (tc/r-query "building.name")]
           (is (or (= q [['("building1") [] '("building2") []]])
                   (= q [['("building2") [] '("building1") []]])))))

(deftest select-prop
         ^{:doc "Checks props"}
         (is (= (tc/check-query "floor.number" [['(0) [] '(0) [] '(0) []]])))
         (is (= (tc/check-query "floor.name" [['(nil) [] '(nil) [] '(nil) []]])))
         (is (= (tc/check-query "building.floor.number" [['(0) [] '(0) [] '(0) []]]))))

(deftest select-props
         ^{:doc "Checks props"}
         (is (= (tc/check-query "floor[number name]" [['(0 nil) [] '(0 nil) [] '(0 nil) []]])))
         (is (= (tc/check-query "floor[name number]" [['(nil 0) [] '(nil 0) [] '(nil 0) []]])))
         (is (= (tc/check-query "room[number]" [['("1") []]]))))

(deftest select-by-short-name
         ^{:doc "Selects object by short name"}
         (is (tc/qstruct? "b" [[Building []]]))
         (is (tc/qstruct? "f" [[Floor []]]))
         (is (tc/check-query "ni" [[]]))
         (is (tc/check-query "ip4i" [[]]))
         (is (tc/qstruct? "f (b)" [[Floor [[Building []]]]]))
         (is (tc/qstruct? "floor (b)" [[Floor [[Building []]]]]))
         (is (tc/qstruct? "f (building)" [[Floor [[Building []]]]])))

