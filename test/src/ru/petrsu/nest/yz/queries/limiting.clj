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

(ns ru.petrsu.nest.yz.queries.limiting
  ^{:author "Vyacheslav Dimitrov"
    :doc "Tests queries with limiting option."}
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
             (.setNumber (Integer. 0))
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

(deftest long-limiting
         (let [ls [1 2 3 1 5 6]]
           (is (tc/eq-colls (yzf/collq "1:long" ls) [[2] [3] [1] [5] [6]]))
           (is (tc/eq-colls (yzf/collq "1-2:long" ls) [[2] [3]]))
           (is (tc/eq-colls (yzf/collq "1-1:long" ls) [[2]]))
           (is (tc/eq-colls (yzf/collq "0-1:long" ls) [[1] [2]]))
           (is (tc/eq-colls (yzf/collq "0-2:long" ls) [[1] [2] [3]]))
           (is (tc/eq-colls (yzf/collq "-1:long" ls) [[1] [2] [3] [1] [5]]))
           (is (tc/eq-colls (yzf/collq "-0-1:long" ls) [[5] [6]]))
           (is (tc/eq-colls (yzf/collq "-1-1:long" ls) [[5]]))))

(deftest building-limiting
         (let [mom (assoc tc/*mom* Building
                                   (assoc (get tc/*mom* Building) 
                                          :sort {:self {:comp #(* -1 (compare %1 %2))
                                                        :keyfn #(.getName %1)}}))
               mom (assoc mom Floor
                                   (assoc (get mom Floor) 
                                          :sort {:self {:comp #(* -1 (compare %1 %2))
                                                        :keyfn #(.getNumber %1)}}))]
           (binding [tc/*mom* mom]
             (let [f #(flatten (tc/rows-query %1))]
               (is (tc/eq-colls (f "0-1:building") [b1 b2]))
               (is (= (f "0-1:d:building") [b1 b2]))
               (is (= (f "0-1:a:building") [b2 b1]))
               (is (= (f "0-0:d:building") [b1]))
               (is (= (f "0-0:a:building") [b2]))
               (is (= (f "1-1:d:building") [b2]))
               (is (= (f "1-1:a:building") [b1]))
               (is (= (f "1:d:building") [b2]))
               (is (= (f "1:a:building") [b1]))
               (is (= (f "-1:d:building") [b1]))
               (is (= (f "-1:a:building") [b2]))
               (is (= (f "-0-0:d:building") [b2]))
               (is (= (f "-0-0:a:building") [b1]))
               (is (= (f "-0-1:d:building") [b1 b2]))
               (is (= (f "-0-1:a:building") [b2 b1]))
               (is (= (f "-0-10:d:building") [b1 b2]))
               (is (= (f "-0-10:a:building") [b2 b1]))
             
               (is (tc/eq-colls (f "building (floor)") [b2 f1_b2 b1 f1_b1 b1 f2_b1]))
               (is (tc/eq-colls (f "-1:a:building (floor)") [b2 f1_b2]))
               (is (tc/eq-colls (f "-1:a:building (1:floor)") [b2]))
               (is (tc/eq-colls (f "-1:a:building (0-0:floor)") [b2 f1_b2]))
               (is (tc/eq-colls (f "-1:a:building (-0-0:floor)") [b2 f1_b2]))
               (is (tc/eq-colls (f "-1:a:building (-0-10:floor)") [b2 f1_b2]))
             
               (is (tc/eq-colls (f "2:a:building (floor)") []))
               (is (tc/eq-colls (f "2:a:building (-1:floor)") []))
               (is (tc/eq-colls (f "2:a:building (0:floor)") []))
               (is (tc/eq-colls (f "2:a:building (0-5:floor)") []))
             
               (is (tc/eq-colls (f "building (0-0:d:floor)") [b2 f1_b2 b1 f1_b1]))
               (is (tc/eq-colls (f "building (1:d:floor)") [b2 b1 f2_b1]))
               (is (tc/eq-colls (f "building (0-1:floor)") [b2 f1_b2 b1 f1_b1 b1 f2_b1]))
               (is (tc/eq-colls (f "0-1:building (0-1:floor)") [b2 f1_b2 b1 f1_b1 b1 f2_b1]))
             
               (is (tc/eq-colls (f "building.floor") [f1_b2 f1_b1 f2_b1]))
               (is (tc/eq-colls (f "building.0:floor") [f1_b2 f1_b1 f2_b1]))
               (is (tc/eq-colls (f "building.0-1:d:floor") [f1_b2 f1_b1]))
               (is (tc/eq-colls (f "building.2:d:floor") [f2_b1]))
               (is (tc/eq-colls (f "building.-0-0:d:floor") [f2_b1]))
               (is (tc/eq-colls (f "0-0:a:building.0-0:d:floor") [f1_b2]))
               (is (tc/eq-colls (f "0-0:a:building.1-10:d:floor") []))
               (is (tc/eq-colls (f "0-0:a:building.1:d:floor") []))
               (is (tc/eq-colls (f "0-0:d:building.0-0:d:floor") [f1_b1]))
               (is (tc/eq-colls (f "0-0:d:building.1-10:d:floor") [f2_b1]))
               (is (tc/eq-colls (f "0-0:d:building.1:d:floor") [f2_b1]))
               
               (is (tc/eq-colls (f "-0-0:a:building.floor") [f1_b1 f2_b1]))
               (is (tc/eq-colls (f "-0-0:a:building.1:a:floor") [f1_b1]))
               (is (tc/eq-colls (f "-0-0:a:building.1:d:floor") [f2_b1]))
               (is (tc/eq-colls (f "-0-0:a:building.-1:a:floor") [f2_b1]))
               (is (tc/eq-colls (f "-0-0:a:building.-1:d:floor") [f1_b1]))
               (is (tc/eq-colls (f "-0-0:a:building.0-0:d:floor") [f1_b1]))
               (is (tc/eq-colls (f "-0-0:a:building.0-0:a:floor") [f2_b1]))
               (is (tc/eq-colls (f "-0-0:a:building.0-1:d:floor") [f1_b1 f2_b1]))
               (is (tc/eq-colls (f "-0-0:a:building.0-1:a:floor") [f2_b1 f1_b1]))
               (is (tc/eq-colls (f "-0-0:a:building.-0-0:d:floor") [f2_b1]))
               (is (tc/eq-colls (f "-0-0:a:building.-0-0:a:floor") [f1_b1]))))))

