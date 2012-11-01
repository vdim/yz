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
         (see code from the core.clj file) and the YZQuery instances.

         At the present moment factory supports the following ElementManagers:
          - JPA's ElementManager (method createJPAElementManager). For getting it
            you must pass your javax.persistence.EntityManager.

          - Collection's ElementManager (method createCollectionElementManager). 
            It is simple collection's element manager worked with elements
            direct in memory. For getting it you must specify your collection
            with elements and list of classes for MOM if any."}

  (:require
   (ru.petrsu.nest.yz [core :as yz] [mom-utils :as mu]))
  (:import
    (ru.petrsu.nest.yz.core ElementManager)
    (ru.petrsu.nest.yz YZQuery)
    (java.util List Collection)
    (clojure.lang APersistentMap))
  (:gen-class :name ru.petrsu.nest.yz.YZFactory
              :methods [;; Single collection ElementManager. 
                        ;; Collection is specified.
                        ^{:static true} 
                        [createSCElementManager 
                         [java.util.Collection]
                         ru.petrsu.nest.yz.core.ElementManager]

                        ;; Single collection ElementManager. 
                        ;; Collection and list of classes are specified.
                        ^{:static true} 
                        [createSCElementManager 
                         [java.util.Collection java.util.Collection]
                         ru.petrsu.nest.yz.core.ElementManager]
                        
                        ;; Multiple collection ElementManager.
                        ;; Map is specifed where key is class and
                        ;; value is collection of objects.
                        ^{:static true} 
                        [createMCElementManager 
                         [java.util.Map]
                         ru.petrsu.nest.yz.core.ElementManager]
                        
                        ;; Multiple collection ElementManager.
                        ;; Collection and list of classes are specified.
                        ^{:static true} 
                        [createMCElementManager 
                         [java.util.Collection java.util.Collection]
                         ru.petrsu.nest.yz.core.ElementManager]

                        ;; Creates YZQuery for working with collection.
                        ;; Collection is specified.
                        ^{:static true} 
                        [createCollectionYZQuery [java.util.Collection]
                         ru.petrsu.nest.yz.YZQuery]

                        ;; Creates YZQuery for working with collection.
                        ;; Collection and list of classes are specified.
                        ^{:static true} 
                        [createCollectionYZQuery [java.util.Collection java.util.Collection]
                         ru.petrsu.nest.yz.YZQuery]
                        
                        ;; Creates MOM from specified file.
                        ^{:static true} 
                        [createMomFromFile [String] clojure.lang.APersistentMap]
                        ]))


(defn ^ElementManager c-em
  "Returns implementation of the 
  ElementManager for collections. Parameters:
    - coll - collection with root elements.
    - classes - list with classes.
    - mom - name of file with MOM (if any, nil by default) 
        or mom itself.
        In case mom is nil then mom is generated due to 
        the gen-mom function from the mom-utils namespace."
  ([^Collection coll, ^Collection classes]
   (c-em coll classes :generate))
  ([^Collection coll, ^Collection classes mom]
   (let [mom (cond 
               ; getting mom from file.
               (string? mom) (mu/mom-from-file mom)
               ; generating mom from list of classes.    
               (= mom :generate) 
               (let [cls (if (or (nil? classes) (empty? classes))
                           (and (seq coll) [(class (nth coll 0))])
                           classes)]
                 (mu/gen-mom cls nil))
               ;If mom is itself or nil.
               :else mom)]
     (reify ElementManager
       (^Iterable getElems [_ ^Class cl] 
          (if (= cl Object) 
            coll
            (filter #(instance? cl %) coll)))
       (^APersistentMap getMom [_] mom)
      
       ;; Value is got from bean of the object o.
       (^Object getPropertyValue [this ^Object o, ^String property]
          (let [b (if (map? o) o (bean o))]
            ((keyword property) b)))))))


(defn ^ElementManager -createSCElementManager
  "Single Collection ElementManager. User defines
  single collection with some elements and list 
  of model's classes so he can execute query
  only with respect to elements from this collection."
  ([^Collection coll]
   (c-em coll nil))
  ([^Collection coll, ^Collection classes]
   (c-em coll classes)))


(defn -createCollectionYZQuery
  "Returns instance of the YZQuery which works with collection."
  ([^java.util.Collection coll]
   (ru.petrsu.nest.yz.YZQuery. (c-em coll nil)))
  ([^java.util.Collection coll ^java.util.Collection classes]
   (ru.petrsu.nest.yz.YZQuery. (c-em coll classes))))


(defn -createMomFromFile
  "Creates MOM from specified name of file."
  [^String f-name]
  (mu/mom-from-file f-name))


(defn find-related-colls
  "For specified collection of some elements and class of 
  this elements finds related collections which
  elements have class from classes."
  [^Collection coll ^Class main-cl ^Collection classes, mom]
  (let [classes (remove #(= main-cl %) classes)
        em (c-em coll (conj classes main-cl) mom)
        q (str (.getSimpleName main-cl) " (%s)")]
    (reduce #(let [rq (yz/pquery (format q (.getSimpleName %2)) em)
                   error (:error rq)]
               (if error
                 %1
                 ;(throw (Exception. error))
                 (assoc 
                   %1 %2 
                   (distinct (remove nil? (map (fn [row] 
                                                 (try (nth row 1) (catch Exception _ nil))) 
                                               (:rows rq)))))))
            {main-cl coll}
            classes)))


(defn ^ElementManager mc-em
  "Returns implementation of the 
  ElementManager for multiple collections."
  ([coll-or-elems, ^Collection classes]
   (mc-em coll-or-elems, classes, nil))
  ([coll-or-elems, ^Collection classes mom]
   (let [classes (if (map? coll-or-elems) 
                   (keys coll-or-elems)
                   classes)
         mom (cond (map? mom) mom
                   (string? mom) (mu/mom-from-file mom)
                   :else (mu/gen-mom classes nil))
         ; Maps class to collection of elements with this class.
         elems (cond (map? coll-or-elems) coll-or-elems
                     (coll? coll-or-elems)
                     (if (empty? coll-or-elems)
                       (reduce #(assoc %1 %2 []) {} classes)
                       (find-related-colls coll-or-elems (class (nth coll-or-elems 0)) classes mom))
                     :else (throw (Exception. (str "Unexpected type of coll-or-elems: " 
                                                   (class coll-or-elems) 
                                                   ". It must be map or collection."))))]
     (reify ElementManager
       (^Iterable getElems [_ ^Class cl] (get elems cl))
       (^APersistentMap getMom [_] mom)
      
       ;; If o is map then value of key "property" is returned 
       ;; otherwise value is got from bean of the object o.
       (^Object getPropertyValue [this ^Object o, ^String property]
          (let [b (if (map? o) o (bean o))]
            ((keyword property) b)))))))


(defn ^ElementManager -createMCElementManager
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


(defn rfilter
  "Filters results due to a specified collection.
  Returns new result which is not contain elements
  which are not into the collection. Collection must 
  be set because of contains? function is used."
  [result coll]
  (vec (reduce (fn [r [f s]] 
                 (if (contains? coll f) 
                   (concat r [f (if (empty? s) s (rfilter s coll))])
                   r))
               []
               (partition 2 result))))


(defn collq
  "Simple interface for quering. User should define just
  query and collection with objects. Parameters:
    q - YZ's query.
    coll - collection with objects.
    args - arguments where
      :clazz key must be specify class of collection. 
        If clazz is not supplied then MOM will be nil.
      :rtype key is type of result (:rows or :result - flat or hierarchical 
        representation of the result respectively). All structure is returned by default.
      :verbose key specify whether result includes elements which is
        not contained into the coll.
      :mom key defines Map Of Object Model. If it is not specified then
        mom is generated from value of :clazz key. If MOM is specified
        then :clazz key is ignored.

  Examples:
    (collq \"string\" [1 2 \"1\" 3])
      => ([\"1\"])
    (collq \"long\" [1 2 \"1\" 3])
      => ([1] [2] [3])
    (collq \"string\" [1 2 \"1\" 3] :rtype :result)
      => [\"1\" []]
    (collq \"string\" [1 2 \"1\" 3] :rtype :result :clazz String)
      => [\"1\" []]
    (collq \"string\" [1 2 \"1\" 3] :clazz String)
      => ([\"1\"])"
  [^String q coll & args]
  (let [parts (partition 2 args)
        {:keys [rtype clazz verbose mom]} (zipmap (map first parts) (map second parts))
        [cls mom] (if mom 
                    [(filter class? (keys mom)) mom]
                    (let [clazz (if (or (nil? clazz) (coll? clazz)) clazz [clazz])]
                      ; if clazz is nil then mom will be nil.
                      (if clazz [clazz (mu/gen-mom clazz nil)] [nil nil]))) 
        em (c-em coll cls mom) ; define element manager
        r (yz/pquery q mom em)]
    (if (:error r) 
      (throw (:thrwable r))
      (if verbose
        (if rtype (rtype r) r)
        (if (or (nil? rtype) (= rtype :result) (= rtype :rows))
          (let [result (rfilter (:result r) (set coll))]
            (case rtype
              :result result
              :rows (yz/get-rows result)
              nil (assoc r :result result :rows (yz/get-rows result))))
          (rtype r))))))


(comment
  (collq "string" [1 2 "1" 3])
  (collq "long" [1 2 "1" 3])
  (collq "string" [1 2 "1" 3] :rtype :result)
  (collq "string" [1 2 "1" 3] :rtype :result :clazz String)
  (collq "string" [1 2 "1" 3] :clazz String)
)
