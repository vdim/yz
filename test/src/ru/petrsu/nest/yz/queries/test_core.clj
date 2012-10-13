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

(ns ru.petrsu.nest.yz.queries.test-core
  ^{:author "Vyacheslav Dimitrov"
    :doc "Tests functions from tc namespaces."}
  (:use ru.petrsu.nest.yz.core 
        clojure.test)
  (:require [ru.petrsu.nest.yz.queries.core :as tc]))


(deftest t-eq-results?
         (let [f #(every? (fn [r] (%1 (apply tc/eq-results? r))) %2)]
           (is (f true?
                  [
                   [[] []]
                   [[1 []] [1 []]]
                   [[1 [] 1 []] [1 [] 1 []]]
                   [[2 [] 1 []] [2 [] 1 []]]
                   [[1 [] 2 []] [2 [] 1 []]]
                   [[1 [] 2 [] 3 []] [1 [] 2 [] 3 []]]
                   [[1 [1 []] 2 [] 3 []] [1 [1 []] 2 [] 3 []]]
                   [[2 [] 1 [1 []] 3 []] [1 [1 []] 2 [] 3 []]]
                   [[1 [4 [] 5 []] 2 [] 3 []] [1 [4 [] 5 []] 2 [] 3 []]]
                   [[1 [4 [] 5 [] 6 []] 2 [] 3 []] [1 [4 [] 5 [] 6 []] 2 [] 3 []]]
                   [[1 [4 [] 5 [7 []] 6 []] 2 [] 3 []] [1 [4 [] 5 [7 []] 6 []] 2 [] 3 []]]
                   [[1 [4 [] 6 [] 5 [7 []]] 2 [] 3 []] [1 [4 [] 5 [7 []] 6 []] 2 [] 3 []]]
                   ]))
           (is (f false?
                  [
                   [[] [1 []]]
                   [[0 []] [1 []]]
                   [[1 [] 2 []] [1 [] 1 []]]
                   [[1 [] 2 [] 3 []] [1 [] 2 [] 4 []]]
                   [[1 [] 2 [] 3 []] [1 [] 3 [] 3 []]]
                   [[1 [] 2 [] 3 []] [3 [] 2 [] 3 []]]
                   [[1 [1 []] 4 [] 3 []] [1 [1 []] 2 [] 3 []]]
                   [[1 [2 []] 2 [] 3 []] [1 [1 []] 2 [] 3 []]]
                   [[1 [1 []] 2 [] 3 []] [2 [1 []] 2 [] 3 []]]
                   [[1 [1 []] 2 [] 3 []] [1 [1 []] 2 [] 3 [4 []]]]
                   ]))
           ))
