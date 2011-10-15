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

(ns ru.petrsu.nest.yz.benchmark.benchmark
  ^{:author "Vyacheslav Dimitrov"
    :doc "System for benchmarking different types of queries."}
  (:use ru.petrsu.nest.yz.core)
  (:require [ru.petrsu.nest.yz.benchmark.bd-utils :as bu]
            [ru.petrsu.nest.yz.hb-utils :as hb]
            [ru.petrsu.nest.yz.benchmark.yz :as yz]
            [ru.petrsu.nest.yz.benchmark.hql :as hql]))


(defn- create-em
  "Returns EntityManager due to specified name (bench is default)."
  ([]
   (create-em "bench"))
  ([n]
   (.createEntityManager (javax.persistence.Persistence/createEntityManagerFactory n))))


(defn get-clean-bd
  "Creates entity manager, clean database 
  and generates some database's structure."
  []
  (let [emb (create-em)]
    (do (bu/schema-export)
      (bu/create-bd 1000 emb))
    emb))


(defn run-query
  "Runs specified query ('q') due to specified function ('f')
  which takes string-query and some EntityManager."
  [f q em]
  (if (nil? em)
   (let [em (get-clean-bd)
         t (run-query f q em)]
     (.close em)
     t)
    (f q em)))


(def ncount
  ^{:doc "Defines amount of evaluating set of queries. "}
  (identity 100))


(defn- add-seq
  "Adds two collection."
  [coll1 coll2]
  (map #(vec (map + %1 %2)) coll1 coll2))


(defn- getf
  "Returns function."
  [f]
  (resolve (symbol (str "ru.petrsu.nest.yz.benchmark." f "/do-q"))))


(defn -main
  "Takes a number of query and some settings and returns time of executing query."
  ([num, n, url, dialect, driver, ql]
   ((getf ql)
      num, n, 
      {"hibernate.connection.url" url, 
       "hibernate.dialect" dialect, 
       "hibernate.connection.driver_class" driver}))
  ([num, n, ql]
   ((getf ql) num, n, {})))

