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


(def b1 (doto (Building.) (.addFloor f1_b1) (.addFloor f2_b1)))
(def b2 (doto (Building.) (.addFloor f1_b2)))

(def son (doto (SON.)
           (.addBuilding b1) 
           (.addBuilding b2)))

;; Define entity manager.

(declare *em*)
(defn setup [f]
    (binding [*em* (tc/create-em [son])] (f) (.close *em*)))

(use-fixtures :once setup)


;; Define tests

(deftest select-floors-and-building
         ^{:doc "Selects all Building and its Floor objects."}
         (let [q (run-query "building (floor)" tc/mom *em*)]
           (is (= 1 (count q)))
           (is (= 2 (count (q 0))))
           (let [f-b1 (get (q 0) b1), ; f-b1 is floors of b1 building
                 f-b2 (get (q 0) b2)] ; f-b2 is floors of b2 building
             (is (not (nil? f-b1)))
             (is (= 1 (count f-b1)))
             (is (= 2 (count (f-b1 0)))) ; Check the number of floors for object b1.
             (is (every? #(instance? Floor %) (keys (f-b1 0))))  ; Check class of floors for object b1.
             (is (not (nil? f-b2)))
             (is (= 1 (count f-b2)))
             (is (= 1 (count (f-b2 0)))) ; Check the number of floors for object b2.
             (is (every? #(instance? Floor %) (keys (f-b2 0)))))))  ; Check class of floors for object b1.

(deftest select-floors-and-rooms
         ^{:doc "Selects all Building and its Room objects."}
         (let [q (run-query "building (room)" tc/mom *em*)]
           (is (= 1 (count q)))
           (is (= 2 (count (q 0))))
           (let [r-b1 (get (q 0) b1), ; r-b1 is rooms of b1 building
                 r-b2 (get (q 0) b2)] ; r-b2 is rooms of b2 building
             (is (not (nil? r-b1)))
             (is (= 1 (count r-b1)))
             (is (= 4 (count (r-b1 0)))) ; Check the number of rooms for object b1.
             (is (every? #(instance? Room %) (keys (r-b1 0))))  ; Check class of rooms for object b1.
             (is (not (nil? r-b2)))
             (is (= 1 (count r-b2)))
             (is (= 2 (count (r-b2 0)))) ; Check the number of rooms for object b2.
             (is (every? #(instance? Room %) (keys (r-b2 0)))))))  ; Check class of rooms for object b1.

