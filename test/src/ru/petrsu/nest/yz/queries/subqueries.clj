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

(def r_101 (doto (Room.) (.setDescription "f1_b1") (.setNumber "101")))
(def r_102 (doto (Room.) (.setNumber "102")))
(def f1_b1 (doto (Floor.) 
             (.setName "f1_b1")
             (.setDescription "SM")
             (.setNumber (Integer. 1))
             (.addRoom r_101) (.addRoom r_102)))

(def f2_b1 (doto (Floor.) 
             (.setName "f2_b1")
             (.setNumber (Integer. 2))
             (.addRoom (doto (Room.) (.setNumber "201"))) 
             (.addRoom (doto (Room.) (.setNumber "202")))))

(def r_1001 (doto (Room.) (.setDescription "f1_b2") (.setNumber "1001")))
(def r_1002 (doto (Room.) (.setDescription "f1_b2") (.setNumber "1002")))
(def f1_b2 (doto (Floor.) 
             (.setName "f1_b2")
             (.setNumber (Integer. 1))
             (.addRoom r_1001) 
             (.addRoom r_1002)))

(def b1 (doto (Building.) 
          (.setName "building") (.setDescription "101")
          (.addFloor f1_b1) (.addFloor f2_b1)))
(def b2 (doto (Building.) 
          (.setName "SM") (.setDescription "102")
          (.addFloor f1_b2)))

(def son (doto (SON.)
           (.setName "SM")
           (.addBuilding b1) 
           (.addBuilding b2)))


;; Define entity manager.

(use-fixtures :once (tc/setup-son son))


;; Define tests

(deftest fdesc-equal-bname
         (let [f #(flatten (tc/rows-query %1))]
           (is (= (f "floor#(description = Ŷbuilding[name])") [f1_b1]))
           (is (= (f "floor#(description = Ŷbuilding.name)") [f1_b1]))
           (is (= (f "floor#(building = Ŷbuilding#(name=\"SM\"))") [f1_b2]))
           (is (= (f "floor#(description = Ŷbuilding)") []))
           (is (tc/eq-colls (f "floor#(room = Ŷroom#(number~\"^2.*\"))") [f2_b1]))
           (is (= (f "floor#(description=Ŷbuilding[name])") [f1_b1]))
           (is (= (f "floor#(description=Ŷbuilding.name)") [f1_b1]))))


(deftest cycling
         ^{:doc "Tests cycling in queries something like this: 
                room#(floor = Ŷfloor#(name=\"SN(\"))"}
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
           (is (tc/eq-colls (f "room#(number = Ŷbuilding.description)") [r_101 r_102]))
           (is (tc/eq-colls (f "floor (room#(number = building.description))") [f1_b1 f2_b1 f1_b2 r_101]))
           (is (= (f "room#(number = building.description)") [r_101]))
           (is (= (f "building#(description = room.number)") [b1]))
           (is (= (f "building#(description != ∀room.number)") [b2]))
           (is (= (f "building#(description!=∀room.number)") [b2]))))


(deftest typed-all-modificator
         (let [f #(flatten (tc/rows-query %1))]
           (is (= (f "floor#(description = A:building[name])") [f1_b1]))
           (is (= (f "floor#(description = A:building.name)") [f1_b1]))
           (is (= (f "floor#(building = A:building#(name=\"SM\"))") [f1_b2]))
           (is (= (f "floor#(description = A:building)") []))
           (is (tc/eq-colls (f "floor#(room = A:room#(number~\"^2.*\"))") [f2_b1]))))


(deftest subquery-and-complexconds
         (let [f #(flatten (tc/rows-query %1))]
           ; Use || or &&
           (is (= (f "building#(description = room.number || name=\"SomeName\")") [b1]))
           (is (= (f "building#(description = room.number && name=\"SomeName\")") []))
           (is (= (f "building#(description = (room.number || \"SomeDesc\"))") [b1]))
           (is (= (f "building#(description = (room.number && \"SomeDesc\"))") []))
           (is (tc/eq-colls (f "building#(description = (room.number || \"102\"))") [b1 b2]))
           (is (tc/eq-colls (f "building#(description != (\"102\" && ∀room.number))") []))
           (is (tc/eq-colls (f "building#(description != (\"102\" && =room.number))") [b1]))
           
           ; Use "and" or "or"
           (is (= (f "building#(description = room.number or name=\"SomeName\")") [b1]))
           (is (= (f "building#(description = room.number and name=\"SomeName\")") []))
           (is (= (f "building#(description = (room.number or \"SomeDesc\"))") [b1]))
           (is (= (f "building#(description = (room.number and \"SomeDesc\"))") []))
           (is (tc/eq-colls (f "building#(description = (room.number or \"102\"))") [b1 b2]))
           (is (tc/eq-colls (f "building#(description != (\"102\" and ∀room.number))") []))
           (is (tc/eq-colls (f "building#(description != (\"102\" and =room.number))") [b1]))))


(deftest not-any-modificator
         (let [f #(flatten (tc/rows-query %1))]
           (is (tc/eq-colls (f "floor#(name = room[description])") [f1_b1 f1_b2]))
           (is (= (f "floor#(name = ∀room[description])") [f1_b2]))
           (is (= (f "room#(number = ∀building.description)") [r_101]))

           (is (= (f "building#(description = ∀room.number)") []))
           (is (= (f "building#(description = ∀room.number || name=\"SomeName\")") []))
           (is (= (f "building#(description = ∀room.number || name=\"SM\")") [b2]))
           (is (= (f "building#(description = ∀room.number && name=\"SM\")") []))
           (is (= (f "building#(description = ∀room.number || description=\"SomeDesc\")") []))
           (is (= (f "building#(description = ∀room.number || description=\"102\")") [b2]))
           (is (= (f "building#(description = ∀room.number && description=\"102\")") []))
           (is (= (f "building#(description = (∀room.number || \"SomeDesc\"))") []))
           (is (= (f "building#(description = (∀room.number || \"102\"))") [b2]))
           (is (= (f "building#(description = (∀room.number && \"102\"))") []))
           (is (= (f "building#(description = (\"SomeDesc\" || ∀room.number))") []))
           (is (= (f "building#(description = (\"102\" || ∀room.number))") [b2]))
           (is (= (f "building#(description = (\"102\" && ∀room.number))") []))

           (is (= (f "floor#(name != ∀room[description])") [f2_b1]))
           (is (tc/eq-colls (f "floor#(name != room[description])") [f1_b1 f2_b1]))
           (is (= (f "floor#(name = (\"SomeName\" || != ∀room[description]))") [f2_b1]))
           (is (= (f "floor#(name=(\"SomeName\" || !=∀room[description]))") [f2_b1]))
           (is (= (f "floor#(name=(\"SomeName\" && !=∀room[description]))") []))
           (is (tc/eq-colls (f "floor#(name=(\"f1_b2\" || !=∀room[description]))") [f2_b1 f1_b2]))
           (is (= (f "floor#(name!=(\"SomeName\" && ∀room[description]))") [f2_b1]))
           (is (tc/eq-colls (f "floor#(name!=(\"f1_b2\" || ∀room[description]))") [f2_b1 f1_b1]))
           
           (is (tc/eq-colls (f "floor#(name!=(\"f1_b2\" || \"f1_b1\" || ∀room[description]))") 
                            [f2_b1 f1_b1 f1_b2]))
           (is (tc/eq-colls (f "floor#(name!=(\"f1_b2\" && \"f1_b1\" || ∀room[description]))") 
                            [f2_b1]))
           (is (tc/eq-colls (f "floor#(name!=(\"f1_b2\" && (\"f1_b1\" || ∀room[description])))") 
                            [f2_b1]))
           (is (tc/eq-colls (f "floor#(name=(\"f1_b2\" && \"f1_b1\" || ∀room[description]))") 
                            [f1_b2]))
           (is (tc/eq-colls (f "floor#(name=(\"f1_b2\" && (\"f1_b1\" || ∀room[description])))") 
                            [f1_b2]))
           (is (tc/eq-colls (f "floor#(name!=(\"f1_b2\" or \"f1_b1\" or ∀room[description]))") 
                            [f2_b1 f1_b1 f1_b2]))
           (is (tc/eq-colls (f "floor#(name!=(\"f1_b2\" and \"f1_b1\" or ∀room[description]))") 
                            [f2_b1]))
           (is (tc/eq-colls (f "floor#(name!=(\"f1_b2\" and (\"f1_b1\" or ∀room[description])))") 
                            [f2_b1]))
           (is (tc/eq-colls (f "floor#(name=(\"f1_b2\" and \"f1_b1\" or ∀room[description]))") 
                            [f1_b2]))
           (is (tc/eq-colls (f "floor#(name=(\"f1_b2\" and (\"f1_b1\" or ∀room[description])))") 
                            [f1_b2]))))


(deftest not-any-and-all
         (let [f #(flatten (tc/rows-query %1))]
           (is (= (f "building#(name = Ŷ∀son[name])") [b2]))
           (is (= (f "building#(name = ∀Ŷson[name])") [b2]))
           (is (= (f "building#(name = Ŷson[name])") [b2]))
           (is (= (f "building#(name = Ŷson[name])") [b2]))
           (is (= (f "building#(name=Ŷ∀son[name])") [b2]))
           (is (= (f "building#(name=∀Ŷson[name])") [b2]))
           (is (= (f "building#(name=Ŷson[name])") [b2]))
           (is (= (f "building#(name=Ŷson[name])") [b2]))
           
           (is (= (f "son#(name = Ŷ∀building[name])") []))
           (is (= (f "son#(name = ∀Ŷbuilding[name])") []))
           (is (= (f "son#(name = Ŷbuilding[name])") [son]))
           (is (= (f "son#(name = Ŷbuilding[name])") [son]))
           (is (= (f "son#(name=Ŷ∀building[name])") []))
           (is (= (f "son#(name=∀Ŷbuilding[name])") []))
           (is (= (f "son#(name=Ŷbuilding[name])") [son]))
           (is (= (f "son#(name=Ŷbuilding[name])") [son]))))


(deftest typed-not-any-and-all
         (let [f #(flatten (tc/rows-query %1))]
           (is (= (f "building#(name = A:all:son[name])") [b2]))
           (is (= (f "building#(name = all:A:son[name])") [b2]))
           (is (= (f "building#(name=A:all:son[name])") [b2]))
           (is (= (f "building#(name=all:A:son[name])") [b2]))
           
           (is (= (f "son#(name = A:all:building[name])") []))
           (is (= (f "son#(name = all:A:building[name])") []))
           (is (= (f "son#(name=A:all:building[name])") []))
           (is (= (f "son#(name=all:A:building[name])") []))))


(deftest mixed-not-any-and-all
         (let [f #(flatten (tc/rows-query %1))]
           (is (= (f "building#(name = A:∀son[name])") [b2]))
           (is (= (f "building#(name = ∀A:son[name])") [b2]))
           (is (= (f "building#(name=A:∀son[name])") [b2]))
           (is (= (f "building#(name=∀A:son[name])") [b2]))
           (is (= (f "building#(name = Ŷall:son[name])") [b2]))
           (is (= (f "building#(name = all:Ŷson[name])") [b2]))
           (is (= (f "building#(name=Ŷall:son[name])") [b2]))
           (is (= (f "building#(name=all:Ŷson[name])") [b2]))
           
           (is (= (f "son#(name = A:∀building[name])") []))
           (is (= (f "son#(name = ∀A:building[name])") []))
           (is (= (f "son#(name=A:∀building[name])") []))
           (is (= (f "son#(name=∀A:building[name])") []))
           (is (= (f "son#(name = Ŷall:building[name])") []))
           (is (= (f "son#(name = all:Ŷbuilding[name])") []))
           (is (= (f "son#(name=Ŷall:building[name])") []))
           (is (= (f "son#(name=all:Ŷbuilding[name])") []))))
