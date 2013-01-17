;;
;; Copyright 2011-2013 Vyacheslav Dimitrov <vyacheslav.dimitrov@gmail.com>
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


(ns ru.petrsu.nest.yz.mom-utils
  ^{:author "Vyacheslav Dimitrov"
    :doc "Namespace for working with the MOM: generating, 
         writing/reading to/from file."}
  (:use clojure.pprint)
  (:require [clojure.string :as cst]
            [clojure.java.io :as cio] 
            [ru.petrsu.nest.yz.utils :as u]))


(defn- check-type
  "Defines whether type of pd (PropertyDescription) is contained in list of classes."
  [pd classes]
  (let [pt (.getPropertyType pd)]
    (if (contains? classes pt)
      [pt (.getName pd)]
      (cond (nil? pt) nil
            (contains? (ancestors pt) java.util.Collection)
            (let [t (vec (.. pd getReadMethod getGenericReturnType getActualTypeArguments))]
              (if (and (> (count t) 0) (contains? classes (t 0)))
                [(t 0), (.getName pd)] ))
            (.isArray pt)
            (let [t (.getComponentType pt)]
              (if (and (not (nil? t)) (contains? classes t))
                [t, (.getName pd)]))))))


(defn new-instance
  "Returns new instance 
  for specified class."
  [cl mom]
  (if (u/int-or-abs? cl)
    (let [children (get-in mom [:children cl])
          children (remove u/int-or-abs? children)]
      (.newInstance (first children)))
    (.newInstance cl)))


(defn un-path?
  "Returns true in case path is 
  unnecessary for specified class."
  [cl path mom]
  (let [o (new-instance cl mom)]
    (loop [cl cl prop (first path) path (next path) object o]
      (let [pd (some #(if (= (.getName %) prop) %) (u/descriptors cl))]
        (if (nil? pd)
          false
          (if (empty? path)
            (identical? o (.invoke (.getReadMethod pd) object (into-array [])))
            (let [pt (.getPropertyType pd)

                  ; t is type of property
                  ; new-o is instance of t.
                  ; o-for-in is object for write method.
                  [t new-o o-for-in] 
                  (cond (contains? (ancestors pt) java.util.Collection)
                        (let [t ((vec (.. pd getReadMethod getGenericReturnType getActualTypeArguments)) 0)
                              new-o (new-instance t mom)]
                          [t new-o [#{new-o}]])
                  
                        (.isArray pt) 
                        (let [t (.getComponentType pt)
                              new-o (new-instance t mom)]
                          [t new-o [(into-array [new-o])]])
      
                          :else (let [new-o (new-instance pt mom)]
                                  [pt new-o [new-o]]))
                  _ (.invoke (.getWriteMethod pd) object (into-array o-for-in))]
              (recur t (first path) (next path) new-o))))))))


(defn filter-paths
  "Filters paths due to the un-path? function."
  [cl paths mom]
  (vec (remove #(try
                  (un-path? cl % mom)
                  (catch Exception e false)) 
               paths)))


(defn get-related
  "Returns related classes for specified Class's instance.
  ('classes' must be set for correct working 'contains?' function."
  [cl classes children]
  (let [f (fn [descs] (remove nil? (map #(check-type % classes) descs)))
        rels (f (u/descriptors cl))]
    (if (empty? rels)
      (f (mapcat #(u/descriptors %) (get children cl)))
      rels)))


(defn- adds-related
  "Adds related classes to last element of :path in map 'm'.
  ('classes' must be set for correct working 'contains?' function.
  and doesn't contains visited elements)."
  [m classes children]
  (let [rels (get-related (last (:path m)) classes children)]
    (if (empty? rels)
      m
      (map (fn [[cl prop]] (assoc m :ppath (conj (:ppath m) prop) :path (conj (:path m) cl))) rels))))


(defn- check-to
  "Splits vector with paths ('v') into to vectors:
  first vector contains maps where last element of :path key equals 'to',
  second vector otherwise."
  [to v children]
  (reduce 
    (fn [[to-t to-f] m]
      (let [l (-> m :path last)]
        (if (or (= l to) (contains? (set (get children l)) to))
          [(conj to-t m) to-f]
          [to-t (conj to-f m)])))
      [[] []]
    v))

  
(defn get-ps
  "Returns sequence of maps. Each map contains following keys:
    :path path between \"from\" and \"to\" as classes
    :ppath path between \"from\" and \"to\" as properties"
  [from to elems children]
  (loop [all-paths [{:path [from] :ppath []}] 
         new-elems elems
         old-paths #{from}
         res ()]
    (let [all-paths (flatten (map #(adds-related % new-elems children) all-paths)) 
          [to-t to-f] (check-to to all-paths children)
          new-paths (set (flatten (map #(:path %) all-paths)))]
      (if (= old-paths new-paths) ; not new piece of path
        res
        (recur to-f 
               (set (remove #(and (not= % to) (contains? new-paths %)) new-elems)) 
               new-paths 
               (concat res to-t))))))


(defn get-s-paths
  "Gets maps from get-ps and transforms value of :ppath key to
  one string. Returns sequence of this strings."
  [from to classes children] 
  (let [res (vec (map :ppath (get-ps from to classes children)))]
    (if (empty? res)
      res
      (let [min-count (apply min (map count res))]
        (-> (filter #(= (count %) min-count) res) set vec)))))


(defn init-map-for-cl
  "Inits map for specified class. Adds following keys and values:
    - :sn (short name)
    - :dp (default property)
    - :sort (functions for sorting)
    - :p-properties ()
    - :superclass (super class)"
  [cl, map-cl-old]
  {:sn (u/get-short-name cl)
   :dp (or (:dp map-cl-old) (u/dp cl))
   :sort (:sort map-cl-old)
   :p-properties (:p-properties map-cl-old)
   :superclass (:superclass (bean cl))})


(defn- get-names
  [mom, old-names]
  "Creates a map: names (key) 
  and classes (value) from MOM as value."
  (reduce #(let [cl (%2 0)]
             (if (class? cl)
               (assoc %1 
                      (-> cl bean :simpleName cst/lower-case) cl
                      (-> cl bean :name cst/lower-case) cl
                      (:sn (%2 1)) cl)
               %1)) old-names mom))


(defn gen-basic-mom
  "Generates mom from list of classes 
  (\"classes\" contains list with Class of name mom's classes.)"
  [classes, mom-old]
  (reduce (fn [m cl]
            (assoc m
                   cl
                   (if (u/int-or-abs? cl) 
                     (init-map-for-cl cl (get mom-old cl))
                     (reduce #(let [paths (get-s-paths cl %2 (set classes) (:children mom-old))
                                    paths (if (= %2 cl)
                                            (filter-paths cl paths mom-old)
                                            paths)]
                                (if (empty? paths)
                                  %1 
                                  (assoc %1 %2 paths)))
                             (init-map-for-cl cl (get mom-old cl))
                             classes))))
          mom-old
          classes))


(defn- dissoc-nil
  "Returns new MOM where pairs 
  with nil value are removed."
  [mom]
  (reduce 
    (fn [m, [k v]]
      (if (class? k)
        (assoc m k (reduce (fn [m [k v]]
                             (if (nil? v)
                               m
                               (assoc m k v))) {} v))
        (assoc m k v)))
    {} mom))


(defn children
  "For specified list of classes returns map: class -> children."
  [classes]
  (let [s-classes (set classes)]
    (reduce
      (fn [m cl]
        (let [super-cls (loop [cl- cl s-cls []] 
                          (if-let [cl- (:superclass (bean cl-))]
                            (recur cl- (conj s-cls cl-))
                            s-cls)) 
              super-cls+ints (filter #(contains? s-classes %) (set (concat super-cls (:interfaces (bean cl)))))]
          (reduce (fn [m clazz] (update-in m [clazz] #(conj (if (nil? %) #{} %) cl))) 
                  m 
                  super-cls+ints)))
      {}
      s-classes)))


(defn c-paths
  "Counts elements which have paths 
  into specified MOM."
  [mom cl-cl]
  (reduce #(if (get-in mom %2) (inc %1) %1) 0 cl-cl))


(defn c-all-paths
  "Counts all paths into specified MOM."
  [mom cl-cl]
  (reduce #(if (get-in mom %2) (+ %1 (count (get-in mom %2))) %1) 0 cl-cl))


(defn gen-mom
  "Generates mom from list of classes."
  ([classes]
   (gen-mom classes {}))
  ([classes, mom-old]
   (let [cl-cl (for [cl-s classes cl-t classes] [cl-s cl-t])
         
         mom-old (assoc mom-old :children (children classes))
         mom (dissoc-nil (gen-basic-mom classes mom-old))
         mom (assoc mom 
                    :names (get-names mom (:names mom-old))
                    :children (:children mom-old)
                    :namespaces (get mom-old :namespaces))]
     mom)))


(defn- local-dispatch-var 
  "Dispatcher for Var class for pretty printing."
  [^clojure.lang.Var v] 
  (print v))


(defn to-file
  "Writes mom to file"
  [mom f]
  (let [f (if (instance? String f)
            (cio/file f)
            f)]
    (binding [*out* (cio/writer f)] ; Needed for pretty write.
      (let [cd code-dispatch
            _ (. cd addMethod clojure.lang.Var local-dispatch-var)]
        (with-pprint-dispatch cd (println (write mom :stream nil)))))))


(defn- symbol->class 
  "Takes some object.
  In case object is map then transform symbol keys or values to class.
  In case object is set then transform symbol values to class.
  In case object is symbol, then transform object to class."
  [o]
  (let [tocl #(if (symbol? %) (Class/forName (name %)) %)]
    (cond (map? o)
          (reduce (fn [m [k v]] (assoc m (tocl k) (symbol->class v)))
                  {} 
                  o)
          (set? o) (map tocl o)
          :else (tocl o))))


(defn mom-from-file
  "Takes a name of the file (resource file) and restores a mom from one."
  [f]
  ; WARNING: DON'T CHANGE THIS CODE TO CODE WHICH
  ; IS USED THE LOAD-READER FUNCTION BECAUSE OF IN CASE
  ; MOM IS MORE THAN 64Kb THEN EXCEPTION CLASSFORMATERROR
  ; IS CAUSED.
  (let [fl (ClassLoader/getSystemResourceAsStream f)
        fl (or fl (java.io.FileInputStream. (cio/file f)))]
    (with-open [r (-> (java.io.InputStreamReader. fl)
                    (java.io.BufferedReader.)
                    (java.io.PushbackReader.))]
      (let [mom (symbol->class (read r))
            ; Make using user's namespaces.
            ; MOM can have key :namespaces and string value,
            ; which has code for using namespaces. For example
            ; {:namespaces "(use 'ru.petrsu.nest.util.utils)", ...}
            namespaces (:namespaces mom)
            _ (if namespaces (eval (read-string namespaces)))]
        mom))))


(defn mom-to-file
  "Writes mom to file. Arguments:
    cls-or-mom - List of classes or MOM. In case it is not MOM
                  first MOM is generated and then wrote to file.
    f - a name of target file.
    append - If 'append' is supplied (true) then information is appended to existing file
             (false by default)."
  ([cls-or-mom f]
   (mom-to-file cls-or-mom f false))
  ([cls-or-mom f ^Boolean append]
   (let [mom-old (if (true? append) (mom-from-file f) {})
         mom (cond
               ; List with classes
               (sequential? cls-or-mom) (gen-mom cls-or-mom mom-old)
               
               ; MOM itself.
               :else cls-or-mom)]
     (to-file mom f))))


(defn diffs
  "Returns difference between two moms."
  [mom mom2 cl-cl]
  (reduce
    (fn [v cls]
      (if (not= (get-in mom cls) (get-in mom2 cls))
        (conj v (conj (conj cls (get-in mom cls)) (get-in mom2 cls)))
        v))
    []
    cl-cl))
