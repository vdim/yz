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

(ns ru.petrsu.nest.yz.queries.params
  ^{:author "Vyacheslav Dimitrov"
    :doc "Tests queries with parameters (defq)."}
  (:use clojure.test ru.petrsu.nest.yz.core
        ru.petrsu.nest.yz.queries.bd)
  (:require [ru.petrsu.nest.yz.queries.core :as tc] 
            [ru.petrsu.nest.yz.queries.bd :as bd]))


(defq ^{:mom bd/bd-mom, :em bd/mem} q1 "building#(name=$1)")

(deftest t-q1
         (let [f #(is (= (-> (q1 %1) :rows ffirst) %2))]
           (f "MB" bd/b1)
           (f "TK" bd/b2)
           (f "" nil)))


(defq ^{:mom bd/bd-mom, :em bd/mem} q2 "building#(name=($1 || $2))")

(deftest t-q2
         (let [f #(is (tc/eq-colls (:rows (q2 %1 %2)) %3))]
           (f "MB" "" [[bd/b1]])
           (f "MB" "" [[bd/b1]])
           (f "MB" "UK1" [[bd/b1]])
           (f "TK" "" [[bd/b2]])
           (f "TK" "UK2" [[bd/b2]])
           (f "TK" "MB" [[bd/b2] [bd/b1]])
           (f "MB" "TK" [[bd/b2] [bd/b1]])))


(defq ^{:mom bd/bd-mom, :em bd/mem} q3 "$1.floor")

(deftest t-q3
         (let [f #(is (tc/eq-colls (:rows (q3 %1)) %2))]
           (f bd/b1 [[bd/f1_b1] [bd/f2_b1] [bd/f3_b1]])
           (f bd/b2 [[bd/f1_b2]])
           (f bd/r1001_f1_b2 [[bd/f1_b2]])
           (f bd/rd_ei1_ni1 [[bd/f1_b2]])
           (f bd/rootDevice [[bd/f1_b2]])
           (f bd/d1 [[bd/f1_b1]])
           (f bd/d1_ei1 [[bd/f1_b1]])))


(defq ^{:mom bd/bd-mom, :em bd/mem} q4 "$1.floor#(number=$2)")

(deftest t-q4
         (let [f #(is (tc/eq-colls (:rows (q4 %1 %2)) %3))]
           (f bd/b1 1 [[bd/f1_b1]])
           (f bd/b1 2 [[bd/f2_b1]])
           (f bd/b1 3 [[bd/f3_b1]])
           (f bd/b1 4 [])
           (f bd/b2 1 [[bd/f1_b2]])
           (f bd/b2 2 [])))


(defq ^{:mom bd/bd-mom, :em bd/mem} q5 "$1#(number=$2)")

(deftest t-q5
         (let [f #(is (tc/eq-colls (:rows (q5 %1 %2)) %3))]
           (f bd/f1_b1 1 [[bd/f1_b1]])
           (f bd/f1_b1 2 [])
           (f bd/f2_b1 2 [[bd/f2_b1]])
           (f bd/f2_b1 1 [])))


; List of properties after parameter.
(defq ^{:mom bd/bd-mom, :em bd/mem} q6 "$1[name]")

(deftest t-q6
         (let [f #(is (tc/eq-colls (:rows (q6 %1)) %2))]
           (f bd/b1 [["MB"]])
           (f bd/b2 [["TK"]])
           (f [bd/b1 bd/b2] [["MB"] ["TK"]])))


; Definition defq without ElementManager.
(defq ^{:mom bd/bd-mom} q7 "$1[name]")

(deftest t-q7
         (let [f #(is (tc/eq-colls (:rows (q7 %1 bd/mem)) %2))]
           (f bd/b1 [["MB"]])
           (f bd/b2 [["TK"]])
           (f [bd/b1 bd/b2] [["MB"] ["TK"]])))

; Parameter in medium if path: building.$1.room
(defq ^{:mom bd/bd-mom, :em bd/mem} q8 "building.$1.room")
(deftest t-q8
         (let [f #(is (tc/eq-colls (:rows (q8 %1)) %2))]
           (f bd/f1_b1 [[bd/r101_f1_b1] [bd/r102_f1_b1]])
           (f bd/f2_b1 [[bd/r201_f2_b1] [bd/r202_f2_b1]])))


(defq ^{:mom bd/bd-mom, :em bd/mem} q9 "building.$1")
(deftest t-q9
         (let [f #(is (= (:rows (q9 %1)) %2))]
           (f bd/f1_b1 [[bd/f1_b1]])
           (f bd/f2_b1 [[bd/f2_b1]])))


(defq ^{:mom bd/bd-mom, :em bd/mem} q10 "building.$1[number]")
(deftest t-q10
         (let [f #(is (= (:rows (q10 %1)) %2))]
           (f bd/f1_b1 [[1]])
           (f bd/f2_b1 [[2]])))


(defq ^{:mom bd/bd-mom, :em bd/mem} q11 "building.$1#(number=1)")
(deftest t-q11
         (let [f #(is (tc/eq-colls (:rows (q11 %1)) %2))]
           (f [bd/f1_b1 bd/f2_b1] [[bd/f1_b1]])))


(defq ^{:mom bd/bd-mom, :em bd/mem} q12 "building.$1#(number=$2)")
(deftest t-q12
         (let [f #(is (tc/eq-colls (:rows (q12 %1 %2)) %3))]
           (f [bd/f1_b1 bd/f2_b1] 1 [[bd/f1_b1]])
           (f bd/f1_b1 1 [[bd/f1_b1]])
           (f bd/f2_b1 1 [])
           (f [bd/f1_b1] 1 [[bd/f1_b1]])
           (f [bd/f2_b1] 1 [])
           (f [bd/f1_b1 bd/f2_b1] 2 [[bd/f2_b1]])
           (f [bd/f1_b1 bd/f2_b1] 3 [])))


; List of properties after parameter.
(defq ^{:mom bd/bd-mom, :em bd/mem} q13 "$1.name")

(deftest t-q13
         (let [f #(is (tc/eq-colls (:rows (q13 %1)) %2))]
           (f bd/b1 [["MB"]])
           (f bd/b2 [["TK"]])
           (f [bd/b1 bd/b2] [["MB"] ["TK"]])))


