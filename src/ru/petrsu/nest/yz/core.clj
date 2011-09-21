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


(declare process-nests, get-rows, run-query)
(defn- process-func
  "Gets :function map of q-representation 
  and returns value of evaluation of one."
  [f-map, obj, mom, em]
  (let [params (map #(cond (vector? %)
                           (let [[fmod q] %
                                 rows (if (or (= fmod :indep) (nil? obj))
                                        (get-rows (run-query q mom em))
                                        (get-rows (process-nests q obj mom em)))]
                             (cond (= fmod :single) {:mode :single :res rows}
                                   :else rows))
                           (map? %) {:mode :single :res (process-func % obj mom em)}
                           :else %) 
                    (:params f-map))
        lparams (reduce #(if (and (map? %2) (= (:mode %2) :single)) 
                           (vec (mapcat (fn [lp] (map (fn [o] (conj lp o)) (:res %2))) %1))
                           (vec (map (fn [lp] (conj lp %2)) %1))) [[]] params)]
    (map #(apply (:func f-map) %) lparams)))


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
  [objs, preds]
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
              (filter-by-preds res preds)
              (recur (rest ps) (get-objs (first ps) res)))))))))


(defn- process-prop
  "Processes property."
  [[prop is-recur] obj, mom, em]
  (cond (map? prop) (process-func prop, obj, mom, em)
        (= prop \&) obj
        is-recur (loop [res [] obj- (get-fv obj prop)]
                   (if (nil? obj-)
                     res
                     (recur (conj res obj-) (get-fv obj- prop))))
        :else (get-fv obj prop)))


(defn- process-props
  "If nest has props then function returns value of property(ies),
  otherwise obj is returned."
  [obj, props, mom, em]
  (if (empty? props)
    obj
    (map #(process-prop % obj mom em) props)))


(defn process-then
  "Processes :then value of query structure.
  Returns sequence of objects."
  [then, objs, mom, props, em]
  (loop [then- then objs- objs props- props]
    (if (or (nil? then-) (every? nil? objs-))
      (map (fn [o] [o, (process-props o props- mom em)]) objs-)
      (recur (:then then-) 
             (get-objs-by-path objs- (:what then-) mom (:preds then-))
             (:props then-)))))


(declare process-nests)
(defmacro p-nest
  "Generates code for process :nest value with some objects."
  [nest objs mom em]
  `(reduce #(conj %1 (%2 1) (process-nests (:nest ~nest) (%2 0) ~mom ~em))
          []
          (process-then (:then ~nest) ~objs ~mom (:props ~nest) ~em)))


(defn- process-nest
  "Processes one element from vector from :nest value of query structure."
  [nest objs mom, em]
  (p-nest nest (get-objs-by-path objs (:what nest) mom (:preds nest)) mom em))

(defn- process-nests
  "Processes :nest value of query structure"
  [nests obj mom em]
  (vec (map #(process-nest % [obj] mom em) nests)))


(defn- do-query
  "Gets structure of query getting from parser and returns
  structure of user's result."
  [em mom q]
  (p-nest q (filter-by-preds (select-elems (:what q) em) (:preds q)) mom em))


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
     (list (vec (flatten args)))
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
  [query mom em]
  (if (empty? query)
    (def-result [[]] nil [] ())
    (let [parse-res (try
                      (p/parse query mom)
                      (catch Exception e (.getMessage e)))
          run-query-res (cond (string? parse-res) parse-res
                              (map? parse-res) (try
                                                 (let [pc (process-func parse-res nil mom em)]
                                                   [pc (reduce #(cons [%2] %1) () pc)])
                                                 (catch Exception e (.getMessage e)))
                              :else (try
                                      (let [rq (run-query parse-res mom em)]
                                        [rq (get-rows rq)])
                                      (catch Exception e (.getMessage e))))]
      (if (string? run-query-res)
        (def-result [] run-query-res [] ())
        (def-result (run-query-res 0) nil [] (run-query-res 1))))))

