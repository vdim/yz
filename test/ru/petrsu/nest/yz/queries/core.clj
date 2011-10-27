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

(ns ru.petrsu.nest.yz.queries.core
  ^{:author "Vyacheslav Dimitrov"
    :doc "Helper functions for testing YZ's queries."}
  (:use ru.petrsu.nest.yz.core clojure.test)
  (:require [ru.petrsu.nest.yz.hb-utils :as hb]
            [ru.petrsu.nest.yz.core :as c])
  (:import (javax.persistence EntityManagerFactory Persistence EntityManager)
           (ru.petrsu.nest.son SON Building Room Floor)))


(def ^{:dynamic true} *url* (identity "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1;MVCC=TRUE;create=true"))
(def ^{:dynamic true} *dialect* (identity "org.hibernate.dialect.H2Dialect"))
(def ^{:dynamic true} *driver* (identity "org.h2.Driver"))

(defn create-em
  "Returns entity manager by specified name ('n', 'test-model' name 
  is used by default (for getting name by default you must pass 'n' as nil)) 
  and persists each object of model from 'elems'."
  [elems, n]
  (let [[n m] (if (nil? n) 
                ["test-model" {"hibernate.connection.url" *url*, 
                               "hibernate.dialect" *dialect*, 
                               "hibernate.connection.driver_class" *driver*}] 
                [n {}])
        em (.createEntityManager (Persistence/createEntityManagerFactory n m))
        _ (do (.. em getTransaction begin) 
            (dotimes [i (count elems)]
              (.persist em (elems i)))
            (.. em getTransaction commit))]
    em))


(defn- transform-q
  "Transforms query result to corresponding structure."
  [q-seq]
  (map #(if (vector? %)
          (transform-q %)
          (class %)) 
       q-seq))


(defn- transform-first-q
  "Transforms each first element of each nested query."
  [q]
  (if (vector? q)
    (vec (map #(if (vector? %)
                 [(transform-first-q (first %)) (transform-first-q (second %))]
                 (class %)) 
              q))
    (class q)))

(def ^{:dynamic true
       :tag EntityManager}
  *em*)

(def ^{:dynamic true}
  *mom*)

;(declare *em* *mom*)
(defn setup 
  "Returns function for creating entity manager 
  and closing it after executing all tests."
  ([sons]
   (setup sons nil))
  ([sons n]
   (fn [f]
     (let [em (create-em sons n)]
       (binding [*em* em
                 *mom* (hb/gen-mom-from-metamodel (.getEntityManagerFactory em))]
         (f)
         (.close *em*))))))


(defn- do-q
  "Executes query, If error is occured then
  throw is thrown, else key 'k' of result of the 
  query is returned."
  [query k]
  (let [rq (c/pquery query *mom* *em*)]
    (if (not (nil? (:error rq)))
      (throw Exception e "Query exception.")
      (k rq))))

(defn r-query
  "Returns :result of core/pquery."
  [query]
  (do-q query :result))


(defn rows-query
  "Returns :rows of core/pquery."
  [query]
  (do-q query :rows))


(defn qstruct?
  "Defines whether structure of the specified query correspends to
  the specified query."
  [query, structure]
  (let [rq (if (string? query) (r-query query) query)]
    (= structure (transform-first-q rq))))


(defn check-query
  "Checks correspondence specified result of query to
  specified structure."
  [query, structure]
  (let [rq (if (string? query) (r-query query) query)]
    (= structure (transform-q rq))))


