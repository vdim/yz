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


(ns ru.petrsu.nest.yz.core
  ^{:author "Vyacheslav Dimitrov"
    :doc "This code contains core functions of Clojure's implementation of the YZ language.

         The parsing of queries does due to the fnparse library.
         See code for parsing queries in the parsing.clj file.
         
         In order to use YZ you must have some implementation 
         of the ElementManager interface (see below) and pass it to the pquery function."}
  (:require [ru.petrsu.nest.yz.parsing :as p])
  (:import (clojure.lang PersistentArrayMap PersistentVector)))


(definterface ElementManager
  ^{:doc "This interface is needed for abstraction of any persistence storage. 

         You must implement getElems which takes specified class ('claz')
         and returns collection of objects which have this class.
         The YZ implements the next algorithm 
         (let's consider query: \"building (device#(forwarding=true))\"):
            1. Gets root elements (building) due to the implementation of this getElems.
            2. Finds elements (device) which are linked with root 
               elements (building) due to path from the MOM.
            3. Filters device due to restrictions.
         
         getMom returns the MOM (Map Of the Model).
         
         getPropertyValue takes an object and name of a property and returns
         a value of this property."}
  (^java.util.Collection getElems [^Class claz])
  (^clojure.lang.APersistentMap getMom [])
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


(defrecord Result 
  ^{:doc "Map with the result of executing 'pquery'.
            :result - hierarchical structure of result query.
            :error - if error is occured then value of this keyword contains 
                     string representation of the error. If not then value is nil.
            :thrwable - if error is occured then value of this keyword is
                     Throwable object.
            :columns - list with default names of columns.
            :rows - representation of a result query as set of rows
                    (similarly as table from relation database terminology.)"}
  [result error thrwable columns rows])


(def ^{:tag ElementManager} a-em (atom nil))
(def a-mom (atom nil))


(declare process-preds process-prop process-func get-objs get-qp get-qr process-nests get-objs-by-path)
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
                             %2))) [] preds)

          ; Memoized version of the get-objs function.
          m-go (memoize (fn [ids o]
                          (cond (vector? ids) 
                              (reduce (fn [r {:keys [id cl]}]
                                        (let [objs- (reduce #(get-objs %2 %1) r id)]
                                          (if (nil? cl)
                                            objs-
                                            (filter (partial instance? cl) objs-))))
                                      [o]
                                      ids)
                              (map? ids) (process-func ids o))))

          ; Define function for checking objects.
          [f]
          (reduce (fn [s p]
                    (if (map? p) ; some predicate (another case is :or or :and binary operation.
                      (let [{:keys [all ids func value]} p
                            [allA not-any value] 

                            ; if value is vector so value is described 
                            ; subquery: building#(name=floor.name)
                            (if (vector? value) 
                              (let [[allA not-any rp] value] ; rp - result of parsing subquery.
                                  [allA not-any
                                   (if allA
                                     ; This set doesn't depend on object.
                                     (-> (get-qr rp @a-mom @a-em) :rows flatten set)
                                     rp)])

                              ; If value is keyword then we must get value 
                              ; from list of parameters of query.
                              [true false (if (keyword? value) (get-qp value) value)])]
                        
                        (conj s #(let [v (if allA
                                           value
                                           ; This set depends on object.
                                           (set (flatten (process-nests value (partial get-objs-by-path [%2])))))]
                                  (process-preds %1 %2 all ids func v not-any))))

                      ; :or or :and binary operation.
                      (conj (pop (pop s)) 
                            (if (= p :and)
                              #(and ((peek s) %1 %2) ((peek (pop s)) %1 %2))
                              #(or ((peek s) %1 %2) ((peek (pop s)) %1 %2))))))
                  []  preds)]
      (filter #(f m-go %) objs))))


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


(defn- limiting
  "Filters collection of elements (elems) due to vector of
  limit: [lower-bound, higher-bound, tail]. 
  tail defines whether we must get last elements."
  [elems limit]
  (if limit (let [[l h tail] limit
                  els (cond ;1:floor
                            (and (not l) tail) (take (- (count elems) h) elems)
                            ;-1:floor
                            (not l) (nthrest elems h)
                            ;-2-4:floor
                            tail (let [e (nthrest elems (- (count elems) (inc h)))] 
                                   (take (- (count e) l) e))
                            ;2-4:floor
                            :else (nthrest (take (inc h) elems) l))]
              els)
    elems))


(defn- select-elems
  "Returns from storage objects which have 
  ':what' class from the nest map."
  [nest]
  (let [^ElementManager em @a-em
        {:keys [^Class what ^PersistentVector preds sort exactly unique limit]} nest]
    (if (instance? ru.petrsu.nest.yz.core.ExtendedElementManager em)
      (sort-rq (.getElems em what preds) sort false)
      (let [; Get elements
            elems (.getElems em what)
            ; Filter by exactly option.
            elems (if exactly (filter #(= (class %) what) elems) elems)
            ; Filter by unique option.
            elems (if unique (distinct elems) elems) 
            ; Filter by list of predicates and then sort.
            elems (sort-rq (filter-by-preds elems preds) sort false)
            ; Filter by limit option.
            elems (limiting elems limit)]
        elems))))


(defn- get-fv
  "Returns value of field. First we try to finding property
  due to getPropertyValue function of a ElementManager, 
  if is failed then we try to using reflection (e.g. getDeclaredField)."
  [^Object o, field-name]
  (if (nil? o)
    nil
    (let [^String field-name (if (keyword? field-name) (name field-name) field-name) 
          v (if (= "&" field-name) ; supported self objects into predicates.
              o 
              (try (.getPropertyValue @a-em o field-name)
                (catch Exception e :not-found)))]
      (cond 
        ; If value is nil then function returns nil.
        (nil? v) nil

        ; If value is an array then we check whether a type of the array from the MOM, If true then
        ; we returns a collection from this array.
        (and (.isArray (class v)) (get @a-mom (.getComponentType (class v)))) (seq v)

        ; Returns value.
        :else v))))


(defn- get-qp
  "Returns value from p/query-params for 
  specified number (has string type)."
  [n]
  (nth @p/query-params (dec (Integer/parseInt (name n)))))


(declare get-rows)
(defn- process-func
  "Gets :function map of q-representation 
  and returns value of evaluation of one."
  [f-map, obj]
  (let [params (map #(cond
                           ; params is parameter: @(str $1)
                           (keyword? %) (let [qp (get-qp %)]
                                          (if (instance? Result qp) (:rows qp) qp))
                       
                           ; param is result of a query.
                           (vector? %)
                           (let [[fmod q] %
                                 q (if (keyword? q) (get-qp q) q)
                                 rq (cond (instance? Result q) q
                                          (or (= fmod :indep-list) (= fmod :indep-each) (nil? obj))
                                          (process-nests q select-elems)
                                          :else (process-nests q (partial get-objs-by-path [obj])))]
                             (cond (instance? Result q) (:rows q)
                                   (or (= fmod :indep-each) (= fmod :dep-each))
                                   {:mode :single 
                                    :res (map get-rows (map vec (partition 2 rq)))}
                                   :else (get-rows rq)))

                           ; param is another function.
                           (map? %) (process-func % obj)

                           ; param is self object
                           (= "&" %) obj 

                           ; param is value of the default property.
                           (= "&." %) (get-fv obj (p/get-dp (class obj) @a-mom ))

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
  (flatten (map #(let [fv (get-fv % field-name)]
                   ; DON'T REMOVE THIS IF.
                   ; At least sets are not flattened.
                   (if (instance? java.util.Collection fv)
                     (seq fv)
                     fv))
                objs)))


(defn- eq-arrays?
  "Returns true if array a1 equals array a2."
  [a1 a2]
  (if (or (nil? a1) (nil? a2))
    nil
    (java.util.Arrays/equals a1 a2)))


(defn process-preds
  "Processes restrictions."
  [m-go, o, all ids func value not-any]
  (let [objs (m-go ids o)]
    (and (seq objs)
         (let [;; If objects from objs are arrays then we must compare two arrays.
               func (let [cl (class (nth objs 0))]
                      (if (and cl (.isArray cl)) eq-arrays? func))

               ;; Wrap calling function to try-catch block.
               func #(try (func %1 %2)
                       ; If exception is caused then value is returned as nil.
                       (catch Exception e nil))

               ;; Define filter function.
               f (if all every? some)]
           (cond (map? value) (f #(func (% 0) (% 1)) (for [obj objs, v (process-func value o)] [obj v]))
                 (set? value) (f #(let [lf (if not-any every? some)]
                                   (lf (fn [o] (func o %)) value)) objs)
                 :else (f #(func % value) objs))))))


(defn- get-objs-by-path
  "Returns sequence of objects which has cl-target's 
  (value of the :what key from m) class and are belonged to 'sources' objects."
  [sources m]
  (let [{:keys [preds where ^Class what sort exactly unique limit]} m
        f (if exactly #(= (class %) what) #(instance? what %))
        path (apply min-key count where) ; At this moment we use path with minimum edges.
        elems (sort-rq (filter-by-preds 
                         (filter f
                                 (let [objs (reduce #(get-objs %2 %1) sources path)]
                                   (if unique (distinct objs) objs))) preds)
                       sort false)]
    (limiting elems limit)))


(defn- process-prop
  "Processes property."
  [[prop ^Boolean is-recur] obj]
  (cond (map? prop) 
        (let [fr (process-func prop, obj)]
          (if (and (seq? fr) (= (count fr) 1))
            (first fr)
            fr))
        (= prop :#self-object#) obj
        (= prop :#default-property#) (get-fv obj (p/get-dp (class obj) @a-mom))
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
    (map #(let [new-objs (process-prop % obj)]
            (if (sequential? new-objs)
              ; If value of property is sequence (or array) then we
              ; return sequence. It may be usefull for queries
              ; something like this building.floors.rooms.occupancy
              (seq new-objs)
              new-objs))
         props)))


(defn- process-then
  "Processes :then value of query structure.
  Returns sequence of objects."
  [objs, then, props, tsort]
  (loop [objs- objs, then- then, props- props, tsort tsort]
    (if (or (nil? then-) (every? nil? objs-))
      (let [pp (remove #(let [v (% 1)] 
                          (if (seq? v)
                            (some (fn [o] (= o :not-found)) v)
                            false))
                       (map (fn [o] [o, (process-props o props-)]) objs-))]
        (if props-
          (sort-rq pp tsort true)
          pp))
      (recur (if (and (nil? (:where then-)) props-)
               (remove #(= % :not-found) (flatten (map #(process-props % props-) objs-)))
               (get-objs-by-path objs- then-))
             (:then then-) 
             (:props then-)
             (:sort then-)))))


(defn- p-nest
  "Processes :nest value with some objects."
  [^PersistentArrayMap nest, objs]
  (if (empty? objs)
    []
    (let [n (:nest nest)]
      (reduce #(conj %1 (%2 1) (if (nil? n) [] (process-nests n (partial get-objs-by-path [(%2 0)]))))
              []
              (process-then objs (:then nest) (:props nest) (:sort nest))))))


(defn- process-nests
  "Processes :nest value of query structure"
  [nests f]
  (let [op (atom nil)]
    (reduce #(if (map? %2)
               (let [v1 (if (nil? (get %2 :func)) 
                          (p-nest %2 (f %2))
                          (reduce (fn [r rf] 
                                    (conj r rf [])) [] (process-func %2 nil)))]
                 (if @op
                   (@op %1 v1)
                   v1))
               (do (reset! op %2) %1))
            []
            nests)))


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
   (if (empty? data)
     (if (empty? (nth args 0))
       ()
       (list (vec (flatten args))))
     (mapcat (fn [o]
               (cond (empty? o) [nil]
                     (empty? (nth o 1)) (for [pair (partition 2 o)] (vec (flatten [args pair])))
                     :else (mapcat #(if (empty? %) 
                                      [] 
                                      (get-rows (nth % 1) args (nth o 0) (nth % 0))) 
                                   (partition 2 (nth o 1)))))
             (partition 2 data)))))


(def
  ^{:doc "The memoized version of the parse function from the parsing.clj"}
  mparse (memoize p/parse))


(defn- get-message
  "Returns message of error due to specified Throwable object."
  [^Throwable e]
  (let [msg (.getMessage e)
        msg (if (nil? msg) (.toString e) msg)]
    msg))


(defn get-qr
  "Takes result of parsing and returns result of quering."
  [parse-res ^PersistentArrayMap mom ^ElementManager em]
  (reset! a-em em) 
  (reset! a-mom mom)
  (let [query-res (if (instance? Throwable parse-res)
                    parse-res
                    (try
                      (process-nests parse-res select-elems)
                      (catch Throwable e e)))]
    (if (instance? Throwable query-res)
      (Result. [] (get-message query-res) query-res [] ())
      ;(Result. query-res nil nil [] ()))))
      (let [rows (get-rows query-res)]
        (Result. query-res nil nil (get-columns-lite rows) rows)))))


(defn pquery
  "Returns map where
    :error - defines message of an error 
            (:error is nil if any error is not occured)
    :result - a result of a query
    :columns - vector with column's names.
    :rows - rows of the result of a query."
  ([^String query ^ElementManager em]
   (pquery query (.getMom em) em))
  ([^String query ^PersistentArrayMap mom ^ElementManager em]
   (if (empty? query)
     (Result. [[]] nil nil [] ())
     (let [parse-res (try
                       (p/parse query mom)
                       (catch Throwable e e))
           parse-res (if (nil? parse-res) (Throwable. "Result of parsing is nil.") parse-res)]
       (get-qr parse-res mom em)))))


(defmacro defq
  "Macro for definitions queries. May be usefull for 
  parameterized queries:
    (defq q \"floor#(number=$1)\") 
    (q 1)
    (q 2)"
  [name ^String query]
  (let [mi (meta name)
        conn (:conn mi)
        conn (if (nil? conn) mi conn)
        {:keys [mom em]} conn
        parse-res (p/parse query (eval mom))
        nparams (count @p/query-params)
        params (repeatedly nparams gensym)]
    `(defn ~(symbol (str name)) 
       ([~@params] (do (reset! p/query-params (list ~@params))
                     (get-qr ~parse-res ~mom ~em))))))
