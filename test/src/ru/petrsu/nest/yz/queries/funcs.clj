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

(ns ru.petrsu.nest.yz.queries.funcs
  ^{:author "Vyacheslav Dimitrov"
    :doc "Test queries within some functions."}
  (:use ru.petrsu.nest.yz.core 
        clojure.test)
  (:require [ru.petrsu.nest.yz.queries.core :as tc])
  (:import (ru.petrsu.nest.son SonElement SON Building Room Floor)))


;; Define model


(def f1_b1 (doto (Floor.) 
             (.setNumber (Integer. 1))
             (.addRoom (doto (Room.) (.setNumber "101"))) 
             (.addRoom (doto (Room.) (.setNumber "102")))))

(def f2_b1 (doto (Floor.) 
             (.setNumber (Integer. 2))
             (.addRoom (doto (Room.) (.setNumber "201"))) 
             (.addRoom (doto (Room.) (.setNumber "202")))))

(def f3_b1 (doto (Floor.) 
             (.setNumber (Integer. 3))
             (.addRoom (doto (Room.) (.setNumber "301"))) 
             (.addRoom (doto (Room.) (.setNumber "302")))))

(def f4_b1 (doto (Floor.) 
             (.setNumber (Integer. 4))
             (.addRoom (doto (Room.) (.setNumber "101"))) 
             (.addRoom (doto (Room.) (.setNumber "401"))) 
             (.addRoom (doto (Room.) (.setNumber "402")))))


(def f1_b2 (doto (Floor.) 
             (.setNumber (Integer. 1))
             (.addRoom (doto (Room.) (.setNumber "101"))) 
             (.addRoom (doto (Room.) (.setNumber "1001"))) 
             (.addRoom (doto (Room.) (.setNumber "1002"))) 
             (.addRoom (doto (Room.) (.setNumber "201")))))


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

(use-fixtures :once (tc/setup-son son))

;; Define tests

(deftest t-str
         ^{:doc "Test calling function (count)."}
         (is (= (nth (tc/r-query "@(str \"1\" \"2\")") 0) ["12" []]))
         (is (= (nth (tc/r-query "@(str \"asdf\" 1 \"qwer\")") 0) ["asdf1qwer" []]))
         (is (= (nth (tc/r-query "@(str \"asdf\" 1.0 \"qwer\")") 0) ["asdf1.0qwer" []])))
        

(deftest t-count
         ^{:doc "Tests calling function (count)."}
         (let [f-c #(= (nth (tc/r-query %1) 0) %2)]
           (is (f-c "@(count il:`building')" [3 []]))
           (is (f-c "@(count de:`building')" [1 [] 1 [] 1 []]))
           (is (f-c "@(count `building')" [3 []]))
           (is (f-c "@(count il:`room')" [13 []]))
           (is (f-c "@(count `room')" [13 []]))
           (is (f-c "@(count `room#(number=\"101\")')" [3 []]))
           (is (f-c "@(count `room#(number=\"102\")')" [1 []]))
           (is (f-c "@(count `room#(number=(\"101\" or \"102\"))')" [4 []]))
           (is (f-c "@(count `room#(number=(\"101\" and \"102\"))')" [0 []]))
           (is (f-c "@(count il:`floor')" [5 []]))
           (is (f-c "@(count `floor')" [5 []]))
           (is (f-c "@(count `device')" [0 []]))))


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
           (is (f-c "building#(@(count `floor') < 1)" 0 b3))
           (is (f-c "floor#(number = @(max 1 2 3))" 0 f3_b1))
           (is (f-c "floor#(number = @(count `room'))" 0 f2_b1)))
         (is (= (tc/rows-query "building#(@(count `floor') > 4)") ()))
         (is (= (count (tc/rows-query "building#(@(count `floor')=(4 or 1))")) 2))
         (is (= (count (tc/rows-query "building#(@(count `floor')=(4 or 1 or 0))")) 3))
         (is (= (count (tc/rows-query "building#(@(count `floor')=(4 or 0))")) 2)))


(defn get-name
  "Function for testing support calling function
  with parameter as self object.
  Returns name of specified SonElement."
  [^SonElement se]
  (.getName se))


(defn to-upper
  "Function for testing support calling function
  with parameter as property of self object.
  Converts supplied string to upper case."
  [^String s]
  (.toUpperCase s))


(deftest t-pself-param
         ^{:doc "Testing self object as param of function."}
         (let [f-c #(= (nth (nth (tc/rows-query %1) 0) %2) %3)]
           (is (f-c "building[@(get-name &)]#(name=\"b1\")" 0 "b1"))
           (is (f-c "building[name @(get-name &)]#(name=\"b1\")" 0 "b1"))
           (is (f-c "building[name @(get-name &)]#(name=\"b1\")" 1 "b1"))
           (is (f-c "building[@(get-name &) name]#(name=\"b2\")" 0 "b2"))
           (is (f-c "building[@(get-name &) name]#(name=\"b2\")" 1 "b2"))
           (is (f-c "building[@(get-name &)]#(name=\"b2\")" 0 "b2"))
           (is (f-c "building[@(to-upper &.name)]#(name=\"b2\")" 0 "B2"))
           (is (f-c "building[@(to-upper &.address)]#(name=\"b2\")" 0 "STREET2"))
           (is (f-c "building[@(to-upper &.name) @(to-upper &.address)]#(name=\"b2\")" 0 "B2"))
           (is (f-c "building[@(to-upper &.name) @(to-upper &.address)]#(name=\"b2\")" 1 "STREET2"))
           (is (f-c "building[@(to-upper &.address) @(to-upper &.name)]#(name=\"b2\")" 0 "STREET2"))
           (is (f-c "building[@(to-upper &.address) @(to-upper &.name)]#(name=\"b2\")" 1 "B2"))
           (is (f-c "building[@(to-upper &.address) @(get-name &)]#(name=\"b2\")" 0 "STREET2"))
           (is (f-c "building[@(to-upper &.address) @(get-name &)]#(name=\"b2\")" 1 "b2"))))

