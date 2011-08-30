(ns ru.petrsu.nest.yz.queries.simple-select
  ^{:author Vyacheslav Dimitrov
    :doc "Processes simple select queries."}
  (:use ru.petrsu.nest.yz.core 
        clojure.contrib.test-is)
  (:require [ru.petrsu.nest.yz.queries.core :as tc])
  (:import (ru.petrsu.nest.son SON Building Room Floor)))


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
         (is (tc/check-query (run-query "building" tc/mom tc/*em*) 
                             [[Building [], Building []]])))

(deftest select-floors
         ^{:doc "Selects all Floor objects.
                Result should be like this: 
                [#<Floor Floor 0> [], #<Floor Floor 0> [], #<Floor Floor 0> []]]"}
         (is (tc/check-query (run-query "floor" tc/mom tc/*em*) 
                             [[Floor [], Floor[], Floor[]]])))

(deftest select-rooms
         ^{:doc "Selects all Room objects. Result should be empty."}
         (is (tc/check-query (run-query "room" tc/mom tc/*em*) [[]])))

(deftest select-b-names
         ^{:doc "Selects building's names"}
         (let [q (run-query "building.name" tc/mom tc/*em*)]
           (is (or (= q [['("building1") [] '("building2") []]])
                   (= q [['("building2") [] '("building1") []]])))))

(deftest select-props
         ^{:doc "Checks props"}
         (is (= (run-query "floor.number" tc/mom tc/*em*)
                          [['(0) [] '(0) [] '(0) []]]))
         (is (= (run-query "floor.name" tc/mom tc/*em*)
                          [['(nil) [] '(nil) [] '(nil) []]]))
         (is (= (run-query "building.floor.number" tc/mom tc/*em*)
                          [['(0) [] '(0) [] '(0) []]])))
