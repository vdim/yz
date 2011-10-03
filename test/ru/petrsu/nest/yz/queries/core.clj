(ns ru.petrsu.nest.yz.queries.core
  ^{:author "Vyacheslav Dimitrov"
    :doc "Helper functions for testing YZ's queries."}
  (:use ru.petrsu.nest.yz.core clojure.test)
  (:require [ru.petrsu.nest.yz.hb-utils :as hb]
            [ru.petrsu.nest.yz.core :as c])
  (:import (javax.persistence EntityManagerFactory Persistence EntityManager)
           (ru.petrsu.nest.son SON Building Room Floor)))


(defn create-em
  "Returns entity manager by specified name ('n', 'test-model' name 
  is used by default (for getting name by default you must pass 'n' as nil)) 
  and persists each object of model from 'elems'."
  [elems, n]
  (let [n (if (nil? n) "test-model" n)
        em (.createEntityManager (Persistence/createEntityManagerFactory n))
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

(declare *em* *mom*)
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


(defn r-query
  "Returns :result of core/pquery."
  [query]
  (:result (c/pquery query *mom* *em*)))


(defn rows-query
  "Returns :rows of core/pquery."
  [query]
  (:rows (c/pquery query *mom* *em*)))


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


