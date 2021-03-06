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
            [ru.petrsu.nest.yz.mom-utils :as mu]
            [ru.petrsu.nest.yz.queries.bd :as bd])
  (:import (ru.petrsu.nest.son SON Building Room Floor) 
           (ru.petrsu.nest.yz NotFoundElementException))
  (:use clojure.test))


(def mom- 
  ^{:doc "Defines the map of the object model (used Nest's model)"}
  (mu/mom-from-file "nest.mom"))


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
           (is (f [:rtype :rows] [[1] [2] [3]]))
           (is (f [:rtype :result] [1 [] 2 [] 3 []]))
           (is (f [:rtype :rows :clazz Long] [[1] [2] [3]]))
           (is (f [:rtype :result :clazz Long] [1 [] 2 [] 3 []]))
           (is (f [:clazz Long :rtype :rows] [[1] [2] [3]]))
           (is (f [:clazz Long :rtype :result] [1 [] 2 [] 3 []])))
           
         (is (thrown? NotFoundElementException (yzf/collq "long" [1] :clazz String)))
         (is (thrown? NotFoundElementException (yzf/collq "long" ["af"] :clazz String)))
         (is (thrown? NotFoundElementException (yzf/collq "string" [1] :clazz Long)))
         (is (thrown? NotFoundElementException (yzf/collq "string" ["af"] :clazz Long)))
         (is (thrown? NotFoundElementException (yzf/collq "string" ["af"] :clazz Long)))
         
         (is (= (yzf/collq "long" ["af"] :rtype :result) []))
         (is (= (yzf/collq "string" [1 2 3] :rtype :result) []))
         (is (= (yzf/collq "long" ["af"] :rtype :rows) ()))
         (is (= (yzf/collq "string" [1 2 3] :rtype :rows) ()))

         (is (= (yzf/collq "long" ["af" 1 "sd"] :rtype :rows) [[1]]))
         (is (= (yzf/collq "string" [1 2 "af" 3] :rtype :rows) [["af"]]))
         (is (= (yzf/collq "long" ["af" 1 "sd"] :clazz Long :rtype :rows) [[1]]))
         (is (= (yzf/collq "string" [1 2 "af" 3] :clazz String :rtype :rows) [["af"]]))
         (is (= (yzf/collq "long" ["af" 1 "sd"] :rtype :rows :clazz Long) [[1]]))
         (is (= (yzf/collq "string" [1 2 "af" 3] :rtype :rows :clazz String) [["af"]]))
         (is (= (yzf/collq "long" ["af" 1 "sd"] :rtype :result :clazz Long) [1 []]))
         (is (= (yzf/collq "string" [1 2 "af" 3] :rtype :result :clazz String) ["af" []]))

         (is (= (yzf/collq "string" [1 2 "af" 3] :clazz [String] :rtype :rows) [["af"]]))
         (is (qc/eq-colls (yzf/collq "long" [1 2 "af" 3] :clazz [String Long] :rtype :rows) [[1] [2] [3]]))

         (is (= (yzf/collq "building" [1 2 bd/b1] :clazz [Building] :rtype :rows) [[bd/b1]]))
         (is (qc/eq-colls (yzf/collq "building" [bd/b2 1 2 bd/b1] :clazz [Building] :rtype :rows) [[bd/b1] [bd/b2]]))
         (is (= (yzf/collq "building (floor)" [bd/b2 1 2] :clazz [Building Floor] :rtype :rows) [[bd/b2]]))
         (is (= (yzf/collq "building (floor)" [bd/b2 1 2] :rtype :result :clazz [Building Floor]) 
                [bd/b2 []]))
         (is (= (yzf/collq "building (floor)" [bd/b2 1 2] 
                           :rtype :result 
                           :clazz [Building Floor] 
                           :verbose true) 
                [bd/b2 [bd/f1_b2 []]]))
         (is (= (yzf/collq "building (floor)" [bd/b2 1 2] 
                           :rtype :result 
                           :clazz [Building Floor] 
                           :verbose false) 
                [bd/b2 []]))
         (is (= (yzf/collq "building (floor)" [bd/b2 1 2 bd/f1_b2] :rtype :result :clazz [Building Floor]) 
                [bd/b2 [bd/f1_b2 []]]))
         (is (= (yzf/collq "building (floor)" [bd/b2 1 2 bd/f1_b2] :rtype :result :mom mom-) 
                [bd/b2 [bd/f1_b2 []]]))
         (is (= (yzf/collq "building (floor)" [bd/b2 1 2 bd/f1_b2] 
                           :rtype :result :mom mom- :clazz [Building Floor]) 
                [bd/b2 [bd/f1_b2 []]]))
         )
