(ns ru.petrsu.nest.yz.queries.simple-select
  ^{:author Vyacheslav Dimitrov
    :doc "Processes simple select queries."}
  (:use clojure.contrib.test-is)
  (:require [ru.petrsu.nest.yz.queries.core :as tc])
  (:import (ru.petrsu.nest.son SON Building Room Floor NetworkInterface IPv4Interface)))


;; Define model

(def son (doto (SON.)
           (.addBuilding (doto (Building.) (.setName "building1") 
                           (.addFloor (Floor.)) (.addFloor (Floor.)))) 
           (.addBuilding (doto (Building.) (.setName "building2") 
                           (.addFloor (Floor.))))))


;; Define entity manager.

(use-fixtures :once (tc/setup [son]))


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

(deftest select-rooms
         ^{:doc "Selects all Room objects. Result should be empty."}
         (is (tc/check-query "room" [[]])))

(deftest select-b-names
         ^{:doc "Selects building's names"}
         (let [q (tc/r-query "building.name")]
           (is (or (= q [['("building1") [] '("building2") []]])
                   (= q [['("building2") [] '("building1") []]])))))

(deftest select-props
         ^{:doc "Checks props"}
         (is (= (tc/check-query "floor.number" [['(0) [] '(0) [] '(0) []]])))
         (is (= (tc/check-query "floor.name" [['(nil) [] '(nil) [] '(nil) []]])))
         (is (= (tc/check-query "building.floor.number" [['(0) [] '(0) [] '(0) []]]))))

(deftest select-by-short-name
         ^{:doc "Selects object by short name"}
         (is (tc/qstruct? "b" [[Building []]]))
         (is (tc/qstruct? "f" [[Floor []]]))
         (is (tc/check-query "ni" [[]]))
         (is (tc/check-query "ip4i" [[]]))
         (is (tc/qstruct? "f (b)" [[Floor [[Building []]]]]))
         (is (tc/qstruct? "floor (b)" [[Floor [[Building []]]]]))
         (is (tc/qstruct? "f (building)" [[Floor [[Building []]]]])))

