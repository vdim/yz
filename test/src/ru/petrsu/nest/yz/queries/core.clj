;;
;; Copyright 2011-2012 Vyacheslav Dimitrov <vyacheslav.dimitrov@gmail.com>
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
  (:use clojure.test)
  (:use clojure.java.shell)
  (:require [ru.petrsu.nest.yz.hb-utils :as hb]
            [net.kryshen.planter.store :as planter]
            [ru.petrsu.nest.yz.core :as c]
            [ru.petrsu.nest.son.local-sm :as lsm])
  (:import (javax.persistence EntityManagerFactory Persistence EntityManager)
           (ru.petrsu.nest.son SonBeanUtils SON)
           (ru.petrsu.nest.yz.core ElementManager)))

;; ElementManager (see ru.petrsu.nest.yz.core/ElementManager for more details).
(def ^:dynamic *em*)

;; A map of Object Model.
(def ^:dynamic *mom*)

;;
;; Definition memory ElementManager (see ru.petrsu.nest.yz.core/ElementManager).
;;

(def ^:dynamic *file-mom* "nest.mom")

(defn se-iterator
  "Implements iterable for working with clojure in usual manner."
  [son]
  (reify Iterable 
    (^java.util.Iterator iterator [_] 
       (ru.petrsu.nest.son.SonBeanUtils$BreadthFirstIterator. son))))


(defn em-memory
  "Implementation of the memory ElementManager."
  [son, id-cache]
  (let [elems (map identity (se-iterator son))]
    (reify ElementManager
      (^java.util.Collection getElems [_ ^Class claz] 
           (filter #(instance? claz %) elems))
      (getMom [_] (throw (UnsupportedOperationException. "Not supported.")))

      ;; Value is got from bean of the object o.
      (^Object getPropertyValue [this ^Object o, ^String property]
         ((keyword property) (bean o))))))
      ;(getById [_ ^Object id] (get id-cache id))))


(defn create-id-cache
  "Creates id's cache. This is Map where key is id of 
  object and value is object. (Need for testing getById 
  function of ElementManager.)"
  [son]
  (reduce #(assoc %1 (.getId %2) %2) 
          {}
          (map identity
               (reify Iterable 
                 (^java.util.Iterator iterator [_] 
                    (ru.petrsu.nest.son.SonBeanUtils$BreadthFirstIterator. son))))))


(defn create-emm
  "Returns implementation of the memory ElementManager 
  for specified son."
  [son]
  (em-memory son (create-id-cache son)))


(defn create-emlm
  "Returns implementation of the LocalSonManager 
  for specified son."
  ([son]
   (create-emlm son "data"))
  ([son dir]
   (let [_ (sh "rm" "-Rf" dir) ; Dirty hack: deletes directory with database before making queries.
         store (planter/store dir)
         _ (planter/register-bean store son)
         _ (planter/save-all store)]
     (lsm/create-lsm store))))


;;
;; Definition fixtures (see doc string to the clojure.test namespace for more details).
;;

(def type-em 
  "Defines type of the ElementManager:
    :localsonmanager - LocalSonManager (create-emlm function is used).
    :memorymanager - MemoryElementManager (em-memory function is used)."
  :memorymanager)


(defn setup-son
  "Creates database for the specified son due to specified 
  type of the ElementManager and then executes queries."
  ([son]
   (setup-son son *file-mom*))
  ([son nf]
   (fn [f]
     (binding [*mom* (hb/mom-from-file nf)
               *em* (case type-em
                      :localsonmanager (create-emlm son)
                      :memorymanager (em-memory son (create-id-cache son))
                      (throw (Exception. "ElementManager is not defined.")))]
       (f)))))


;;
;; Definition of JPA's ElementManager.
;;

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

;;
;; Common helper function.
;;

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


(defn- do-q
  "Executes query, If error is occured then
  throw is thrown, else key 'k' of result of the 
  query is returned."
  [query k]
  (let [rq (c/pquery query *mom* *em*)]
    (if (not (nil? (:error rq)))
      (throw (Exception. (str "Query exception: " (:error rq))))
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


(defn eq-colls
  "Equals two collections."
  [coll1 coll2]
  (let [s-coll1 (set coll1)
        s-coll2 (set coll2)]
    (and
      (empty? (remove #(contains? s-coll2 %) coll1))
      (empty? (remove #(contains? s-coll1 %) coll2)))))


(defn eq-maps
  "Equals two maps where value is collection 
  (collections are equaled due to eq-colls)."
  [map1 map2]
  (let [check-map #(reduce (fn [r [k v]] (and r (eq-colls v (get %1 k)))) true %2)]
    (and
      (check-map map1 map2)
      (check-map map2 map1))))
