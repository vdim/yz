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

(ns ru.petrsu.nest.yz.queries.unique
  ^{:author "Vyacheslav Dimitrov"
    :doc "Tests queries with a removing duplicates."}
  (:use ru.petrsu.nest.yz.core 
        clojure.test)
  (:require [ru.petrsu.nest.yz.queries.core :as tc] 
            [ru.petrsu.nest.yz.yz-factory :as yzf])
  (:import (ru.petrsu.nest.son SON Building Room Floor)))


;; Define model


(def r101_f1_b1 (doto (Room.) (.setNumber "101")))
(def r102_f1_b1 (doto (Room.) (.setNumber "102")))
(def f1_b1 (doto (Floor.) 
             (.setNumber (Integer. 1))
             (.addRoom r101_f1_b1) 
             (.addRoom r102_f1_b1)))

(def r201_f2_b1 (doto (Room.) (.setNumber "201")))
(def f2_b1 (doto (Floor.) 
             (.setNumber (Integer. 2))
             (.addRoom r201_f2_b1)))


(def r101_f1_b2 (doto (Room.) (.setNumber "101")))
(def r1001_f1_b2 (doto (Room.) (.setNumber "1001")))
(def f1_b2 (doto (Floor.) 
             (.setNumber (Integer. 1))
             (.addRoom r101_f1_b2) 
             (.addRoom r1001_f1_b2)))

(def b1 (doto (Building.) (.setName "b1") 
          (.setAddress "Street2") 
          (.setDescription "Some desc")
          (.addFloor f1_b1) (.addFloor f2_b1)))
(def b2 (doto (Building.) 
          (.setDescription "Some desc")
          (.setName "b2") 
          (.setAddress "Street1") 
          (.addFloor f1_b2)))

(def son (doto (SON.)
           (.addBuilding b1) 
           (.addBuilding b2)))



;; Define entity manager.

(use-fixtures :once (tc/setup-son son))


;; Define tests

(deftest simple-unique
         (let [ls [1 2 3 1]]
           (is (tc/eq-colls (yzf/collq "long" ls :rtype :rows) [[1] [1] [2] [3]]))
           (is (tc/eq-colls (yzf/collq "u:long" ls :rtype :rows) [[1] [2] [3]]))
           (is (tc/eq-colls (yzf/collq "¹long" ls :rtype :rows) [[1] [2] [3]]))))


(deftest floor-unique
         (let [f #(flatten (tc/rows-query %1))]
           (is (tc/eq-colls (f "room.floor") [f1_b1 f1_b1 f1_b2 f1_b2 f2_b1]))
           (is (tc/eq-colls (f "u:room.floor") [f1_b1 f1_b1 f1_b2 f1_b2 f2_b1]))
           (is (tc/eq-colls (f "room.u:floor") [f1_b1 f1_b2 f2_b1]))

           (is (tc/eq-colls (f "room.floor.building") [b1 b1 b2 b2 b1]))
           (is (tc/eq-colls (f "u:room.floor.building") [b1 b1 b2 b2 b1]))
           (is (tc/eq-colls (f "room.u:floor.building") [b1 b2 b1]))
           (is (tc/eq-colls (f "u:room.u:floor.building") [b1 b2 b1]))
           (is (tc/eq-colls (f "room.floor.u:building") [b1 b2]))
           (is (tc/eq-colls (f "room.u:floor.u:building") [b1 b2]))
           (is (tc/eq-colls (f "u:room.u:floor.u:building") [b1 b2]))))


(deftest floor-unique-not-typed
         (let [f #(flatten (tc/rows-query %1))]
           (is (tc/eq-colls (f "room.floor") [f1_b1 f1_b1 f1_b2 f1_b2 f2_b1]))
           (is (tc/eq-colls (f "¹room.floor") [f1_b1 f1_b1 f1_b2 f1_b2 f2_b1]))
           (is (tc/eq-colls (f "room.¹floor") [f1_b1 f1_b2 f2_b1]))

           (is (tc/eq-colls (f "room.floor.building") [b1 b1 b2 b2 b1]))
           (is (tc/eq-colls (f "¹room.floor.building") [b1 b1 b2 b2 b1]))
           (is (tc/eq-colls (f "room.¹floor.building") [b1 b2 b1]))
           (is (tc/eq-colls (f "¹room.¹floor.building") [b1 b2 b1]))
           (is (tc/eq-colls (f "room.floor.¹building") [b1 b2]))
           (is (tc/eq-colls (f "room.¹floor.¹building") [b1 b2]))
           (is (tc/eq-colls (f "¹room.¹floor.¹building") [b1 b2]))))
