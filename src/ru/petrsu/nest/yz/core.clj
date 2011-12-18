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
    :doc "This code contains core functions of Clojure's implementation of the YZ language.

         The Parsing of queries does due to the fnparse library.
         See the code for the parsing queries in the parsing.clj file.
         
         You must have some implementation of the ElementManager interface (see below) and
         pass it to the pquery function."}
  (:require [ru.petrsu.nest.yz.parsing :as p] [clojure.string :as cs])
  (:import (clojure.lang PersistentArrayMap PersistentVector Keyword)))


(definterface ElementManager
  ^{:doc "This interface is needed for abstraction of any persistence storage. 
         You must implement getElems which takes specified class ('claz')
         and returns collection of objects which have this class.
         The YZ implements the next algorithm 
         (let's consider query: \"building (device#(forwarding=true))\"):
            1. Gets root elements (building) due to the implementation of this interface.
            2. Finds elements (device) which are connected with root elements (building) due to the MOM.
            3. Filters device due to restrictions.
         
         getClasses returns collection of classes which are entities of the model.
         This method is needed for generating the MOM (Map Of the Model).
         
         getPropertyValue takes an object and name of a property and returns
         a value of this property."}
  (^java.util.Collection getElems [^Class claz])
  (^java.util.Collection getClasses [])
  (^Object getPropertyValue [^Object o, ^String property]))


(defprotocol ExtendedElementManager
  ^{:doc "Extends ElementManager. 
         
         Method getElems takes addition parameter in comparison with getElems 
         from ElementManager. The parameter is vector with predicates for 
         first set of objects which is selected. For example, for JPA's storage
         it may be more better filters objects using DataBase engine, than
         gets all objects and then filters it manual. For more details see
         implementation ElementManager for JPA's storage in the yz-factory.clj file.
         So If you implement ExtendedElementManager you must filter first set of 
         objects due to preds parameter."}
  (getElems [this claz preds]))


(def ^{:dynamic true
       :tag ElementManager} *em*)
(def ^{:dynamic true} *mom*)


(declare process-preds, process-prop process-func)
(defn- pp-func
  "Defines whether specified object o satisfies to specified
  vector with predicates preds."
  [o, ^PersistentVector preds]
  (let [ff (first 
             (reduce #(if (map? %2) 
                        (conj %1 %2)
                        (let [fa (peek %1)
                              sa (peek (pop %1))
                              op (if (= %2 :and) 
                                   (and (if (map? fa) (process-preds o fa) fa) 
                                        (if (map? sa) (process-preds o sa) sa))
                                   (or (if (map? fa) (process-preds o fa) fa) 
                                        (if (map? sa) (process-preds o sa) sa)))]
                          (conj (pop (pop %1)) op))) [] preds))]
    (if (map? ff)
      (process-preds o ff)
      ff)))


(defn filter-by-preds
  "Gets sequence of objects and vector with predicates and
  returns new sequence of objects which are filtered by this predicates."
  [objs, preds]
  (if (nil? preds) 
    objs
    (let [; Attempt to improve performace. If parameters of some function
          ; doesn't contain queries then we get result of function and use
          ; it as parameter. So we avoid calling function (which does not
          ; depend from query) for each objects from objs.
          preds 
          (reduce #(conj %1 
                         (let [params (:params (:value %2))]
                           (if (and params (not (some vector? params)))
                             (assoc %2 :value (nth (process-func (:value %2) nil) 0))
                             %2))) [] preds)] 
      (filter #(pp-func % preds) objs))))


(defn- sort-rq
  "Sorts results of query due to specified type of sorting
  and its comparator and keyfn (vector vsort). prop? defines
  whether rq is sequence of sequences from properties."
  [rq vsort prop?]
  (if (nil? vsort)
    rq
    (let [;; Functions for getting comparator.
          get-comp #(let [tcomp (if %1 %1 compare)]
                      (cond (nil? %2) nil
                            (= %2 :asc) tcomp
                            (= %2 :desc) (fn [v1, v2] (* -1 (tcomp v1 v2)))))

          get-gcomp 
          (fn [f vs]
            (fn [v1 v2] 
              (let [r (some 
                        #(let [[tsort tcomp keyfn] (nth vs (f %))
                                tcomp (get-comp tcomp tsort)
                                c (cond (nil? tsort) 0
                                        (and tcomp keyfn) (tcomp (keyfn (nth v1 %)) (keyfn (nth v2 %)))
                                        tsort (tcomp (nth v1 %) (nth v2 %)))]
                           (if (= c 0) nil c)) 
                        (range 0 (min (count v1) (count v2))))]
                (if (nil? r) 0 r))))

          ;; Function for sorting.
          s #(let [tcomp (get-comp %2 %1)]
               (cond 
                 (and %3 tcomp) (sort-by %3 tcomp %4)
                 %1 (sort tcomp %4)
                 :else %4))]
      (cond
        ; Yet another hack for missing sorting result of the 
        ; following queries {a:name}building[name]. 
        (and (every? list? vsort) (every? vector? rq)) rq

        ; Sort by properties which are not selected.
        (every? list? vsort) 
        (let [; This is needed for right order of props and vsort.
              props-sorts (reduce (fn [r l] (cons [[(first l) false] (second l)] r)) [] vsort)
              props (reduce #(conj %1 (%2 0)) [] props-sorts) 
              vsort (reduce #(conj %1 (%2 1)) [] props-sorts) 
              rq (map (fn [o] [o (map #(let [[p _] %]
                                         (if (= p :self)
                                           o
                                           (process-prop %, o))) props)]) rq)]
          (map first (sort-by #(% 1) (get-gcomp identity vsort) rq)))

        (and prop? (every? vector? vsort)) 
        (let [[tsort tcomp keyfn] (first vsort) 
              keyfn (if keyfn #(keyfn (% 0)) #(% 0)) 
              ; First we sort object of class which is selected.
              rq (s tsort tcomp keyfn rq)]
          ; then we sort its vector with properties.
          (sort-by #(% 1) (get-gcomp inc vsort) rq))

        ; It is needed for missing sorting objects which are selected
        ; with properties. This sorting is made in previous test of this cond.
        (every? vector? vsort) rq

        :else 
        (let [[tsort tcomp keyfn] vsort]
          (s tsort tcomp keyfn rq))))))


(defn- select-elems
  "Returns from storage objects which have ':what' class from nest."
  [nest]
  (let [{:keys [^Class what ^PersistentVector preds sort exactly]} nest]
    (if (instance? ru.petrsu.nest.yz.core.ExtendedElementManager *em*)
      (sort-rq (.getElems *em* what preds) sort false)
      (let [elems (.getElems *em* what)
            elems (if exactly (filter #(= (class %) what) elems) elems)]
        (sort-rq (filter-by-preds elems preds) sort false)))))


(defn- get-fv
  "Returns value of field. First we try to finding property
  due to getPropertyValue function of a ElementManager, 
  if is failed then we try to using reflection (e.g. getDeclaredField)."
  [o, field-name]
  (if (nil? o)
    nil
    (let [field-name (if (keyword? field-name) (name field-name) field-name) 
          v (try (.getPropertyValue *em* o field-name)
              (catch Exception e (throw (Exception. (str "Not found property: " field-name)))))]
      (cond 
        ; If value is nil then function returns nil.
        (nil? v) nil

        ; If value not found into bean map then we try find this value due to java reflection.
        (= v :not-found)
        (loop [^Class cl (class o)]
          (cond (nil? cl) (throw (Exception. (str "Not found property: " field-name)))
                (contains? (set (map #(.getName %) (.getDeclaredFields cl))) field-name)
                (.get (doto (.getDeclaredField cl field-name) (.setAccessible true)) o)
                :else (recur (:superclass (bean cl)))))

        ; If value is an array then we check whether a type of the array from the MOM, If true then
        ; we returns a collection from this array.
        (and (.isArray (class v)) (get *mom* (.getComponentType (class v)))) (seq v)

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
                             (if (= fmod :single) 
                               {:mode :single 
                                :res (map (fn [p] (get-rows [p])) 
                                          (mapcat (fn [r] (map vec (partition 2 r))) rq))}
                               (get-rows rq)))

                           ; param is another function.
                           (map? %) (process-func % obj)

                           ; param is self object
                           (= "&" %) obj 

                           ; param is value of the default property.
                           (= "&." %) (get-fv obj (:dp (get *mom* (class obj))))

                           ; param is value of some property of the object.
                           (and (string? %) (.startsWith % "&.")) (get-fv obj (.substring % 2))

                           ; param is string, number or some keyword (true, false, nil).
                           :else %) 
                    (:params f-map))
        lparams (reduce #(if (= (:mode %2) :single)
                           (vec (mapcat (fn [lp] (map (fn [o] (conj lp o)) (:res %2))) %1))
                           (vec (map (fn [lp] (conj lp %2)) %1))) [[]] params)]
    (map #(apply (:func f-map) %) lparams)))


(defn- get-objs
  "Returns sequence of objects which belong to 'objs' 
  by specified 'field-name'."
  [^String field-name, objs]
  (flatten
    (map #(if-let [fv (get-fv % field-name)]
           (if (instance? java.util.Collection fv)
             (seq fv)
             fv))
           objs)))


(defn- eq-arrays?
  "Returns true if array a1 equals arrya a2."
  [a1 a2]
  (if (or (nil? a1) (nil? a2))
    nil
    (java.util.Arrays/equals a1 a2)))


(defn- process-preds
  "Processes restrictions."
  [o, pred] 
  (let [{:keys [ids func value]} pred
        objs (cond (vector? ids) 
                   (reduce #(let [objs- (reduce 
                                          (fn [objs-, field-name] 
                                            (get-objs field-name objs-)) 
                                          %1
                                          (:id %2))]
                              (if (nil? (:cl %2))
                                objs-
                                (filter (fn [obj] (instance? (:cl %2) obj)) objs-)))
                           [o]
                           ids)
                   (map? ids) (process-func ids o))

        ;; If objects from objs are arrays then we must compare two arrays.
        func (let [cl (if (empty? objs) nil (class (nth objs 0)))]
               (if (and cl (.isArray cl)) eq-arrays? func))
        ;; Check function for a regular expression
        func (if (= func #'clojure.core/re-find) 
               (fn [o value] 
                 (if (or (nil? value) (nil? o)) ; Prevent NullPointerException.
                   nil
                   (re-find (re-pattern value) o)))
               func)
        func #(try (func %1 %2)
                ; If exception is caused then value is returned as nil.
                (catch Exception e nil))]
    (if (map? value)
        (some #(func (% 0) (% 1)) (for [obj objs, v (process-func value o)] [obj v]))
        (some #(func % value) objs))))


(defn- get-objs-by-path
  "Returns sequence of objects which has cl-target's 
  (value of the :what key from m) class and are belonged to 'sources' objects."
  [sources m]
  (let [{:keys [preds where ^Class what sort exactly]} m
        f (if exactly #(= (class %) what) #(instance? what %))
        path (first where)] ; At this moment we use first path.
    (sort-rq (filter-by-preds 
               (filter f (reduce #(get-objs %2 %1) sources path)) preds)
             sort false)))


(defn- process-prop
  "Processes property."
  [[prop ^Boolean is-recur] obj]
  (cond (map? prop) 
        (let [fr (process-func prop, obj)]
          (if (and (seq? fr) (= (count fr) 1))
            (first fr)
            fr))
        (= prop :#self-object#) obj
        (= prop :#default-property#) (get-fv obj (:dp (get *mom* (class obj))))
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


(defn- process-then
  "Processes :then value of query structure.
  Returns sequence of objects."
  [objs, then, props, tsort]
  (loop [objs- objs, then- then, props- props, tsort tsort]
    (if (or (nil? then-) (every? nil? objs-))
      (let [pp (map (fn [o] [o, (process-props o props-)]) objs-)]
        (if (and (not-empty pp) (= ((first pp) 0) ((first pp) 1)))
          pp
          (sort-rq pp tsort true)))
      (recur (get-objs-by-path objs- then-)
             (:then then-) 
             (:props then-)
             (:sort then-)))))


(declare process-nests)
(defn- p-nest
  "Processes :nest value with some objects."
  [^PersistentArrayMap nest, objs]
  (reduce #(conj %1 (%2 1) (process-nests (:nest nest) (%2 0)))
          []
          (process-then objs (:then nest) (:props nest) (:sort nest))))


(defn- process-nests
  "Processes :nest value of query structure"
  [nests obj]
  (vec (map #(p-nest % (get-objs-by-path [obj] %)) nests)))


(defn- run-query
  "Returns result of 'query' based on specified map of object model ('mom')
  and instance of some ElementManager ('em')."
  [parse-res]
  (vec (map #(if (nil? (get % :func)) 
               (p-nest % (select-elems %))
               (reduce (fn [r rf] (vec (concat r [rf []]))) [] (process-func % nil)))
            parse-res)))


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
      then (loop [then- then]
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


(defrecord Result 
  ^{:doc "Map with the result of executing 'pquery'."}
  [result error columns rows])


(def
  ^{:doc "The memoized version of the parse function from the parsing.clj"}
  mparse (memoize p/parse))


(defn pquery
  "Returns map where
    :error - defines message of an error 
            (:error is nil if any error is not occured)
    :result - a result of a query
    :columns - vector with column's names.
    :rows - rows of the result of a query."
  [^String query ^PersistentArrayMap mom ^ElementManager em]
  (binding [*mom* mom
            *em* em]
    (if (empty? query)
      (Result. [[]] nil [] ())
      (let [parse-res (try
                        (p/parse query *mom*)
                        (catch Throwable e (.getMessage e)))
            parse-res (if (nil? parse-res) "Result of parsing is nil." parse-res)
            query-res (if (string? parse-res)
                        parse-res
                        (try
                          (run-query parse-res)
                          (catch Throwable e (.getMessage e))))]
        (if (string? query-res)
          (Result. [] query-res [] ())
          (let [rows (distinct (get-rows query-res))]
            (Result. query-res nil (get-columns-lite rows) rows)))))))

