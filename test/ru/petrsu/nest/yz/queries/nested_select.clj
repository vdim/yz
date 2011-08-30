(ns ru.petrsu.nest.yz.queries.nested-select
  ^{:author Vyacheslav Dimitrov
    :doc "Processes queries nested statements like this: 
         object1 (object2 (object3))."}
  (:use ru.petrsu.nest.yz.core 
        clojure.contrib.test-is)
  (:require [ru.petrsu.nest.yz.queries.core :as tc])
  (:import (ru.petrsu.nest.son SON Building Room Floor)))

;; Define model

(def f1_b1 (doto (Floor. 1) 
             (.addRoom (Room. "101")) 
             (.addRoom (Room. "102"))))

(def f2_b1 (doto (Floor. 2) 
             (.addRoom (Room. "201")) 
             (.addRoom (Room. "202"))))

(def f1_b2 (doto (Floor. 1) 
             (.addRoom (Room. "1001")) 
             (.addRoom (Room. "1002"))))


(def b1 (doto (Building.) (.setName "building") (.addFloor f1_b1) (.addFloor f2_b1)))
(def b2 (doto (Building.) (.setName "building") (.addFloor f1_b2)))

(def son (doto (SON.)
           (.addBuilding b1) 
           (.addBuilding b2)))

;; Define entity manager.

(use-fixtures :once (tc/setup [son]))


;; Define tests

(deftest select-b-and-f
         ^{:doc "Selects all Building and its Floor objects."}
         (is (tc/qstruct? (run-query "building (floor)" tc/mom tc/*em*)
                [[Building [[Floor []]]]])))

(deftest select-b-and-r
         ^{:doc "Selects all Building and its Room objects."}
         (is (tc/qstruct? (run-query "building (room)" tc/mom tc/*em*)
                          [[Building [[Room []]]]])))

(deftest select-r-and-b
         ^{:doc "Selects all Room and its Building objects."}
         (is (tc/qstruct? (run-query "room (building)" tc/mom tc/*em*)
                          [[Room [[Building []]]]])))

(deftest select-f-and-b
         ^{:doc "Selects all Floor and its Building objects."}
         (is (tc/qstruct? (run-query "floor (building)" tc/mom tc/*em*)
                [[Floor [[Building []]]]])))

(deftest select-bn-and-f
         ^{:doc "Selects all Building's name and its Floor objects."}
         (let [q (run-query "building.name (floor)" tc/mom tc/*em*)]
           (is (= ((q 0) 0) '("building")))
           (is (or (= (count (((q 0) 1) 0)) 4) (= (count (((q 0) 1) 0)) 2)))
           (is (or (= (count (((q 0) 3) 0)) 4) (= (count (((q 0) 3) 0)) 2)))
           (is (= (class ((((q 0) 1) 0) 0)) Floor))
           (is (= (class ((((q 0) 3) 0) 0)) Floor))
           (is (= ((q 0) 2) '("building")))))

(deftest select-b-f-r
         ^{:doc "Selects all Building, its Floor and its Room objects."}
         (is (tc/qstruct? (run-query "building (floor (room))" tc/mom tc/*em*)
                [[Building [[Floor [[Room []]]]]]])))

(deftest select-b-r-f
         ^{:doc "Selects all Building, its Room and its Floor objects."}
         (is (tc/qstruct? (run-query "building (room (floor))" tc/mom tc/*em*)
                [[Building [[Room [[Floor []]]]]]])))


