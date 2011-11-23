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

(ns ru.petrsu.nest.yz.queries.sorting
  ^{:author "Vyacheslav Dimitrov"
    :doc "Tests queries with a sorting."}
  (:use ru.petrsu.nest.yz.core 
        clojure.test)
  (:require [ru.petrsu.nest.yz.queries.core :as tc])
  (:import (ru.petrsu.nest.son SON Building Room Floor)))


;; Define model


(def r101_f1_b1 (doto (Room.) (.setNumber "101")))
(def r102_f1_b1 (doto (Room.) (.setNumber "102")))
(def f1_b1 (doto (Floor.) 
             (.setNumber (Integer. 1))
             (.addRoom r101_f1_b1) 
             (.addRoom r102_f1_b1)))

(def r201_f2_b1 (doto (Room.) (.setNumber "201")))
(def r202_f2_b1 (doto (Room.) (.setNumber "202")))
(def f2_b1 (doto (Floor.) 
             (.setNumber (Integer. 2))
             (.addRoom r201_f2_b1) 
             (.addRoom r202_f2_b1)))

(def r301_f3_b1 (doto (Room.) (.setNumber "301")))
(def r302_f3_b1 (doto (Room.) (.setNumber "302")))
(def f3_b1 (doto (Floor.) 
             (.setNumber (Integer. 3))
             (.addRoom r301_f3_b1) 
             (.addRoom r302_f3_b1)))

(def r101_f4_b1 (doto (Room.) (.setNumber "101")))
(def r401_f4_b1 (doto (Room.) (.setNumber "401")))
(def r402_f4_b1 (doto (Room.) (.setNumber "402")))
(def f4_b1 (doto (Floor.) 
             (.setNumber (Integer. 4))
             (.addRoom r101_f4_b1) 
             (.addRoom r401_f4_b1) 
             (.addRoom r402_f4_b1)))


(def r101_f1_b2 (doto (Room.) (.setNumber "101")))
(def r1001_f1_b2 (doto (Room.) (.setNumber "1001")))
(def r1002_f1_b2 (doto (Room.) (.setNumber "1002")))
(def r201_f1_b2 (doto (Room.) (.setNumber "201")))
(def f1_b2 (doto (Floor.) 
             (.setNumber (Integer. 1))
             (.addRoom r101_f1_b2) 
             (.addRoom r1001_f1_b2) 
             (.addRoom r1002_f1_b2) 
             (.addRoom r201_f1_b2)))

(def b1 (doto (Building.) (.setName "b1") 
          (.setAddress "Street2") 
          (.setDescription "Some desc")
          (.addFloor f1_b1) (.addFloor f2_b1)
          (.addFloor f3_b1) (.addFloor f4_b1)))
(def b2 (doto (Building.) 
          (.setDescription "Some desc")
          (.setName "b2") 
          (.setAddress "Street1") 
          (.addFloor f1_b2)))

(def b3 (doto (Building.) 
          (.setDescription "Some desc2")
          (.setName "b3") 
          (.setAddress "Street3")))

(def son (doto (SON.)
           (.addBuilding b1) 
           (.addBuilding b2)
           (.addBuilding b3))) 



;; Define entity manager.

(use-fixtures :once (tc/setup-son son))


;; Define tests

(deftest tsort
         (is (= (tc/rows-query "↑building") [[b1] [b2] [b3]]))
         (is (= (tc/rows-query "↓building") [[b3] [b2] [b1]]))
         (binding [tc/*mom* (assoc tc/*mom* Building
                                   (assoc (get tc/*mom* Building) 
                                          :sort {:self {:keyfn #(.getAddress %)}}))]
           (is (= (tc/rows-query "↑building") [[b2] [b1] [b3]]))
           (is (= (tc/rows-query "↓building") [[b3] [b1] [b2]])))
         (binding [tc/*mom* (assoc tc/*mom* 
                                   Floor
                                   (assoc (get tc/*mom* Floor) 
                                          :sort {:self {:keyfn #(.getNumber %)}})
                                   Room
                                   (assoc (get tc/*mom* Room) 
                                          :sort {:self {:keyfn #(.getNumber %)}}))]
           (is (= (tc/rows-query "↑building (↑floor)") 
                  [[b1 f1_b1] [b1 f2_b1] [b1 f3_b1] [b1 f4_b1] [b2 f1_b2] [b3]]))
           (is (= (tc/rows-query "↓building (↑floor)") 
                  [[b3] [b2 f1_b2] [b1 f1_b1] [b1 f2_b1] [b1 f3_b1] [b1 f4_b1]]))
           
           (is (= (tc/rows-query "↑building (↓floor)") 
                  [[b1 f4_b1] [b1 f3_b1] [b1 f2_b1] [b1 f1_b1] [b2 f1_b2] [b3]]))
           (is (= (tc/rows-query "↓building (↓floor)") 
                  [[b3] [b2 f1_b2] [b1 f4_b1] [b1 f3_b1] [b1 f2_b1] [b1 f1_b1]]))

           (is (= (tc/rows-query "↑floor#(building.name=\"b1\") (↓room)") 
                  [[f1_b1 r102_f1_b1] [f1_b1 r101_f1_b1] 
                   [f2_b1 r202_f2_b1] [f2_b1 r201_f2_b1] 
                   [f3_b1 r302_f3_b1] [f3_b1 r301_f3_b1] 
                   [f4_b1 r402_f4_b1] [f4_b1 r401_f4_b1] [f4_b1 r101_f4_b1]]))))


(defn floor-keyfn
  [floor]
  (.getNumber floor))

(deftest keyfn-as-var
         ^{:doc "Tests sorting in case the keyfn of the Floor class is a var."}
         (binding [tc/*mom* (assoc tc/*mom* 
                                   Floor
                                   (assoc (get tc/*mom* Floor) 
                                          :sort {:self {:keyfn #'floor-keyfn}}))]
           (is (= (tc/rows-query "↑building (↑floor)") 
                  [[b1 f1_b1] [b1 f2_b1] [b1 f3_b1] [b1 f4_b1] [b2 f1_b2] [b3]]))
           (is (= (tc/rows-query "↓building (↑floor)") 
                  [[b3] [b2 f1_b2] [b1 f1_b1] [b1 f2_b1] [b1 f3_b1] [b1 f4_b1]]))))


(deftest sort-prop
         (is (= (tc/rows-query "floor[↑number]") [[1] [2] [3] [4]]))
         (is (= (tc/rows-query "floor[↓number]") [[4] [3] [2] [1]]))
         (binding [tc/*mom* (assoc tc/*mom* Building
                                   (assoc (get tc/*mom* Building) 
                                          :sort {:self {:keyfn #(.getName %)}}))]
           (is (= (tc/rows-query "↑building[name]") [["b1"] ["b2"] ["b3"]]))
           (is (= (tc/rows-query "↓building[name]") [["b3"] ["b2"] ["b1"]]))
           (is (= (tc/rows-query "↑building[↑name]") [["b1"] ["b2"] ["b3"]]))
           (is (= (tc/rows-query "↓building[↓name]") [["b3"] ["b2"] ["b1"]]))
           (is (= (tc/rows-query "↑building[↓name]") [["b3"] ["b2"] ["b1"]]))
           (is (= (tc/rows-query "↓building[↑name]") [["b1"] ["b2"] ["b3"]]))

           (is (= (tc/rows-query "building[↑name address]") [["b1" "Street2"] ["b2" "Street1"] ["b3" "Street3"]]))
           (is (= (tc/rows-query "building[name ↑address]") [["b2" "Street1"] ["b1" "Street2"] ["b3" "Street3"]]))
           (is (= (tc/rows-query "building[↑name ↑address]") [["b1" "Street2"] ["b2" "Street1"] ["b3" "Street3"]]))

           (is (= (tc/rows-query "building[↑description ↑name]") 
                  [["Some desc" "b1"] ["Some desc" "b2"] ["Some desc2" "b3"]]))
           (is (= (tc/rows-query "building[↑description ↓name]") 
                  [["Some desc" "b2"] ["Some desc" "b1"] ["Some desc2" "b3"]]))
           (is (= (tc/rows-query "↑building[↑description ↑name]") 
                  [["Some desc" "b1"] ["Some desc" "b2"] ["Some desc2" "b3"]]))
           (is (= (tc/rows-query "↓building[↑description ↓name]") 
                  [["Some desc" "b2"] ["Some desc" "b1"] ["Some desc2" "b3"]]))
           (is (= (tc/rows-query "son (building[↑description ↑name])") 
                  [[son "Some desc" "b1"] [son "Some desc" "b2"] [son "Some desc2" "b3"]]))))


(deftest sort-by-comp-from-mom
         (binding [tc/*mom* (assoc tc/*mom* Building
                                   (assoc (get tc/*mom* Building) 
                                          :sort {:name {:comp #(* -1 (compare %1 %2))}}))]
           (is (= (tc/rows-query "building[↑name]") [["b3"] ["b2"] ["b1"]]))
           (is (= (tc/rows-query "building[↓name]") [["b1"] ["b2"] ["b3"]]))))


(deftest sort-default-property-by-comp-from-mom
         (binding [tc/*mom* (assoc tc/*mom* Building
                                   (assoc (get tc/*mom* Building) 
                                          :sort {:name {:comp #(* -1 (compare %1 %2))}}))]
           (is (= (tc/rows-query "building[↑&.]") [["b3"] ["b2"] ["b1"]]))
           (is (= (tc/rows-query "building[↓&.]") [["b1"] ["b2"] ["b3"]]))))


(deftest sort-self-object-by-comp-from-mom
         (binding [tc/*mom* (assoc tc/*mom* Building
                                   (assoc (get tc/*mom* Building) 
                                          :sort {:self {:comp #(* -1 (compare %1 %2))
                                                        :keyfn #(.getName %1)}}))]
           (is (= (tc/rows-query "building[↑&]") [[b3] [b2] [b1]]))
           (is (= (tc/rows-query "building[↓&]") [[b1] [b2] [b3]]))))


(deftest sort-by-pwns
         ^{:doc "Tests queries with sorting by properties which are not selected."}
         (binding [tc/*mom* (assoc tc/*mom* Building
                                   (assoc (get tc/*mom* Building) 
                                          :sort {:self {:comp #(* -1 (compare %1 %2))
                                                        :keyfn #(.getName %1)}}))]
           (is (= (tc/rows-query "{↑name}building") [[b1] [b2] [b3]]))
           (is (= (tc/rows-query "{↓name}building") [[b3] [b2] [b1]]))
           (is (= (tc/rows-query "{↑description ↑name}building") [[b1] [b2] [b3]]))
           (is (= (tc/rows-query "{↑description ↓name}building") [[b2] [b1] [b3]]))
           (is (= (tc/rows-query "{↓description ↑name}building") [[b3] [b1] [b2]]))
           (is (= (tc/rows-query "{↓description ↓name}building") [[b3] [b2] [b1]]))
           (is (= (tc/rows-query "{↑&}building") [[b3] [b2] [b1]]))
           (is (= (tc/rows-query "{↓&}building") [[b1] [b2] [b3]]))))

(deftest sort-by-pwns-function
         ^{:doc "Tests sorting by function."}
         (is (= (tc/rows-query "{↑@(count `floor')}building") [[b3] [b2] [b1]]))
         (is (= (tc/rows-query "{↓@(count `floor')}building") [[b1] [b2] [b3]]))

         (is (= (tc/rows-query "{↓@(count `room') ↑number}floor") 
                [[f1_b2] [f4_b1] [f1_b1] [f2_b1] [f3_b1]]))
         (is (= (tc/rows-query "{↓@(count `room') ↓number}floor") 
                [[f1_b2] [f4_b1] [f3_b1] [f2_b1] [f1_b1]]))
         (is (= (tc/rows-query "{↑@(count `room') ↑number}floor") 
                [[f1_b1] [f2_b1] [f3_b1] [f4_b1] [f1_b2]]))
         (is (= (tc/rows-query "{↑@(count `room') ↓number}floor") 
                [[f3_b1] [f2_b1] [f1_b1] [f4_b1] [f1_b2]])))

