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
  ^{:author Vyacheslav Dimitrov
    :doc "This functions are for working with hibernate because
         user (at least we use hibernate in nest) 
         can use hibernate as framework between its 
         object model and database."}
  (:require [clojure.xml :as cx] 
            [clojure.set :as cs]))

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
      (if (contains? (ancestors pt) java.util.Collection)
        (let [t (vec (.. pd getReadMethod getGenericReturnType getActualTypeArguments))]
          (if (and (> (count t) 0) (contains? classes (t 0)))
            [(t 0), (.getName pd)] ))))))


(defn get-related
  "Returns related classes for specified Class's instance.
  ('classes' must be set for correct working 'contains?' function."
  [cl classes]
  (remove nil?
    (map #(check-type % classes)
         (seq (.. java.beans.Introspector
                (getBeanInfo cl)
                (getPropertyDescriptors))))))

(defn adds-related
  "Adds related classes to last element of :path in map 'm'.
  ('classes' must be set for correct working 'contains?' function.
  and doesn't contains visited elements."
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


(defn gen-mom
  "Generates MOM. There is several possibilities:
    1. Generates from hibernate configuration xml file 
      (usual named hibernate.cfg.xml) with mapping.
      It's usefull in case when you use hibernate as
      implementation of Criteria API 2.0. 
    2. Searches classes with annotations javax.persistence.Entity and
      generates MOM from this list."
  [hb-name]
  (let [classes (map #(Class/forName %) (get-classes hb-name))] ; "classes" contains list with Class of name of classes
    (mget-paths
;      {:path [ru.petrsu.nest.son.Occupancy] :ppath []}
      ru.petrsu.nest.son.Device
      ru.petrsu.nest.son.Building
      (set classes))))
;    (for [from classes to classes] 
;      [from to])))

(gen-mom "/home/adim/tsen/clj/libs/yz/test/etc/hibernate.cfg.xml")
