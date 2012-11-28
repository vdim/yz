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
  (:require [ru.petrsu.nest.yz.mom-utils :as mu]
            [net.kryshen.planter.store :as planter]
            [ru.petrsu.nest.yz.core :as c]
            [ru.petrsu.nest.yz.parsing :as p]
            [ru.petrsu.nest.yz.yz-factory :as yzf]
            [ru.petrsu.nest.son.local-sm :as lsm] 
            [ru.petrsu.nest.yz.jpa-em.mom :as hmom])
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
  ([son]
   (em-memory son nil))
  ([son mom]
   (let [elems (map identity (se-iterator son))]
     (reify ElementManager
       (^java.lang.Iterable getElems [_ ^Class claz] 
            (filter #(instance? claz %) elems))
       (getMom [_] mom)
 
       ;; Value is got from bean of the object o.
       (^Object getPropertyValue [this ^Object o, ^String property]
          ((keyword property) (bean o)))))))


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
    :memorymanager - MemoryElementManager (em-memory function is used). 
    :multicollectionmanager - MultiCollectionElementManager (cm-em is used)."
  :memorymanager)

(def classes
  [ru.petrsu.nest.son.SON 
   ru.petrsu.nest.son.Occupancy 
   ru.petrsu.nest.son.NetworkElement 
   ru.petrsu.nest.son.Device 
   ru.petrsu.nest.son.LinkInterface 
   ru.petrsu.nest.son.UnknownLinkInterface 
   ru.petrsu.nest.son.EthernetInterface 
   ru.petrsu.nest.son.VLANInterface 
   ru.petrsu.nest.son.NetworkInterface 
   ru.petrsu.nest.son.UnknownNetworkInterface 
   ru.petrsu.nest.son.IPv4Interface 
   ru.petrsu.nest.son.Network 
   ru.petrsu.nest.son.UnknownNetwork 
   ru.petrsu.nest.son.IPNetwork 
   ru.petrsu.nest.son.OrganizationalElement 
   ru.petrsu.nest.son.AbstractOU 
   ru.petrsu.nest.son.SimpleOU 
   ru.petrsu.nest.son.CompositeOU 
   ru.petrsu.nest.son.SpatialElement 
   ru.petrsu.nest.son.Room 
   ru.petrsu.nest.son.Floor 
   ru.petrsu.nest.son.Building])

(defn setup-son
  "Creates database for the specified son due to specified 
  type of the ElementManager and then executes queries."
  ([son]
   (setup-son son *file-mom*))
  ([son nf]
   (fn [f]
     (let [mom (mu/mom-from-file nf)]
       (binding [*mom* mom
                 *em* (case type-em
                        :localsonmanager (create-emlm son)
                        :memorymanager (em-memory son mom)
                        :multicollectionmanager 
                        (let [bs (seq (.getBuildings son))
                              coll (or bs [son])] 
                          (yzf/mc-em coll classes mom))
                        (throw (Exception. "ElementManager is not defined.")))]
         (f))))))


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
                 *mom* (hmom/gen-mom-from-metamodel (.getEntityManagerFactory em))]
         (f)
         (.close *em*))))))

;;
;; Common helper function.
;;

(defn- transform-q
  "Transforms query result to corresponding structure."
  [q-seq]
  (vec (map #(if (vector? %)
          (transform-q %)
          (class %)) 
       q-seq)))


(defn- transform-first-q
  "Transforms each first element of each nested query."
  [q]
  (cond (and (vector? q) (empty? q)) []
        (vector? q) [(transform-first-q (first q)) (transform-first-q (second q))]
        :else (class q)))


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


(defn qparse
  "Parses specified query using *mom*."
  [query]
  (p/parse query *mom*))


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
  (if (and (coll? coll1) (coll? coll2))
    (let [s-coll1 (set coll1)
          s-coll2 (set coll2)
          f (fn [c1 c2] (empty? (remove #(if (coll? %)
                                           (some (fn [coll11] (eq-colls coll11 %)) c2)
                                           (contains? s-coll2 %)) c1)))]
      (if (or (not= (count coll1) (count coll2))
              (not= (count s-coll1) (count s-coll2)))
        false
        (and
          (f s-coll1 s-coll2)
          (f s-coll2 s-coll1))))
    false))


(defn eq-results?
  "Equals two result of query (value of the 
  :result key from structure which 
  returns by the pquery function)."
  [c1 c2]
  (if (not= (count c1) (count c2))
    false
    (let [pc1 (set (partition 2 c1))
          pc2 (set (partition 2 c2))
          r (for [a pc1 b pc2 :when (let [[f1 s1] a
                                          [f2 s2] b]
                                      (and 
                                        (if (and (coll? f1) (coll? f2))
                                          (eq-colls f1 f2)
                                          (= f1 f2))
                                        (if (and (empty? s1) (empty? s2)) 
                                          true
                                          (eq-results? s1 s2))))]
              a)]
      (= (count r) (count pc1) (count pc2)))))

    
(defn eq-maps
  "Equals two maps where value is collection 
  (collections are equaled due to eq-colls)."
  [map1 map2]
  (let [check-map #(reduce (fn [r [k v1]] (and r (let [v2 (get %1 k)
                                                       t (cond
                                                           (map? v1) (eq-maps v1 v2)
                                                           (coll? v1) (eq-colls v1 v2)
                                                           :else (= v1 v2))
                                                       _ (if (not t) (println "k = " k ", v1 = " v1 ", v2 = " v2))]
                                                   t)))
                           true %2)]
    (and
      (check-map map1 map2)
      (check-map map2 map1))))
