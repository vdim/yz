(ns ru.petrsu.nest.yz.queries.funcs
  ^{:author "Vyacheslav Dimitrov"
    :doc "Test queries within some functions."}
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

(def f3_b1 (doto (Floor. 3) 
             (.addRoom (Room. "301")) 
             (.addRoom (Room. "302"))))

(def f4_b1 (doto (Floor. 4) 
             (.addRoom (Room. "101")) 
             (.addRoom (Room. "401")) 
             (.addRoom (Room. "402"))))


(def f1_b2 (doto (Floor. 1) 
             (.addRoom (Room. "101")) 
             (.addRoom (Room. "1001")) 
             (.addRoom (Room. "201")) 
             (.addRoom (Room. "1002"))))


(def b1 (doto (Building.) (.setName "b1") (.setAddress "Street1") 
          (.addFloor f1_b1) (.addFloor f2_b1)
          (.addFloor f3_b1) (.addFloor f4_b1)))
(def b2 (doto (Building.) (.setName "b2") (.setAddress "Street2") (.addFloor f1_b2)))

(def b3 (doto (Building.) (.setName "b3") (.setAddress "Street1")))

(def son (doto (SON.)
           (.addBuilding b1) 
           (.addBuilding b2)
           (.addBuilding b3))) 


;; Define entity manager.

(use-fixtures :once (tc/setup [son]))

;; Define tests

(deftest t-str
         ^{:doc "Test calling function (count)."}
         (is (= (nth (tc/r-query "@(str \"1\" \"2\")") 0) "12"))
         (is (= (nth (tc/r-query "@(str \"asdf\" 1 \"qwer\")") 0) "asdf1.0qwer")))
        

(deftest t-count
         ^{:doc "Tests calling function (count)."}
         (let [f-c #(= (nth (tc/r-query %1) 0) %2)]
           (is (f-c "@(count %building')" 3))
           (is (f-c "@(count $building')" 1))
           (is (f-c "@(count `building')" 3))
           (is (f-c "@(count %room')" 13))
           (is (f-c "@(count `room')" 13))
           (is (f-c "@(count `room#(number=\"101\")')" 3))
           (is (f-c "@(count `room#(number=\"102\")')" 1))
           (is (f-c "@(count `room#(number=(\"101\" or \"102\"))')" 4))
           (is (f-c "@(count `room#(number=(\"101\" and \"102\"))')" 0))
           (is (f-c "@(count %floor')" 5))
           (is (f-c "@(count `floor')" 5))
           (is (f-c "@(count `device')" 0))))


(deftest t-count-prop
         ^{:doc "Tests calling function (count) from list of properties."}
         (let [f-c #(= (nth (nth (tc/rows-query %1) 0) %2) %3)]
           (is (f-c "building#(name=\"b1\")[& @(count `floor')]" 0 b1))
           (is (f-c "building#(name=\"b1\")[& @(count `floor')]" 1 4))
           (is (f-c "building#(name=\"b1\")[& @(count `room')]" 1 9))
           (is (f-c "building#(name=\"b1\")[& @(count `room') @(count `floor')]" 0 b1))
           (is (f-c "building#(name=\"b1\")[& @(count `room') @(count `floor')]" 1 9))
           (is (f-c "building#(name=\"b1\")[& @(count `room') @(count `floor')]" 2 4))
           (is (f-c "building#(name=\"b1\")[@(count `room') & @(count `floor')]" 0 9))
           (is (f-c "building#(name=\"b1\")[@(count `room') & @(count `floor')]" 1 b1))
           (is (f-c "building#(name=\"b1\")[@(count `room') & @(count `floor')]" 2 4))
           (is (f-c "building#(name=\"b1\")[@(count `room') @(count `floor') &]" 0 9))
           (is (f-c "building#(name=\"b1\")[@(count `room') @(count `floor') &]" 1 4))
           (is (f-c "building#(name=\"b1\")[@(count `room') @(count `floor') &]" 2 b1))
           (is (f-c "building#(name=\"b1\")[& @(count `floor#(number=1)')]" 1 1))
           (is (f-c "building#(name=\"b1\")[& @(count `floor#(number=(1 or 2))')]" 1 2))
           (is (f-c "building#(name=\"b1\")[& @(count `floor#(number=(1 and 2))')]" 1 0))

           (is (f-c "building#(name=\"b2\")[& @(count `floor')]" 0 b2))
           (is (f-c "building#(name=\"b2\")[& @(count `floor')]" 1 1))
           (is (f-c "building#(name=\"b2\")[& @(count `room')]" 1 4))

           (is (f-c "building#(name=\"b3\")[& @(count `floor')]" 0 b3))
           (is (f-c "building#(name=\"b3\")[& @(count `floor')]" 1 0))
           (is (f-c "building#(name=\"b3\")[& @(count `room')]" 1 0))))


(deftest t-count-pred
         ^{:doc "Tests calling function (count) from predicates."}
         (let [f-c #(= (nth (nth (tc/rows-query %1) 0) %2) %3)]
           (is (f-c "building#(@(count `floor') > 1)" 0 b1))
           (is (f-c "building#(@(count `floor') = 1)" 0 b2))
           (is (f-c "building#(@(count `floor') < 1)" 0 b3)))
         (is (= (tc/rows-query "building#(@(count `floor') > 4)") ()))
         (is (= (count (tc/rows-query "building#(@(count `floor')=(4 or 1))")) 2))
         (is (= (count (tc/rows-query "building#(@(count `floor')=(4 or 1 or 0))")) 3))
         (is (= (count (tc/rows-query "building#(@(count `floor')=(4 or 0))")) 2)))
