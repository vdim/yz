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
            [ru.petrsu.nest.yz.queries.core :as qc]
            [ru.petrsu.nest.yz.queries.bd :as bd])
  (:import (ru.petrsu.nest.son SON Building Room Floor))
  (:use clojure.test))


(deftest find-rc
         ^{:doc "Tests the find-related-colls function."}
         (let [floors [bd/f1_b1 bd/f2_b1 bd/f3_b1]]
           (is (qc/eq-maps (yzf/find-related-colls floors Floor [Floor Room]) 
                           {Floor floors, Room [bd/r101_f1_b1 bd/r102_f1_b1 bd/r201_f2_b1 bd/r202_f2_b1]}))
           (is (qc/eq-maps (yzf/find-related-colls floors Floor [Floor Room Building]) 
                           {Floor floors, 
                            Room [bd/r101_f1_b1 bd/r102_f1_b1 bd/r201_f2_b1 bd/r202_f2_b1]
                            Building [bd/b1]})))
         (let [floors [bd/f1_b1 bd/f1_b2]]
           (is (qc/eq-maps (yzf/find-related-colls floors Floor [Floor Room]) 
                           {Floor floors, Room [bd/r101_f1_b1 bd/r102_f1_b1 bd/r1001_f1_b2 bd/r1002_f1_b2]}))
           (is (qc/eq-maps (yzf/find-related-colls floors Floor [Floor Room Building]) 
                           {Floor floors, 
                            Room [bd/r101_f1_b1 bd/r102_f1_b1 bd/r1001_f1_b2 bd/r1002_f1_b2]
                            Building [bd/b1 bd/b2]}))))

