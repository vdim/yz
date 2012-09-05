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


; This functions are for working with hibernate as
; user (at least we use hibernate in nest) 
; can use hibernate as framework between its 
; object model and database.
; We suppose that hibernate.cfg.xml has structure like this:
;    <hibernate-configuration>
;      <session-factory>
;          <property name="hibernate.connection.driver_class">org.apache.derby.jdbc.EmbeddedDriver</property>
;          <property name="hibernate.connection.url">jdbc:derby:db;create=true</property>
;          <!-- and so on.-->
;          <mapping class="ru.petrsu.nest.son.EthernetInterface"/>
;          <mapping class="ru.petrsu.nest.son.Network"/>
;          <mapping class="ru.petrsu.nest.son.IPNetwork"/>
;          <!-- and so on.-->
;    </session-factory>
;  </hibernate-configuration>



(ns ru.petrsu.nest.yz.hb-utils
  ^{:author "Vyacheslav Dimitrov"
    :doc "This functions are for working with hibernate because
         user (at least we use hibernate in nest) 
         can use hibernate as framework between its 
         object model and database."}
  (:use clojure.pprint)
  (:require [clojure.xml :as cx] 
            [clojure.set :as cs]
            [clojure.string :as cst]
            [clojure.java.io :as cio] 
            [ru.petrsu.nest.yz.utils :as u])
  (:import (javax.persistence Transient EntityManagerFactory Persistence)))


(defn- get-classes
  "Returns sequence of name of classes (package+name) from hibernate.cfg.xml.
  Function extracts value from mapping tag and class attribute."
  [hb-name]
  (map :class  
    (filter :class 
      (for [attr (:content ((:content (cx/parse hb-name)) 0))] (:attrs attr)))))


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
      (for [[cl prop] rels]
        (assoc (assoc m :ppath (conj (:ppath m) prop)) :path (conj (:path m) cl))))))


(defn- check-to
  "Splits vector with paths ('v') into to vectors:
  first vector contains maps where last element of :path key equals 'to',
  second vector otherwise."
  [to v]
  (loop [to-t [] to-f [] vv v]
    (if (empty? vv)
      [to-t to-f]
      (let [m (first vv)]
        (if (= (last (:path m)) to)
          (recur (conj to-t m) to-f (rest vv))
          (recur to-t (conj to-f m) (rest vv)))))))

  
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
               (set (remove #(contains? new-paths %) new-elems)) 
               new-paths 
               (concat res to-t))))))


(defn get-s-paths
  "Gets maps from get-ps and transforms value of :ppath key to
  one string. Returns sequence of this strings."
  [from to classes]
  (vec (map :ppath (get-ps from to classes))))


(defn get-fields-name
  "Returns list of all field's names (including superclass's
  fields and excluding fields with Transient annotation)."
  [cl]
  (if (or (nil? cl)  (.isInterface cl))
    ()
    (map name (keys (bean (.newInstance cl))))))


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


(defn- get-sns
  "Creates a map: short names (key) 
  and classes (value) from MOM as value."
  [mom, old-sns]
  (reduce #(assoc %1 (:sn (%2 1)) (%2 0)) old-sns mom))


(defn- get-names
  [mom, key, old-names]
  "Creates a map: names (key) 
  and classes (value) from MOM as value."
  (reduce #(let [cl (%2 0)]
             (if (instance? Class cl)
               (assoc %1 
                      (cst/lower-case (key (bean cl))) 
                      cl)
               %1)) old-names mom))


(defn paths-to-parent
  "Returns paths from cl-source to parent of cl-target."
  [mom cl-source cl-target]
  (some #(if (not= % cl-source)
           (let [ps (get-in mom [cl-source %])]
             (if (not (empty? ps)) 
               ps)))
        (ancestors cl-target)))


(defn paths-to-child
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
          {}
          (get-in mom [:children cl-source])))


(defn- gen-basic-mom
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


(defn copy-paths
  "If there is path from one class to another and from another class to
  yet another class but there is not path from the one class to the 
  yet another class then we create path from the one class to the
  yet another class."
  [mom classes]
  (reduce (fn [m cl-source]
            (reduce (fn [m2 cl-target] 
                      (let [paths (get-in m2 [cl-source cl-target])]
                            (if (not (and (empty? paths) (map? paths)))
                              (let [m-of-source (get m2 cl-source)
                                    m-of-target (get m2 cl-target)]
                                (assoc m2 cl-source
                                       (reduce #(if (or (= cl-source %2) (get m-of-source %2))
                                                  %
                                                  (let [ps (vec (for [a paths b (get m-of-target %2)] (vec (concat a b))))]
                                                    (if (not-empty ps)
                                                      (assoc %1 %2 ps)
                                                      %)))
                                               m-of-source classes)))
                              m2)))
                    m 
                    classes))
          mom
          classes))


(defn get-paths-to-parent
  "In case path between two classes is empty, then
  this function tries to find path between first class and
  parent of second class."
  [mom classes f]
  (reduce (fn [m cl-source]
            (reduce (fn [m2 cl-target] 
                      (let [paths (get-in m2 [cl-source cl-target])]
                        (if (empty? paths)
                          (let [paths (f m2 cl-source cl-target)]
                            (if (empty? paths) 
                              m2
                              (assoc-in m2 [cl-source cl-target] paths)))
                          m2)))
                    m 
                    classes))
          mom
          classes))


(defn gen-mom
  "Generates mom from list of classes."
  [classes, mom-old]
  (let [mom (dissoc-nil (gen-basic-mom classes, mom-old))
        sns (get-sns mom, (:sns mom-old))
        snames (get-names mom :simpleName (:snames mom-old))
        names (get-names mom :name (:names mom-old))
        children (children classes)
        mom (assoc mom :sns sns :names names :snames snames :children children)
        mom (get-paths-to-parent mom classes paths-to-parent)
        mom (copy-paths mom classes)
        mom (get-paths-to-parent mom classes paths-to-child)]
    mom))


(defn gen-mom-from-cfg
  "Generates MOM from hibernate configuration xml file 
      (usual named hibernate.cfg.xml) with 'mapping' tags.
      It's usefull in case when you use hibernate as
      implementation of Criteria API 2.0."
  [hb-name, mom-old]
  (gen-mom (map #(Class/forName %) (get-classes hb-name)), mom-old))


(defn gen-mom-from-metamodel
  "Takes EntityManagerFactory and generates mom from metamodel."
  [emf, mom-old]
  (gen-mom (map #(.getJavaType %) (.. emf getMetamodel getEntities)), mom-old))


(defn- local-dispatch-var 
  "Dispatcher for Var class for pretty printing."
  [^clojure.lang.Var v] 
  (print v))


(defn- to-file
  "Writes mom to file"
  [mom, f]
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
  [mom]
  (let [tocl #(if (symbol? %) (Class/forName (name %)) %)]
    (cond (map? mom)
          (reduce (fn [m [k v]] (assoc m (tocl k) (symbol->class v)))
                  {} 
                  mom)
          (set? mom) (map tocl mom)
          :else (tocl mom))))


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
    emf-or-hbcfg-or-mom - EntityManagerFactory or String (name of hibernate config file) 
                          or list of classes or MOM. In case it is not MOM
                          first MOM is generated and then wrote to file.
    f - a name of target file.
    append - If 'append' is supplied (true) then information is appended to existing file
             (false by default)."
  ([emf-or-hbcfg-or-mom f]
   (mom-to-file emf-or-hbcfg-or-mom f false))
  ([emf-or-hbcfg-or-mom f ^Boolean append]
   (let [mom-old (if (true? append) (mom-from-file f) {})
         s emf-or-hbcfg-or-mom
         mom (cond
               ; JPA's EntityManagerFactory TODO: replace by ru.petrsu.nest.yz.core.ElementManager
               (instance? EntityManagerFactory s) (gen-mom-from-metamodel s, mom-old)
                   
               ; hibernate.cfg.xml
               (instance? String s) (gen-mom-from-cfg s, mom-old)
                   
               ; List with classes
               (sequential? s) (gen-mom s, mom-old)
               
               ; MOM itself.
               :else s)]
     (to-file mom f))))


(defn gen-mom*
  "Generates MOM due to specfied parameters:
    out - name of file for MOM.
    old-mom - name of file with old-mom (empty if not needed).
    append - define whether new mom must be appended to old mom.
    src - source of classes
    classes - list with classes or name of file in case 
      hibernate configuration is used."
  [out old-mom ^Boolean append src classes]
  (let [old-mom (if (and append (not (empty? old-mom))) (mom-from-file old-mom) {})
        mom (case src 
              :hibernate-cfg 
              (gen-mom-from-cfg classes old-mom)
              :persistense 
              (gen-mom-from-metamodel 
                (Persistence/createEntityManagerFactory classes) old-mom)
              :list-classes 
              (gen-mom (map #(Class/forName %) (remove empty? (cst/split classes #"\s"))) 
                       old-mom)
              (throw (Exception. (str "Unexpected type of sources: " src))))]
    (mom-to-file mom out)))


