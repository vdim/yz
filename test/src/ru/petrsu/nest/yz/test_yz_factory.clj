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

(ns ru.petrsu.nest.yz.test-yz-factory
  ^{:author "Vyacheslav Dimitrov"
    :doc "Tests for core functions."}
  (:require [ru.petrsu.nest.yz.yz-factory :as yzf]
            [ru.petrsu.nest.yz.core :as c] 
            [ru.petrsu.nest.yz.queries.bd :as bd])
  (:import (ru.petrsu.nest.son SON Building Room Floor))
  (:use clojure.test))


(defn eq-colls
  "Equals two collections."
  [coll1 coll2]
  (let [s-coll1 (set coll1)
        s-coll2 (set coll2)]
    (and
      (empty? (remove #(contains? s-coll2 %) coll1))
      (empty? (remove #(contains? s-coll1 %) coll2)))))


(defn eq-maps
  "Equals two maps where value is collection 
  (collections are equaled due to eq-colls)."
  [map1 map2]
  (let [check-map #(reduce (fn [r [k v]] (and r (eq-colls v (get %1 k)))) true %2)]
    (and
      (check-map map1 map2)
      (check-map map2 map1))))


(deftest find-rc
         ^{:doc "Tests the find-related-colls function."}
         (let [floors [bd/f1_b1 bd/f2_b1 bd/f3_b1]]
           (is (eq-maps (yzf/find-related-colls floors Floor [Floor Room]) 
                        {Floor floors, Room [bd/r101_f1_b1 bd/r102_f1_b1 bd/r201_f2_b1 bd/r202_f2_b1]}))
           (is (eq-maps (yzf/find-related-colls floors Floor [Floor Room Building]) 
                        {Floor floors, 
                         Room [bd/r101_f1_b1 bd/r102_f1_b1 bd/r201_f2_b1 bd/r202_f2_b1]
                         Building [bd/b1]})))
         (let [floors [bd/f1_b1 bd/f1_b2]]
           (is (eq-maps (yzf/find-related-colls floors Floor [Floor Room]) 
                        {Floor floors, Room [bd/r101_f1_b1 bd/r102_f1_b1 bd/r1001_f1_b2 bd/r1002_f1_b2]}))
           (is (eq-maps (yzf/find-related-colls floors Floor [Floor Room Building]) 
                        {Floor floors, 
                         Room [bd/r101_f1_b1 bd/r102_f1_b1 bd/r1001_f1_b2 bd/r1002_f1_b2]
                         Building [bd/b1 bd/b2]}))))

