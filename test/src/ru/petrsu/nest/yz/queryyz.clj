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

(ns ru.petrsu.nest.yz.queryyz
  ^{:author "Vyacheslav Dimitrov"
    :doc "Tests the YZQuery class."}
  (:use ru.petrsu.nest.yz.core 
        clojure.test)
  (:require [ru.petrsu.nest.yz.queries.core :as tc] 
            [ru.petrsu.nest.yz.queries.bd :as bd])
  (:import (ru.petrsu.nest.yz YZQuery)))

;; Define entity manager.

(use-fixtures :once (tc/setup-son bd/son))

;; Define the YZQuery instance.
(def qyz (YZQuery. "nest.mom" bd/mem))


;; Define tests

(deftest get-result-list
         ^{:doc "Tests the getResultList method."}
         (let [f #(tc/eq-colls (.getResultList (.create qyz %1)) %2)]
           (is (f "building" [[bd/b1] [bd/b2]]))
           (is (f "floor" [[bd/f1_b1] [bd/f2_b1] [bd/f3_b1] [bd/f1_b2]]))
           (is (f "building (floor)" [[bd/b1 bd/f1_b1] [bd/b1 bd/f2_b1] [bd/b1 bd/f3_b1] [bd/b2 bd/f1_b2]]))))


(deftest get-flat-list
         ^{:doc "Tests the getFlatList method."}
         (let [f #(tc/eq-colls (.getFlatResult (.create qyz %1)) %2)]
           (is (f "building" [bd/b1 bd/b2]))
           (is (f "floor" [bd/f1_b1 bd/f2_b1 bd/f3_b1 bd/f1_b2]))
           (is (f "building (floor)" [bd/b1 bd/f1_b1 bd/b1 bd/f2_b1 bd/b1 bd/f3_b1 bd/b2 bd/f1_b2]))))
