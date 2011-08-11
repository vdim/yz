(ns ru.petrsu.nest.yz.hb-utils
  ^{:author Vyacheslav Dimitrov
    :doc "Functions for working with hibernate.
         User can use hibernate as framework beetwen its 
         object model and database."}
  (:require [clojure.xml :as cx]))

(defn- get-classes
  "Returns sequence of name of classes (package+name) from hibernate.cfg.xml.
  Suppose that hibernate.cfg.xml has structure like this:
    <hibernate-configuration>
      <session-factory>
          <property name=\"hibernate.connection.driver_class\">org.apache.derby.jdbc.EmbeddedDriver</property>
          <property name=\"hibernate.connection.url\">jdbc:derby:db;create=true</property>
          <!-- and so on.-->
          <mapping class=\"ru.petrsu.nest.son.EthernetInterface\"/>
          <mapping class=\"ru.petrsu.nest.son.Network\"/>
          <mapping class=\"ru.petrsu.nest.son.IPNetwork\"/>
          <!-- and so on.-->
    </session-factory>
  </hibernate-configuration>

  Function extracts value from mapping tag and class attribute.
  "
  [hb-name]
  (map :class  
    (filter :class 
      (for [attr (:content ((:content (cx/parse hb-name)) 0))] (:attrs attr)))))

(defn gen-mom
  "Generates MOM. There is several possibility:
    1. Generates from hibernate configuration xml file 
      (usual named hibernate.cfg.xml) with mapping.
      It's usefull in case when you use hibernate as
      implementation of Criteria API 2.0. 
    2. Searches classes with annotations javax.persistence.Entity and
      generates MOM from this list."
  [hb-name]
  (get-classes hb-name))
