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


(ns ru.petrsu.nest.yz.core
  ^{:author "Vyacheslav Dimitrov"
    :doc "This code contains core functions of the Clojure's implementation of the YZ language.

         The Parsing of queries does due to the fnparse library.
         See the code for the parsing queries in the parsing.clj file.

         Criteria API 2.0 is used as API for access to a storage."}
  (:require [ru.petrsu.nest.yz.parsing :as p] [clojure.string :as cs])
  (:use ru.petrsu.nest.yz.functions)
  (:import (javax.persistence.criteria 
             CriteriaQuery CriteriaBuilder Predicate Root)
           (javax.persistence EntityManager)
           (clojure.lang PersistentArrayMap PersistentVector Keyword)))


(definterface ElementManager
  ^{:doc "This interface is needed for abstraction of any persistence storage. 
         You should implement getElements which takes specified class ('claz')
         and returns collection of objects which have this class.
         The YZ implements the next algorithm 
         (let's consider query: \"building (device#(forwarding=true))\"):
            1. Gets root elements (building) due to the implementation of this interface.
            2. Finds elements (device) which are connected with root elements (building) due to the MOM.
            3. Executes restrictions.
         
         getClasses returns collection of classes which are entities of the model.
         This method is needed for generating the MOM (Map Of the Model).
         
         Also you can implement getById for more performance access to object by specified id. This
         function is called in case a query contains structure like this: ...#(id=2)."}
  (^java.util.Collection getElements [^Class claz])
  (^java.util.Collection getClasses [])
  (^Object getById [^Object id]))

(declare em, mom)

(defn ^String tr-pred
  "Transforms 'pred' map into string"
  [pred]
  (let [v (:value pred)
        v (if (nil? v) "nil" v)]
    (str "(ru.petrsu.nest.yz.core/process-preds o, " (:ids pred) 
         ", " (:func pred) ", " v ")")))

(defn- contains-f?
  "Checks whether vector with predicates contains 
  function as a value of the key :value."
  [^PersistentArrayMap pred]
  (some #(or (map? (:value %)) (map? (:ids %))) pred))

(defn- get-path
  "Returns Path for specified vector with names of properties and
  the root element."
  [^Root root, ids]
  (reduce #(try (.join %1 %2)
             (catch Exception e (.get %1 %2))) root ids))


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
        v (if (and (instance? String v) (= \" (nth v 0))) (subs v 1 (- (count v) 1)) v)]
    (cond (and (= "=" op) (nil? v)) (.isNull cb path)
          (= "=" op) (.equal cb path v)
          (= ">" op) (.gt cb path (Double/parseDouble v))
          (= "<" op) (.lt cb path (Double/parseDouble v))
          (= ">=" op) (.ge cb path (Double/parseDouble v))
          (= "<=" op) (.le cb path (Double/parseDouble v))
          (and (= "not=" op) (nil? v)) (.isNotNull cb path)
          (= "not=" op) (.notEqual cb path v)
          :else (throw (Exception. (str "No find function " op))))))


(defn- ^Predicate create-predicate
  "Takes stack with definition of restrictions 
  and CriteriaBuilding's instance. Returns Predicate."
  [^PersistentVector preds, ^CriteriaBuilder cb, ^Root root]
  ((reduce #(cond (map? %2) (conj %1 (get-op %2 cb root))
                  (= :and %2) (complex-predicate %1 .and cb)
                  (= :or %2) (complex-predicate %1 .or cb)
                  :else %2) [] preds) 0))


(declare filter-by-preds, create-string-from-preds)
(defn- select-elems
  "Returns from storage objects which have 'cl' class."
  [^Class cl, ^PersistentVector preds]
  (if (instance? EntityManager em)
        (let [^CriteriaBuilder cb (.getCriteriaBuilder em)
              cr (.createTupleQuery cb)
              ^Root root (. cr (from cl))
              cr (.. cr (multiselect [root]) (distinct true))
              ch-p (contains-f? preds) ; ch-p defines whether "preds" contains function.
              elems (map #(.get % 0) 
                         (.. em (createQuery (if (or (nil? preds) ch-p)
                                                 cr 
                                                 (.where cr (create-predicate preds cb root))))
                           getResultList))]
          (if ch-p
            (filter-by-preds elems (create-string-from-preds preds))
            elems))
        (try 
          (if (and (= (count preds) 1) (= ((:ids (preds 0)) 0) "id") (= (:func (preds 0)) "="))
            (.getById em (:value (preds 0)))
            (filter-by-preds (.getElements em cl) (create-string-from-preds preds)))
          (catch IllegalArgumentException e 
            (throw (IllegalArgumentException. 
                     "Don't recognize ElementManager. Do you implement getElements method?"))))))


(defn get-fv
  "Returns value of field. First we try to find property
  due to bean function, if is failed then we try to using
  reflection (e.g. getDeclaredField)."
  [o, ^Keyword field-name]
  (if (nil? o)
    nil
    (let [v (get (bean o) field-name :not-found)]
      (cond 
        ; If value is nil then function return nil.
        (nil? v) v

        ; If value not found into bean map then we try find this value due to java reflection.
        (= v :not-found)
        (let [field-name (name field-name)]
          (loop [^Class cl (class o)]
            (cond (nil? cl) (throw (Exception. (str "Not found property: " field-name)))
                  (contains? (set (map #(.getName %) (.getDeclaredFields cl))) field-name)
                  (.get (doto (.getDeclaredField cl field-name) (.setAccessible true)) o)
                  :else (recur (:superclass (bean cl))))))

        ; If value is an array then we check whether a type of the array from the MOM, If true then
        ; we returns a collection from this array.
        (and (.isArray (class v)) (get mom (.getComponentType (class v)))) (map identity v)

        ; Returns value.
        :else v))))


(declare process-nests, get-rows, run-query)
(defn- process-func
  "Gets :function map of q-representation 
  and returns value of evaluation of one."
  [f-map, obj]
  (let [params (map #(cond 
                           ; param is result of a query.
                           (vector? %)
                           (let [[fmod q] %
                                 rq (if (or (= fmod :indep) (nil? obj))
                                        (run-query q)
                                        (process-nests q obj))]
                             (cond (= fmod :single) {:mode :single 
                                                     :res (map (fn [p] (get-rows [p])) 
                                                               (mapcat (fn [r] (map vec (partition 2 r))) rq))}
                                   :else (get-rows rq)))

                           ; param is another function.
                           (map? %) (process-func % obj)

                           ; param is self object
                           (= "&" %) obj 

                           ; param is value of the default property.
                           (= "&." %) (get-fv obj (keyword (:dp (get mom (class obj)))))

                           ; param is value of some property of the object.
                           (and (instance? String %) (.startsWith % "&."))  (get-fv obj (keyword (.substring % 2)))

                           ; param is string or number.
                           :else %) 
                    (:params f-map))
        lparams (reduce #(if (and (map? %2) (= (:mode %2) :single)) 
                           (vec (mapcat (fn [lp] (map (fn [o] (conj lp o)) (:res %2))) %1))
                           (vec (map (fn [lp] (conj lp %2)) %1))) [[]] params)]
    (map #(apply (:func f-map) %) lparams)))


(defn- get-objs
  "Returns sequence of objects which are belonged to 'objs' 
  by specified 'field-name'"
  [^String field-name, objs]
  (flatten
    (map (fn [o] 
           (if-let [fv (get-fv o (keyword field-name))]
             (if (instance? java.util.Collection fv)
               (map identity fv)
               fv)))
           objs)))


(defn- eq-arrays?
  "Returns true if array a1 equals arrya a2."
  [a1 a2]
  (if (or (nil? a1) (nil? a2))
    nil
    (java.util.Arrays/equals a1 a2)))


(defn process-preds
  "Processes restrictions."
  [o, l-side, f, value]
  (let [objs (cond (vector? l-side) (reduce #(get-objs %2 %1) [o] l-side)
                   (map? l-side) (process-func l-side o))

        ;; If objects from objs are arrays then we must compare two arrays.
        f (let [cl (if (empty? objs) nil (class (nth objs 0)))]
           (if (and (not (nil? cl)) (.isArray cl)) eq-arrays? f))]
    (if (map? value)
      (some #(f (% 0) (% 1)) (for [obj objs, v (process-func value o)] [obj v]))
      (some #(f % value) objs))))


(defn- ^String create-string-from-preds
  "Creates string from preds vector for 
  checking object due to restriction"
  [^PersistentVector preds]
  (if (nil? preds)
    nil
    (str "#=(eval (fn [o] " 
         ((reduce #(cond (keyword? %2) (conj (pop (pop %1)) 
                                             (str " (" (name %2) " " (peek %1) " " (peek (pop %1)) " )") )
                  :else (conj %1 (tr-pred %2)))
                  [] 
                  preds) 0) "))")))


(defn- filter-by-preds
  "Gets sequence of objects and string of restrictions and
  returns new sequence of objects which are filtered by specified preds."
  [objs, ^String preds]
  (if (nil? preds) 
    objs 
    (let [f (read-string preds)]
      (filter #(f %) objs))))


(defn- get-objs-by-path
  "Returns sequence of objects which has cl-target's class and are
  belonged to 'sources' objects."
  [sources ^String preds paths]
  (mapcat #(loop [ps % res sources]
             (if (empty? ps)
               (filter-by-preds res preds)
               (recur (rest ps) (get-objs (first ps) res))))
          paths))


(defn- process-prop
  "Processes property."
  [[prop ^Boolean is-recur] obj]
  (cond (map? prop) (process-func prop, obj)
        (= prop :self-object) obj
        is-recur (loop [res [] obj- (get-fv obj prop)]
                   (if (nil? obj-)
                     res
                     (recur (conj res obj-) (get-fv obj- prop))))
        :else (get-fv obj prop)))


(defn- process-props
  "If nest has props then function returns value of property(ies),
  otherwise obj is returned."
  [obj, props]
  (if (empty? props)
    obj
    (map #(process-prop % obj) props)))


(defn process-then
  "Processes :then value of query structure.
  Returns sequence of objects."
  [then, objs, props, cl]
  (loop [then- then, objs- objs, props- props]
    (if (or (nil? then-) (every? nil? objs-))
      (map (fn [o] [o, (process-props o props-)]) objs-)
      (recur (:then then-) 
             (get-objs-by-path objs- (create-string-from-preds (:preds then-)) (:where then-))
             (:props then-)))))


(declare process-nests)
(defmacro p-nest
  "Generates code for process :nest value with some objects."
  [^PersistentArrayMap nest, objs]
  `(reduce #(conj %1 (%2 1) (process-nests (:nest ~nest) (%2 0)))
          []
          (process-then (:then ~nest) ~objs (:props ~nest) (:what ~nest))))


(defn- process-nest
  "Processes one element from vector from :nest value of query structure."
  [^PersistentArrayMap nest, objs]
  (p-nest nest (get-objs-by-path 
                 objs  
                 (create-string-from-preds (:preds nest)) 
                 (:where nest))))


(defn- process-nests
  "Processes :nest value of query structure"
  [nests obj]
  (vec (map #(process-nest % [obj]) nests)))


(defn- do-query
  "Gets structure of query getting from parser and returns
  structure of user's result."
  [q]
  (p-nest q (select-elems (:what q) (:preds q))))


(defn- run-query
  "Returns result of 'query' based on specified map of object model ('mom')
  and instance of some ElementManager ('em')."
  [parse-res]
  (vec (map #(do-query %) parse-res)))


(defn- get-column-name
  "Returns the string representation of column 
  for the specified nest from result of query."
  [^PersistentArrayMap nest]
  (let [then (:then nest)
        ^Class what (:what nest)
        props (:props nest)
        pprops (fn [props ^Class parent] 
                 (map #(let [v (% 0)]
                         (if (= v \&)
                           (.getSimpleName parent)
                           v))
                         props))]
    (cond  
      (not (nil? then)) (loop [then- then]
                          (if (nil? (:then then-))
                            (if (nil? (:props then-))
                              (.getSimpleName (:what then-))
                              (pprops (:props then-) (:what then-)))
                            (recur (:then then-))))
      (not (empty? props)) (pprops props what)
      :else (.getSimpleName what))))


(defn get-columns
  "Returns vector with columns names 
  for the specified result of parsing some query."
  [parse-res]
  (loop [p parse-res res []]
    (if (every? nil? p)
      (let [res (loop [f (first res) res- res r (rest res)] 
                  (if (nil? f)
                    res-
                    (recur (first r) 
                           (if (sequential? f) 
                             (reduce inc res- f)
                             res-) 
                           (rest r))))]
      (map #(reduce (fn [sv n] (str sv ", " n) ) %) res))
      (recur (flatten (map #(:nest %) p)) 
               (conj res (map #(get-column-name %) p))))))


(defn- get-columns-lite
  "Takes rows with result of query and returns vector with columns name.
  Doesn't use complex algorithm for getting column's names."
  [rows]
  (if (empty? rows)
    ()
    (let [m (reduce #(max %1 (count %2)) 0 rows)
          row (some #(if (= (count %) m) %) rows)]
      (map #(if (nil? %) "" (.getSimpleName (class %))) row))))


(defn get-rows
  "Returns set of rows. The 'data' is the result of 
  processing a query."
  ([data]
     (get-rows data ()))
  ([data & args]
   (if (empty? (data 0))
     (if (empty? (nth args 0))
       ()
       (list (vec (flatten args))))
     (mapcat (fn [o]
               (cond (empty? o) [nil]
                     (empty? (o 1)) (for [pair (partition 2 o)] (vec (flatten [args pair])))
                     :else (mapcat #(if (empty? %) [] (get-rows (nth % 1) args (nth % 0))) (partition 2 o))))
             data))))


(defn- def-result
  "Returns map of the reusult executing 'pquery'."
  [result, error, columns rows]
  {:result result
   :error error
   :columns columns
   :rows rows})


(defn pquery
  "Returns map where
    :error - defines message of an error 
            (:error is nil if nothing errors is occured)
    :result - a result of a query
    :columns - vector with column's names.
    :rows - rows of the result of a query."
  [^String query ^PersistentArrayMap mom em]
  (do (def mom mom)
    (def em em)
    (if (empty? query)
      (def-result [[]] nil [] ())
      (let [parse-res (try
                        (p/parse query mom)
                        (catch Throwable e (.getMessage e)))
            run-query-res (cond (string? parse-res) parse-res
                                (map? parse-res) (try
                                                   (let [pc (process-func parse-res nil)]
                                                     [pc (reduce #(cons [%2] %1) () pc)])
                                                   (catch Throwable e (.getMessage e)))
                                :else (try
                                        (let [rq (run-query parse-res)]
                                          [rq (get-rows rq)])
                                        (catch Throwable e (.getMessage e))))]
        (if (string? run-query-res)
          (def-result [] run-query-res [] ())
          (def-result (run-query-res 0) nil (get-columns-lite (run-query-res 1)) (run-query-res 1)))))))

