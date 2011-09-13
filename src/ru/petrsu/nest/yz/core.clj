(ns ru.petrsu.nest.yz.core
  ^{:author Vyacheslav Dimitrov
    :doc "This code contains core functions of the Clojure's implementation of the YZ language.

         The Parsing of queries does due to the fnparse library.
         See the code for the parsing queries in the parsing.clj file

         Criteria API 2.0 is used as persistence storage."}
  (:require [ru.petrsu.nest.yz.parsing :as p])
  (:import (javax.persistence.criteria CriteriaQuery)))


(defn- select-elems
  "Returns from storage objects which have 'cl' class."
  [cl em]
  (let [cr (.. em getCriteriaBuilder createTupleQuery)
        root (. cr (from cl))]
    (map #(.get % 0) (.. em (createQuery (doto cr (.multiselect [root]))) getResultList))))


(defn get-fv
  "Returns value of field."
  [o, field-name]
  (loop [cl (class o)]
    (if (nil? cl)
      (throw (NoSuchFieldException. ))
      (if (contains? (set (map #(.getName %) (.getDeclaredFields cl))) field-name)
        (.get (doto (.getDeclaredField cl field-name) (.setAccessible true)) o)
        (recur (:superclass (bean cl)))))))


(declare p-nest, filter-by-preds, process-nests, get-rows)
(defn- process-func
  "Gets :function map of q-representation 
  and returns value of evaluation of one."
  [f-map, obj, mom]
  (let [params (map #(if (vector? %)
                       (get-rows (process-nests % obj mom))
                       %) 
                    (:params f-map))]
    (apply (:func f-map) params)))


(defn- get-objs
  "Returns sequence of objects which are belonged to 'objs' 
  by specified 'field-name'"
  [field-name, objs]
  (flatten
    (map (fn [o] 
           (if-let [fv (get-fv o field-name)]
             (if (instance? java.util.Collection fv)
               (reduce #(conj %1 %2) [] fv)
               fv)))
           objs)))


(defn process-preds
  "Processes restrictions."
  [o, paths f value]
  (some #(f % value) (reduce #(get-objs %2 %1) [o] paths)))


(defn- filter-by-preds
  "Gets sequence of objects and string of restrictions and
  returns new sequence of objects which are filtered by specified preds."
  [objs, preds, mom]
  (if (nil? preds) 
    objs 
    (let [f (read-string preds)] 
      (filter #(f %) objs))))


(defn- get-objs-by-path
  "Returns sequence of objects which has cl-target's class and are
  belonged to 'sources' objects (search is based on mom)."
  [sources cl-target mom preds]
  (let [cl-source (class (nth sources 0))]
    (loop [cl- cl-target]
      (let [paths (get (get mom cl-source) cl-)]
        (if (empty? paths)
          (if (nil? cl-)
            (throw (Exception. (str "Not found path between " cl-source " and " cl-target ".")))
            (recur (:superclass (get mom cl-target))))
          (loop [ps (nth paths 0) res sources]
            (if (empty? ps)
              (filter-by-preds res preds mom)
              (recur (rest ps) (get-objs (first ps) res)))))))))


(defn- process-prop
  "Processes property."
  [[prop is-recur] obj, mom]
  (cond (map? prop) (process-func prop, obj, mom)
        (= prop \&) obj
        is-recur (loop [res [] obj- (get-fv obj prop)]
                   (if (nil? obj-)
                     res
                     (recur (conj res obj-) (get-fv obj- prop))))
        :else (get-fv obj prop)))


(defn- process-props
  "If nest has props then function returns value of property(ies),
  otherwise obj is returned."
  [obj, props, mom]
  (if (empty? props)
    obj
    (map #(process-prop % obj mom) props)))


(defn process-then
  "Processes :then value of query structure.
  Returns sequence of objects."
  [then, objs, mom, props]
  (loop [then- then objs- objs props- props]
    (if (or (nil? then-) (every? nil? objs-))
      (map (fn [o] [o, (process-props o props- mom)]) objs-)
      (recur (:then then-) 
             (get-objs-by-path objs- (:what then-) mom (:preds then-))
             (:props then-)))))


(declare process-nests)
(defmacro p-nest
  "Generates code for process :nest value with some objects."
  [nest objs mom]
  `(reduce #(conj %1 (%2 1) (process-nests (:nest ~nest) (%2 0) ~mom))
          []
          (process-then (:then ~nest) ~objs ~mom (:props ~nest))))


(defn- process-nest
  "Processes one element from vector from :nest value of query structure."
  [nest objs mom]
  (p-nest nest (get-objs-by-path objs (:what nest) mom (:preds nest)) mom))

(defn- process-nests
  "Processes :nest value of query structure"
  [nests obj mom]
  (vec (map #(process-nest % [obj] mom) nests)))


(defn- do-query
  "Gets structure of query getting from parser and returns
  structure of user's result."
  [em mom q]
  (p-nest q (filter-by-preds (select-elems (:what q) em) (:preds q) mom) mom))


(defn run-query
  "Returns result of 'query' based on specified map of object model ('mom')
  and instance of javax.persistence.EntityManager ('em')."
  [parse-res mom em]
  (vec (map #(do-query em mom %) parse-res)))


(defn- get-column-name
  "Returns the string representation of column 
  for the specified nest from result of query."
  [nest]
  (let [then (:then nest)
        what (:what nest)
        props (:props nest)]
    (cond  
      (not (nil? then)) (loop [then- then]
                          (if (nil? (:then then-))
                            [(.getSimpleName (:what then-))]
                            (recur (:then then-))))
      (not (empty? props)) (map first props)
      :else [(.getSimpleName what)])))


(defn get-columns
  "Returns vector with columns names 
  for the specified result of parsing some query."
  [parse-res]
  (loop [p parse-res res []]
    (if (every? nil? p)
      res
      (recur (flatten (map #(:nest %) p)) 
             (conj res (mapcat #(get-column-name %) p))))))


(defn get-rows
  "Returns set of rows. The 'data' is the result of 
  processing a query."
  ([data]
   (get-rows data ()))
  ([data & args]
   (if (empty? (data 0))
     (flatten args)
     (mapcat (fn [o]
               (if (empty? o)
                 [nil]
                 (if (empty? (o 1))
                   (for [pair (partition 2 o)] (vec (flatten [args pair])))
                   (mapcat #(if (empty? %) [] (get-rows (nth % 1) args (nth % 0))) (partition 2 o)))))
          data))))

(defn def-result
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
  [query mom em]
  (if (empty? query)
    (def-result [[]] nil [] ())
    (let [parse-res (try
                      (p/parse query mom)
                      (catch Exception e (.getMessage e)))
          run-query-res (if (string? parse-res)
                          parse-res
                          (try
                            (run-query parse-res mom em)
                            (catch Exception e (.getMessage e))))]
      (if (string? run-query-res)
        (def-result [] run-query-res [] ())
        (def-result run-query-res nil [] (get-rows run-query-res))))))

