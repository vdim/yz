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


(defn- get-fv
  "Returns value of field."
  [o, field-name]
  (.get (doto (.getDeclaredField (class o) field-name) (.setAccessible true)) o))


(defn- get-objs
  "Returns sequence of objects which is belonged to 'objs' 
  by specified 'field-name'"
  [field-name, objs]
  (flatten
    (map (fn [o] 
           (if-let [fv (get-fv o field-name)]
             (if (instance? java.util.Collection fv)
               (reduce #(conj %1 %2) [] fv)
               fv)))
           objs)))


(defn- get-objs-by-path
  "Returns sequence of objects which has cl-target's class and is 
  belonged to 'sources' objects (search is based on mom)."
  [sources cl-target mom]
  (if-let [paths (get (get mom (class (nth sources 0))) cl-target)]
    (loop [ps (nth paths 0) res sources]
      (if (empty? ps)
        res
        (recur (rest ps) (get-objs (first ps) res))))
    (throw (Exception. (str "Not found path between " (class (nth sources 0)) " and " cl-target ".")))))


(defn- process-then
  "Processes :then value of query structure."
  [then, objs, mom]
  (loop [then- then objs- objs]
    (if (or (nil? then-) (every? nil? objs-))
      objs-
      (recur (:then then-) (get-objs-by-path objs- (:what then-) mom)))))


(declare process-nests)
(defn- process-nest
  "Processes one element from vector from :nest value of query structure."
  [nest objs mom]
  (reduce #(assoc %1 %2 (process-nests (:nest nest) %2 mom)) 
          {}
          (process-then (:then nest) (get-objs-by-path objs (:what nest) mom) mom)))


(defn- process-nests
  "Processes :nest value of query structure"
  [nests obj mom]
  (vec (map #(process-nest % [obj] mom) nests)))


(defn- do-query
  "Gets structure of query getting from parser and returns
  structure of user's result."
  [em mom q]
  (reduce #(assoc %1 %2 (process-nests (:nest q) %2 mom)) 
          {}
          (process-then (:then q) (select-elems (:what q) em) mom)))


(defn run-query
  "Returns result of 'query' based on specified map of object model ('mom')
  and instance of javax.persistence.EntityManager ('em')."
  [query mom em]
  (let [parse-res (p/parse query mom)]
    (vec (map #(do-query em mom %) parse-res))))

