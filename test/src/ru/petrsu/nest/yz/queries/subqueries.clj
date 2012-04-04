;;
;; Copyright 2012 Vyacheslav Dimitrov <vyacheslav.dimitrov@gmail.com>
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

(ns ru.petrsu.nest.yz.queries.subqueries
  ^{:author "Vyacheslav Dimitrov"
    :doc "Test queries with subquery in the right part of
         condition. Something like this: floor#(name = room.name)"}
  (:use ru.petrsu.nest.yz.core 
        clojure.test)
  (:require [ru.petrsu.nest.yz.queries.core :as tc])
  (:import (ru.petrsu.nest.son 
             SON, Building, Room, Floor)))

(def r_101 (doto (Room.) (.setNumber "101")))
(def r_102 (doto (Room.) (.setNumber "102")))
(def f1_b1 (doto (Floor.) 
             (.setDescription "SM")
             (.setNumber (Integer. 1))
             (.addRoom r_101) (.addRoom r_102)))

(def f2_b1 (doto (Floor.) 
             (.setName "f2_b1")
             (.setNumber (Integer. 2))
             (.addRoom (doto (Room.) (.setNumber "201"))) 
             (.addRoom (doto (Room.) (.setNumber "202")))))

(def f1_b2 (doto (Floor.) 
             (.setName "f1_b2")
             (.setNumber (Integer. 1))
             (.addRoom (doto (Room.) (.setNumber "1001"))) 
             (.addRoom (doto (Room.) (.setNumber "1002")))))

(def b1 (doto (Building.) 
          (.setName "building") (.setDescription "101")
          (.addFloor f1_b1) (.addFloor f2_b1)))
(def b2 (doto (Building.) 
          (.setName "SM") (.setDescription "102")
          (.addFloor f1_b2)))

(def son (doto (SON.)
           (.addBuilding b1) 
           (.addBuilding b2)))


;; Define entity manager.

(use-fixtures :once (tc/setup-son son))


;; Define tests

(deftest fdesc-equal-bname
         (let [f #(flatten (tc/rows-query %1))]
           (is (= (f "floor#(description = ∀building[name])") [f1_b1]))
           (is (= (f "floor#(description = ∀building.name)") [f1_b1]))
           (is (= (f "floor#(building = ∀building#(name=\"SM\"))") [f1_b2]))
           (is (= (f "floor#(description = ∀building)") []))
           (is (tc/eq-colls (f "floor#(room = ∀room#(number~\"^2.*\"))") [f2_b1]))))


(deftest cycling
         ^{:doc "Tests cycling in queries something like this: 
                room#(floor = ∀floor#(name=\"SN(\"))"}
         (let [f #(flatten (tc/rows-query (str "room#(floor = floor#(name=\"" %1 "\"))")))]
           (is (= (f "(") []))
           (is (= (f "()") []))
           (is (= (f ")") []))
           (is (= (f "SN(") []))
           (is (= (f "SN()") []))
           (is (= (f "SN)") []))
           (is (= (f "SN(sadf)") []))
           (is (= (f "SN(s(h())d))adf)") []))))


(deftest independence-subquery
         ^{:doc "Tests subqueries which doesn't depend on main query."}
         (let [f #(flatten (tc/rows-query %1))]
           (is (= (f "floor#(description = building[name])") []))
           (is (tc/eq-colls (f "room#(number = ∀building.description)") [r_101 r_102]))
           (is (tc/eq-colls (f "floor (room#(number = building.description))") [f1_b1 f2_b1 f1_b2 r_101]))
           (is (= (f "room#(number = building.description)") [r_101]))
           (is (= (f "building#(description = room.number)") [b1]))
           (is (= (f "building#(description != room.number)") [b2]))))


(deftest typed-any-modificator
         (let [f #(flatten (tc/rows-query %1))]
           (is (= (f "floor#(description = a:building[name])") [f1_b1]))
           (is (= (f "floor#(description = a:building.name)") [f1_b1]))
           (is (= (f "floor#(building = a:building#(name=\"SM\"))") [f1_b2]))
           (is (= (f "floor#(description = a:building)") []))
           (is (tc/eq-colls (f "floor#(room = a:room#(number~\"^2.*\"))") [f2_b1]))))


(deftest subquery-and-complexconds
         (let [f #(flatten (tc/rows-query %1))]
           (is (= (f "building#(description = room.number || name=\"SomeName\")") [b1]))
           (is (= (f "building#(description = room.number && name=\"SomeName\")") []))
           (is (= (f "building#(description = (room.number || \"SomeDesc\"))") [b1]))
           (is (= (f "building#(description = (room.number && \"SomeDesc\"))") []))
           (is (tc/eq-colls (f "building#(description = (room.number || \"102\"))") [b1 b2]))
           (is (tc/eq-colls (f "building#(description != (\"102\" && room.number))") []))
           (is (tc/eq-colls (f "building#(description != (\"102\" && =room.number))") [b1]))))
