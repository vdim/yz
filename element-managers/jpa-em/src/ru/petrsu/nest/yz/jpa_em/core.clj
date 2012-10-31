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

(ns ru.petrsu.nest.yz.jpa-em.core
  ^{:author "Vyacheslav Dimitrov"
    :doc "Implementation of the ElementManager for JPA 2.0."}
  (:require
   (ru.petrsu.nest.yz [core :as yz] [mom-utils :as mu]))
  (:import
    (javax.persistence EntityManager)
    (javax.persistence.criteria CriteriaQuery CriteriaBuilder Predicate Root)
    (ru.petrsu.nest.yz.core ElementManager ExtendedElementManager)
    (clojure.lang PersistentVector PersistentArrayMap))
  (:gen-class :name ru.petrsu.nest.yz.JPAEelemetManager
              :methods [;; JPA's element manager.
                        ^{:static true} 
                        [createJPAElementManager 
                         [javax.persistence.EntityManager] 
                         ru.petrsu.nest.yz.core.ElementManager]]))


(defn- contains-f?
  "Checks whether vector with predicates contains 
  function as a value of the key :value."
  [^PersistentArrayMap pred]
  (some #(or (map? (:value %)) (map? (:ids %))) pred))


(defn- get-path
  "Returns Path for specified vector with names of properties and
  the root element."
  [^Root root, ids]
  (reduce #(let [ids- (:id %2)]
             (reduce 
               (fn [r id]
                 (try (.join r id)
                   (catch Exception e (.get r id)))) %1 ids-)) root ids))


(defmacro complex-predicate
  "Creates complex predicate for specified operator ('.and' or '.or')
  from stack ('v') with set of Predicate. Adds new predicate to the 
  top of stack and returns new stack."
  [v, op, cb]
  `(conj (pop (pop ~v)) (~op ~cb (peek ~v) (peek (pop ~v)))))


(defn- ^Predicate get-op
  "Finds corresponding value of :func of pred map to some Clojure's function, 
  and then generates code for creating Predicate due to get-p macros."
  [^PersistentArrayMap pred, ^CriteriaBuilder cb, ^Root root]
  (let [op (:func pred)
        path (get-path root (:ids pred))
        ;v  (cs/trim (:value pred))
        v (:value pred)
        v (if (and (instance? String v) (> 0 (count v)) (= \" (nth v 0))) (subs v 1 (dec (count v))) v)]
    (cond (and (= #'clojure.core/= op) (nil? v)) (.isNull cb path)
          (= #'clojure.core/= op) (.equal cb path v)
          (= #'clojure.core/> op) (.gt cb path (Double/parseDouble v))
          (= #'clojure.core/< op) (.lt cb path (Double/parseDouble v))
          (= #'clojure.core/>= op) (.ge cb path (Double/parseDouble v))
          (= #'clojure.core/<= op) (.le cb path (Double/parseDouble v))
          (and (= #'clojure.core/not= op) (nil? v)) (.isNotNull cb path)
          (= #'clojure.core/not= op) (.notEqual cb path v)
          :else (throw (Exception. (str "No find function " op))))))


(defn- ^Predicate create-predicate
  "Takes stack with definition of restrictions 
  and CriteriaBuilding's instance. Returns Predicate."
  [^PersistentVector preds, ^CriteriaBuilder cb, ^Root root]
  ((reduce #(cond (map? %2) (conj %1 (get-op %2 cb root))
                  (= :and %2) (complex-predicate %1 .and cb)
                  (= :or %2) (complex-predicate %1 .or cb)
                  :else %2) [] preds) 0))


;; Implementation of JPA's ElementManager.
(deftype JPAElementManager [em]
  ElementManager
  ;; Implementation getElems's method. Root is created for 
  ;; specified class
  (^java.util.Collection getElems [this ^Class claz] 
     (.getElems this claz nil))

  ;; Implementation getMom's method. Gets all classes from JPA's metamodel and
  ;; then gerenates MOM.
  (getMom [_] (mu/gen-mom 
                (map #(.getJavaType %) 
                     (.. em getEntityManagerFactory getMetamodel getEntities))
                nil))
 
  ;; Value is got from bean of the object o.
  (^Object getPropertyValue [this ^Object o, ^String property]
     (get (bean o) (keyword property) :not-found))


  ExtendedElementManager
  (getElems 
    [_ claz preds]
    (let [^CriteriaBuilder cb (.getCriteriaBuilder em)
          cr (.createTupleQuery cb)
          ^Root root (. cr (from claz))
          cr (.. cr (multiselect [root]) (distinct true))
          ch-p (contains-f? preds) ; ch-p defines whether "preds" contains function.
          elems (map #(.get % 0) 
                     (.. em (createQuery (if (or (nil? preds) ch-p)
                                           cr 
                                           (.where cr (create-predicate preds cb root))))
                       getResultList))]
      (if ch-p
        (yz/filter-by-preds elems preds)
        elems))))
            

(defn ^ElementManager -createJPAElementManager
  "Returns implementation of JPA's ElementManager."
  [^EntityManager em]
  (JPAElementManager. em))


