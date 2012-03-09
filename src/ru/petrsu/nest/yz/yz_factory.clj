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

(ns ru.petrsu.nest.yz.yz-factory
  ^{:author "Vyacheslav Dimitrov"
    :doc "Factory for creating different types of the ElementManager 
         (see code from the core.clj file) and the QueryYZ instances.

         At the present moment factory supports the following ElementManagers:
          - JPA's ElementManager (method createJPAElementManager). For getting it
            you must pass your javax.persistence.EntityManager.

          - Collection's ElementManager (method createCollectionElementManager). 
            It is simple collection's element manager worked with elements
            direct in memory. For getting it you must specify your collection
            with elements and list of classes for MOM if any."}

  (:require
   (ru.petrsu.nest.yz [core :as yz] [hb-utils :as hu]))
  (:import
    (javax.persistence EntityManager)
    (javax.persistence.criteria CriteriaQuery CriteriaBuilder Predicate Root)
    (ru.petrsu.nest.yz.core ElementManager ExtendedElementManager)
    (ru.petrsu.nest.yz QueryYZ)
    (java.util List Collection)
    (clojure.lang APersistentMap PersistentVector Keyword PersistentArrayMap))
  (:gen-class :name ru.petrsu.nest.yz.YZFactory
              :methods [;; JPA's element manager.
                        ^{:static true} 
                        [createJPAElementManager 
                         [javax.persistence.EntityManager] 
                         ru.petrsu.nest.yz.core.ElementManager]

                        ;; Single collection ElementManager.
                        ^{:static true} 
                        [createSCElementManager 
                         [java.util.Collection]
                         ru.petrsu.nest.yz.core.ElementManager]

                        ;; Single collection ElementManager.
                        ^{:static true} 
                        [createSCElementManager 
                         [java.util.Collection java.util.Collection]
                         ru.petrsu.nest.yz.core.ElementManager]
                        
                        ;; Multiple collection ElementManager.
                        ^{:static true} 
                        [createMCElementManager 
                         [java.util.Map]
                         ru.petrsu.nest.yz.core.ElementManager]
                        
                        ;; Multiple collection ElementManager.
                        ^{:static true} 
                        [createMCElementManager 
                         [java.util.Collection java.util.Collection]
                         ru.petrsu.nest.yz.core.ElementManager]

                        ;; Creates QueryYZ for working with collection.
                        ^{:static true} 
                        [createCollectionQueryYZ [java.util.Collection]
                         ru.petrsu.nest.yz.QueryYZ]

                        ;; Creates QueryYZ for working with collection.
                        ^{:static true} 
                        [createCollectionQueryYZ [java.util.Collection java.util.Collection]
                         ru.petrsu.nest.yz.QueryYZ]
                        
                        ;; Creates MOM from specified file.
                        ^{:static true} 
                        [createMomFromFile [String] clojure.lang.APersistentMap]
                        ]))


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
  (getMom [_] (hu/gen-mom 
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


(defn ^EntityManager c-em
  "Returns implementation of the ElementManager for collections."
  [^Collection coll, ^Collection classes]
  (let [cls (if (or (nil? classes) (empty? classes))
              (and (seq coll) [(class (nth coll 0))])
              classes)
        mom (hu/gen-mom cls nil)]
    (reify ElementManager
      (^Collection getElems [_ ^Class _] coll)
      (^APersistentMap getMom [_] mom)
      
      ;; Value is got from bean of the object o.
      (^Object getPropertyValue [this ^Object o, ^String property]
         (let [b (if (map? o) o (bean o))]
           ((keyword property) b))))))


(defn ^EntityManager -createSCElementManager
  "Single Collection ElementManager. User defines
  single collection with some elements and list 
  of model's classes so he can execute query
  only with respect to elements from this collection."
  ([^Collection coll]
   (c-em coll nil))
  ([^Collection coll, ^Collection classes]
   (c-em coll classes)))


(defn -createCollectionQueryYZ
  "Returns instance of the QueryYZ which works with collection."
  ([^java.util.Collection coll]
   (ru.petrsu.nest.yz.QueryYZ. (c-em coll nil)))
  ([^java.util.Collection coll ^java.util.Collection classes]
   (ru.petrsu.nest.yz.QueryYZ. (c-em coll classes))))


(defn -createMomFromFile
  "Creates MOM from specified name of file."
  [^String f-name]
  (hu/mom-from-file f-name))


(defn find-related-colls
  "For specified collection of some elements and class of 
  this elements finds related collections which
  elements have class from classes."
  [^Collection coll main-cl ^Collection classes]
  (let [classes (remove #(= main-cl %) classes)
        em (c-em coll (conj classes main-cl))
        cl-s (cond (string? main-cl) main-cl 
                   (class? main-cl) (.getSimpleName main-cl)
                   :else (throw (Exception. "Unexpected type of main-cl. It must be String or Class.")))
        q (str cl-s " (%s)")]
    (reduce #(let [rq (yz/pquery (format q (.getSimpleName %2)) em)
                   error (:error rq)]
               (if error
                 (throw (Exception. error))
                 (assoc 
                   %1 %2 
                   (distinct (remove nil? (map (fn [row] 
                                                 (try (nth row 1) (catch Exception _ nil))) 
                                               (:rows rq)))))))

            {main-cl coll}
            classes)))


(defn ^EntityManager mc-em
  "Returns implementation of the 
  ElementManager for multiple collections."
  [coll-or-elems, ^Collection classes]
  (let [classes (if (map? coll-or-elems) 
                  (keys coll-or-elems)
                  classes)
        mom (hu/gen-mom classes nil)
        ; Maps class to collection of elements with this class.
        elems (cond (map? coll-or-elems) coll-or-elems
                    (coll? coll-or-elems)
                    (if (empty? coll-or-elems)
                      (reduce #(assoc %1 %2 []) {} classes)
                      (find-related-colls coll-or-elems (class (nth coll-or-elems 0)) classes))
                    :else (throw (Exception. "Unexpected type of coll-or-elems. 
                                             It must be map or collection.")))]
    (reify ElementManager
      (^Collection getElems [_ ^Class cl] (get elems cl))
      (^APersistentMap getMom [_] mom)
      
      ;; If o is map then value of key "property" is returned 
      ;; otherwise value is got from bean of the object o.
      (^Object getPropertyValue [this ^Object o, ^String property]
         (let [b (if (map? o) o (bean o))]
           ((keyword property) b))))))


(defn ^EntityManager -createMCElementManager
  "Multiple collection ElementManager. 
  First version: 
    elems is map where key is class and value is collection of
    elements with this class.
  Second version: 
    coll is collection of elements and classes is 
    list of model's classes. elems (see desctiption above) 
    is built automatically."
  ([^java.util.Map elems]
   (mc-em elems nil))
  ([^Collection coll, ^Collection classes]
   (mc-em coll classes)))
