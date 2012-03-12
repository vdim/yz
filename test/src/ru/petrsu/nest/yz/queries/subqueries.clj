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
             SON, Building, Room, Floor, Network)))

(def f1_b1 (doto (Floor.) 
             (.setDescription "SM")
             (.setNumber (Integer. 1))
             (.addRoom (doto (Room.) (.setNumber "101"))) 
             (.addRoom (doto (Room.) (.setNumber "102")))))

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

(def b1 (doto (Building.) (.setName "building") (.addFloor f1_b1) (.addFloor f2_b1)))
(def b2 (doto (Building.) (.setName "SM") (.addFloor f1_b2)))

(def son (doto (SON.)
           (.addBuilding b1) 
           (.addBuilding b2)))


;; Define entity manager.

(use-fixtures :once (tc/setup-son son))


;; Define tests

(deftest fdesc-equal-bname
         (let [f #(flatten (tc/rows-query %1))]
           (is (= (f "floor#(description = building[name])") [f1_b1]))
           (is (= (f "floor#(description = building.name)") [f1_b1]))
           (is (= (f "floor#(building = building#(name=\"SM\"))") [f1_b2]))
           (is (= (f "floor#(description = building)") []))
           (is (tc/eq-colls (f "floor#(room = room#(number~\"^2.*\"))") [f2_b1]))))
