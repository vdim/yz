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
;  (map #(reduce (fn [x1, x2] (str x1 "." x2)) (:ppath %)) 
  (map :ppath (get-paths from to classes)))


; TODO: exculde fields with javax.persistence.Transient
(defn get-fields-name
  "Returns list of all field's names (including superclass's
  fields and excluding fields with Transient annotation)."
  [cl]
  (loop [cl- cl res ()]
    (if (nil? cl-)
      res
      (recur (:superclass (bean cl-)) 
             (concat res(map #(.getName %) (.getDeclaredFields cl-)))))))

(defn init-map-for-cl
  "Inits map for specified class. Adds following keys and values:
    - :sn (short name)
    - :dp (default property)
    - :superclass (super class)
    - :properties (list of properties)"
  [cl]
  {:sn ""
   :dp ""
   :superclass (:superclass (bean cl))
   :properties (get-fields-name cl)})


(defn gen-mom
  "Generates mom from list of classes 
  (\"classes\" contains list with Class of name mom's classes.)"
  [classes]
  (reduce (fn [m cl]
            (assoc m
                   cl
                   (reduce #(assoc 
                              %1 
                              %2 
                              (get-s-paths cl %2 (set classes)) ) 
                           (init-map-for-cl cl) 
                           classes)))
          {}
          classes))


(defn gen-mom-from-cfg
  "Generates MOM from hibernate configuration xml file 
      (usual named hibernate.cfg.xml) with 'mapping' tags.
      It's usefull in case when you use hibernate as
      implementation of Criteria API 2.0."
  [hb-name]
  (gen-mom (map #(Class/forName %) (get-classes hb-name))))


(defn gen-mom-from-classes
  "Searches classes in classpath with annotations javax.persistence.Entity and
      generates MOM from this list."
  [])

