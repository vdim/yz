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
  (:require [clojure.xml :as cx] 
            [clojure.set :as cs]
            [clojure.string :as cst]
            [clojure.java.io :as cio])
  (:import (javax.persistence Transient EntityManagerFactory)))


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
      (cond (contains? (ancestors pt) java.util.Collection)
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
  (remove nil?
    (map #(check-type % classes)
         (seq (.. java.beans.Introspector
                (getBeanInfo cl)
                (getPropertyDescriptors))))))


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

  
(defn get-paths
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


(defn- get-s-paths
  "Gets maps from get-paths and transforms value of :ppath key to
  one string. Returns sequence of this strings."
  [from to classes]
  (vec (map :ppath (get-paths from to classes))))


(defn get-fields-name
  "Returns list of all field's names (including superclass's
  fields and excluding fields with Transient annotation)."
  [cl]
  (if (or (nil? cl)  (.isInterface cl))
    ()
    (map name (keys (bean (.newInstance cl))))))


(defn get-short-name
  "Return a short name for the specified class. 
  The short is a set of upper letters from the simple name of the class."
  [cl]
  (.toLowerCase (reduce str ""
                        (for [a (.getSimpleName cl) 
                              :when (< (int a) (int \a))] a))))

(defn init-map-for-cl
  "Inits map for specified class. Adds following keys and values:
    - :sn (short name)
    - :dp (default property)
    - :superclass (super class)
    - :properties (list of properties)"
  [cl, map-cl-old]
  {:sn (get-short-name cl)
   :dp (:dp map-cl-old)
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

(defn- gen-basic-mom
  "Generates mom from list of classes 
  (\"classes\" contains list with Class of name mom's classes.)"
  [classes, mom-old]
  (reduce (fn [m cl]
            (assoc m
                   cl
                   (reduce #(assoc 
                              %1 
                              %2 
                              (get-s-paths cl %2 (set classes)) ) 
                           (init-map-for-cl cl (get mom-old cl)) 
                           classes)))
          mom-old
          classes))

(defn gen-mom
  "Generates mom from list of classes."
  [classes, mom-old]
  (let [mom (gen-basic-mom classes, mom-old)
        sns (get-sns mom, (:sns mom-old))
        snames (get-names mom :simpleName (:snames mom-old))
        names (get-names mom :name (:names mom-old))]
    (assoc mom :sns sns :names names :snames snames)))

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


(defn- to-file
  "Writes mom to file"
  [mom, f]
  (let [f (if (instance? String f)
            (cio/file f)
            f)]
    (cio/copy (.toString mom) f)))


(defn mom-from-file
  "Takes a name of the file (resource file) and restores a mom from one."
  [f]
  (let [fl (ClassLoader/getSystemResourceAsStream f)
        fl (if (nil? fl) (cio/file f) fl)]
    (load-reader (cio/reader fl))))


(defn mom-to-file
  "Writes mom to file. If emf-or-hbcfg-or-mom is 
  EntityManagerFactory, String (name of hibernate config file) 
  or list of classes then first mom is generated and then wrote to file.
  If 'append' is supplied then information is appended to existing file."
  ([emf-or-hbcfg-or-mom f]
   (mom-to-file emf-or-hbcfg-or-mom f false))
  ([emf-or-hbcfg-or-mom f ^Boolean append]
   (let [mom-old (if (and append (.exists (cio/file f))) (mom-from-file f) {})
         s emf-or-hbcfg-or-mom
         mom (cond (instance? EntityManagerFactory s) (gen-mom-from-metamodel s, mom-old)
                   (instance? String s) (gen-mom-from-cfg s, mom-old)
                   (sequential? s) (gen-mom s, mom-old)
                   :else s)]
     (to-file mom f))))

