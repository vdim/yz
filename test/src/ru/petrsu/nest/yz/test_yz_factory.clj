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
            [ru.petrsu.nest.yz.hb-utils :as hu]
            [ru.petrsu.nest.yz.queries.bd :as bd])
  (:import (ru.petrsu.nest.son SON Building Room Floor))
  (:use clojure.test))


(def mom- 
  ^{:doc "Defines the map of the object model (used Nest's model)"}
  (hu/mom-from-file "nest.mom"))


(deftest find-rc
         ^{:doc "Tests the find-related-colls function."}
         (let [floors [bd/f1_b1 bd/f2_b1 bd/f3_b1]]
           (is (qc/eq-maps (yzf/find-related-colls floors Floor [Floor Room] mom-) 
                           {Floor floors, Room [bd/r101_f1_b1 bd/r102_f1_b1 bd/r201_f2_b1 bd/r202_f2_b1]}))
           (is (qc/eq-maps (yzf/find-related-colls floors Floor [Floor Room Building] mom-) 
                           {Floor floors, 
                            Room [bd/r101_f1_b1 bd/r102_f1_b1 bd/r201_f2_b1 bd/r202_f2_b1]
                            Building [bd/b1]})))
         (let [floors [bd/f1_b1 bd/f1_b2]]
           (is (qc/eq-maps (yzf/find-related-colls floors Floor [Floor Room] mom-) 
                           {Floor floors, Room [bd/r101_f1_b1 bd/r102_f1_b1 bd/r1001_f1_b2 bd/r1002_f1_b2]}))
           (is (qc/eq-maps (yzf/find-related-colls floors Floor [Floor Room Building] mom-) 
                           {Floor floors, 
                            Room [bd/r101_f1_b1 bd/r102_f1_b1 bd/r1001_f1_b2 bd/r1002_f1_b2]
                            Building [bd/b1 bd/b2]}))))


(deftest collq-f
         ^{:doc "Tests the collq function"}
         (let [f #(qc/eq-colls (apply yzf/collq "long" [1 2 3] %1) %2)]
           (is (f () [[1] [2] [3]]))
           (is (f [:rtype :rows] [[1] [2] [3]]))
           (is (f [:rtype :result] [1 [] 2 [] 3 []]))
           (is (f [:rtype :rows :clazz Long] [[1] [2] [3]]))
           (is (f [:rtype :result :clazz Long] [1 [] 2 [] 3 []]))
           (is (f [:clazz Long :rtype :rows] [[1] [2] [3]]))
           (is (f [:clazz Long :rtype :result] [1 [] 2 [] 3 []])))
           
         (is (thrown? Exception (yzf/collq "long" [1] :clazz String)))
         (is (thrown? Exception (yzf/collq "long" ["af"] :clazz String)))
         (is (thrown? Exception (yzf/collq "string" [1] :clazz Long)))
         (is (thrown? Exception (yzf/collq "string" ["af"] :clazz Long)))
         
         (is (= (yzf/collq "long" ["af"] :rtype :result) []))
         (is (= (yzf/collq "string" [1 2 3] :rtype :result) []))
         (is (= (yzf/collq "long" ["af"]) ()))
         (is (= (yzf/collq "string" [1 2 3]) ()))
         (is (= (yzf/collq "long" ["af"] :rtype :rows) ()))
         (is (= (yzf/collq "string" [1 2 3] :rtype :rows) ()))

         (is (= (yzf/collq "long" ["af" 1 "sd"]) [[1]]))
         (is (= (yzf/collq "string" [1 2 "af" 3]) [["af"]]))
         (is (= (yzf/collq "long" ["af" 1 "sd"] :clazz Long) [[1]]))
         (is (= (yzf/collq "string" [1 2 "af" 3] :clazz String) [["af"]]))
         (is (= (yzf/collq "long" ["af" 1 "sd"] :rtype :rows :clazz Long) [[1]]))
         (is (= (yzf/collq "string" [1 2 "af" 3] :rtype :rows :clazz String) [["af"]]))
         (is (= (yzf/collq "long" ["af" 1 "sd"] :rtype :result :clazz Long) [1 []]))
         (is (= (yzf/collq "string" [1 2 "af" 3] :rtype :result :clazz String) ["af" []])))
