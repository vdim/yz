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


(ns ru.petrsu.nest.yz.mom-utils
  ^{:author "Vyacheslav Dimitrov"
    :doc "Namespace for working with the MOM: generating, 
         writing/reading to/from file."}
  (:use clojure.pprint)
  (:require [clojure.string :as cst]
            [clojure.java.io :as cio] 
            [ru.petrsu.nest.yz.utils :as u]))


(defn sort-classes
  "Sorts list of classes in the following order: firsts are parents, 
  nexts are child."
  [classes]
  (sort (fn [cl1 cl2]
          (cond (contains? (-> cl1 ancestors set) cl2) 1 
                (contains? (-> cl2 ancestors set) cl1) -1
                :else 0))
        classes))


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


(defn get-related
  "Returns related classes for specified Class's instance.
  ('classes' must be set for correct working 'contains?' function."
  [cl classes]
  (remove nil? (map #(check-type % classes) (u/descriptors cl))))


(defn- adds-related
  "Adds related classes to last element of :path in map 'm'.
  ('classes' must be set for correct working 'contains?' function.
  and doesn't contains visited elements)."
  [m classes]
  (let [rels (get-related (last (:path m)) classes)]
    (if (empty? rels)
      m
      (map (fn [[cl prop]] 
             (assoc m :ppath (conj (:ppath m) prop) :path (conj (:path m) cl))) rels))))


(defn- check-to
  "Splits vector with paths ('v') into to vectors:
  first vector contains maps where last element of :path key equals 'to',
  second vector otherwise."
  [to v]
  (reduce 
    (fn [[to-t to-f] m]
      (if (= (-> m :path last) to)
        [(conj to-t m) to-f]
        [to-t (conj to-f m)]))
    [[] []]
    v))

  
(defn get-ps
  "Returns sequence of maps. Each map contains following keys:
    :path path between \"from\" and \"to\" as classes
    :ppath path between \"from\" and \"to\" as properties"
  [from to elems]
  (loop [all-paths [{:path [from] :ppath []}] 
         new-elems elems
         old-paths #{from}
         res ()]
    (let [all-paths (flatten (map #(adds-related % new-elems) all-paths)) 
          [to-t to-f] (check-to to all-paths)
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
  [from to classes]
  (vec (map :ppath (get-ps from to classes))))


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
                   (reduce #(let [paths (get-s-paths cl %2 (set classes))]
                              (if (empty? paths)
                                %1 
                                (assoc %1 %2 paths)))
                           (init-map-for-cl cl (get mom-old cl)) 
                           classes)))
          mom-old
          classes))


(defn- dissoc-nil
  "Returns new MOM where pairs with nil value are
  removed."
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


(defn concat-paths
  "If there is path from one class to another and from another class to
  yet another class but there is not path from the one class to the 
  yet another class then we create path from the one class to the
  yet another class."
  [mom cl-cl classes]
  (reduce (fn [mom [cl-source cl-target]] 
            (let [paths (get-in mom [cl-source cl-target])]
                  (if (not (and (empty? paths) (map? paths)))
                    (let [m-of-source (get mom cl-source)
                          m-of-target (get mom cl-target)]
                      (assoc mom cl-source
                             (reduce #(if (or (= cl-source %2) (get m-of-source %2))
                                        %
                                        (let [ps (vec (for [a paths b (get m-of-target %2)] (vec (concat a b))))]
                                          (if (not-empty ps)
                                            (assoc %1 %2 ps)
                                            %)))
                                     m-of-source classes)))
                    mom)))
          mom
          cl-cl))


(defn paths-to-parent
  "Returns paths from cl-source to parent of cl-target."
  [mom cl-source cl-target]
  (some #(if (not= % cl-source)
           (let [ps (get-in mom [cl-source %])]
             (not-empty ps))) 
        (ancestors cl-target)))
        ;[(-> cl-target bean :superclass)]))


(defn paths-to-child
  "Returns paths from cl-source to child of cl-target."
  [mom cl-source cl-target]
  ; In case there are paths between children and cl-target
  ; we return map where child -> paths between this child and
  ; cl-target.
  (reduce #(let [; path from child to cl-target
                 p (get-in mom [cl-source %2])]
             (if p
               (assoc %1 %2 p)
               %1))
          nil
          (get-in mom [:children cl-target])))


(defn paths-from-parent
  "Returns paths from cl-source to child of cl-target."
  [mom cl-source cl-target]
  ; In case there are paths between children and cl-target
  ; we return map where child -> paths between this child and
  ; cl-target.
  (reduce #(let [; path from child to cl-target
                 p (get-in mom [%2 cl-target])]
             (if p
               (assoc %1 %2 p)
               %1))
          nil 
          (ancestors cl-source)))
          ;[(-> cl-source bean :superclass)]))


(defn paths-from-child
  "Returns paths from child of cl-source to cl-target."
  [mom cl-source cl-target]
  ; In case there are paths between children and cl-target
  ; we return map where child -> paths between this child and
  ; cl-target.
  (reduce #(let [; path from child to cl-target
                 p (get-in mom [%2 cl-target])]
             (if p
               (assoc %1 %2 p)
               %1))
          nil
          (get-in mom [:children cl-source])))


(defn paths-from-ints
  "Returns path from an abstract interface 
  to the specified class. The path is map
  where key is a child of \"a-cl\" and value is
  path from the child to \"cl\"."
  [mom a-cl cl]
  (reduce #(let [p (get-in mom [%2 cl])]
             (if (and p (not= %2 cl))
               (assoc %1 %2 p)
               %1))
          nil
          (get-in mom [:children a-cl])))


(defn paths-to-ints
  "Returns path from the specified class to 
  an abstract interface. The path is map
  where key is a child of \"a-cl\" and value is
  path from \"cl\" to the child."
  [mom cl a-cl]
  (reduce #(let [p (get-in mom [cl %2])]
             (if (and p (not= %2 cl))
               (assoc %1 %2 p)
               %1))
          nil
          (get-in mom [:children a-cl])))


(defn get-paths-to
  "In case path between two classes is empty the function 
  tries to find path between first class and
  parent of second class due to specified function f."
  [mom cl-cl f]
  (reduce (fn [mom [cl-s cl-t]]
            (if-let [paths (or (get-in mom [cl-s cl-t]) (f mom cl-s cl-t))]
              (assoc-in mom [cl-s cl-t] paths)
              mom))
          mom 
          cl-cl))


(defn c-paths
  "Counts path into specified MOM."
  [mom cl-cl]
  (reduce #(if (get-in mom %2) (inc %1) %1) 0 cl-cl))


(defn ints-not-ints
  "Takes list of classes and returns vector
  where first element is classes which are not
  interface or abstract class and second element
  is classes which are."
  [classes]
  (reduce (fn [[not-ins ins] cl] (if (u/int-or-abs? cl)
                                   [not-ins (conj ins cl)]
                                   [(conj not-ins cl) ins])) 
          [[] []] 
          classes))


(defn copy-paths
  "Returns new mom. Let's we have three classes: a, b, c.
  The a is parent of the b, there is path between c and a, but
  there is no path between c and b, so we copy the path to c and b."
  [mom classes]
  (reduce
    (fn [mom [a b c]]
      (if (empty? (get-in mom [c b]))
        (let [children (set (get-in mom [:children a]))]
          (if (and (contains? children b) (not-empty (get-in mom [c a])))
            (assoc-in mom  [c b] (get-in mom [c a]))
            mom))
        mom))
    mom
    (for [a classes b classes c classes :when (and (not= a b) (not= b c) (not= a c))] [a b c])))


(defn gen-mom
  "Generates mom from list of classes."
  ([classes]
   (gen-mom classes {}))
  ([classes, mom-old]
   (let [cl-cl (for [cl-s (sort-classes classes) cl-t (sort-classes classes)] [cl-s cl-t])
         [cls-without-ints interfaces] (ints-not-ints classes)
         cl-cl-without-ints (for [cl-s (sort-classes cls-without-ints) 
                                  cl-t (sort-classes cls-without-ints)] 
                              [cl-s cl-t])
         
         mom (dissoc-nil (gen-basic-mom (sort-classes classes) mom-old))
         mom (assoc mom 
                    :names (get-names mom (:names mom-old))
                    :children (children classes)
                    :namespaces (get mom-old :namespaces))

         ; Copy paths.
         mom (copy-paths mom classes)
         ; Concats paths.
         mom (concat-paths mom cl-cl-without-ints cls-without-ints)
;         _ (println "count of paths = " (c-paths mom cl-cl))
;         mom (get-paths-to mom cl-cl-without-ints paths-from-parent)
;         _ (println "count of paths = " (c-paths mom cl-cl))
;         mom (get-paths-to mom cl-cl-without-ints paths-to-parent)
;         _ (println "count of paths = " (c-paths mom cl-cl))
;         mom (get-paths-to mom cl-cl-without-ints paths-from-child)
;         _ (println "count of paths = " (c-paths mom cl-cl))
;         mom (get-paths-to mom cl-cl-without-ints paths-to-child)
;         _ (println "count of paths = " (c-paths mom cl-cl)) 
;         
;         
;         cl-cl2 (for [cl-s (sort-classes interfaces) cl-t (sort-classes cls-without-ints)] [cl-s cl-t])
;         mom (get-paths-to mom cl-cl2 paths-to-ints)
;         _ (println "count of paths = " (c-paths mom cl-cl))  
;         mom (get-paths-to mom cl-cl2 paths-from-ints)
;         _ (println "count of paths = " (c-paths mom cl-cl)) 
;         
;         cl-cl3 (for [cl-s (sort-classes cls-without-ints) cl-t (sort-classes interfaces)] [cl-s cl-t])
;         mom (get-paths-to mom cl-cl3 paths-to-ints)
;         _ (println "count of paths = " (c-paths mom cl-cl))  
;         mom (get-paths-to mom cl-cl3 paths-from-ints)
;         _ (println "count of paths = " (c-paths mom cl-cl)) 
         
         ;_ (println "before copy-paths = " (get-in mom [ru.petrsu.nest.son.CompositeOU ru.petrsu.nest.son.Room]))
        
         
         ;mom (copy-paths mom cl-cl classes)
         ;_ (println "count of paths = " (c-paths mom cl-cl))
         
         ;cls_empty1 (reduce #(if (get-in mom %2) %1 (conj %1 %2)) [] cl-cl)
         ;_ (pprint cls_empty1)
         ;cls_empty2 (reduce #(if (get-in mom %2) %1 (conj %1 %2)) [] cl-cl)
         ;_ (pprint (remove #(contains? (set cls_empty2) %) cls_empty1))
         ;mom (get-paths-to mom classes paths-to-parent)
         ;mom (get-paths-to mom classes paths-to-child)
         ;mom (get-paths-to mom classes paths-from-parent)
         ;mom (get-paths-to mom classes paths-from-child)
         ;mom (copy-paths mom classes)
         ]
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


