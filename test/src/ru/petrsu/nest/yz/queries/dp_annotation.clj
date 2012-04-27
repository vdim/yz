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

(ns ru.petrsu.nest.yz.queries.dp-annotation
  ^{:author "Vyacheslav Dimitrov"
    :doc "Tests default properties which is defined 
         due to the DefaultProperty annotation."}
  (:use ru.petrsu.nest.yz.core 
        clojure.test 
        ru.petrsu.nest.yz.queries.uni-bd)
  (:require [ru.petrsu.nest.yz.queries.core :as tc]))


(defn- query-uni-db
  "Executes specified query for university database."
  [query]
  (:rows (pquery query uni-em)))


(deftest preds
         ^{:doc "Tests queries with predicates and default property."}
         (let [rows (query-uni-db "course#(.=\"Algebra\")")]
           (is (= (count rows) 1))
           (is (= ((nth rows 0) 0) alg))))


(deftest props
         ^{:doc "Tests queries with projection and default property."}
         (let [rows (flatten (query-uni-db "course[&.]"))]
           (is (= (count rows) 4))
           (is (tc/eq-colls rows ["Algebra" "Geometry" "Russian" "German"])))
         (let [rows (flatten (query-uni-db "course[& &.]"))]
           (is (= (count rows) 8))
           (is (tc/eq-colls rows ["Algebra" alg "Geometry" geo "Russian" rus "German" ger]))
         ))

         
(deftest sorts
         ^{:doc "Tests queries with predicates and sorting."}
         (is (= (flatten (query-uni-db "{a:&.}course")) 
                [alg geo ger rus]))
         (is (= (flatten (query-uni-db "{d:&.}course")) 
                [rus ger geo alg])))

