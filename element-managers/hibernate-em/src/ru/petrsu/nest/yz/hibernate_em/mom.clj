;;
;; Copyright 2012 Vyacheslav Dimitrov <vyacheslav.dimitrov@gmail.com>
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
; user can use hibernate as framework between its 
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



(ns ru.petrsu.nest.yz.hibernate-em.mom
  ^{:author "Vyacheslav Dimitrov"
    :doc "Helper functions for generating and using the MOM from
         Hibernate Entity Manager.."}
  (:use clojure.pprint)
  (:require [clojure.xml :as cx] 
            [clojure.set :as cs]
            [clojure.string :as cst]
            [clojure.java.io :as cio] 
            [ru.petrsu.nest.yz.utils :as u] 
            [ru.petrsu.nest.yz.hb-utils :as hu])
  (:import (javax.persistence Transient EntityManagerFactory Persistence)))


(defn- get-classes
  "Returns sequence of name of classes (package+name) from hibernate.cfg.xml.
  Function extracts value from mapping tag and class attribute."
  [hb-name]
  (map :class  
    (filter :class 
      (for [attr (:content ((:content (cx/parse hb-name)) 0))] (:attrs attr)))))


(defn gen-mom-from-cfg
  "Generates MOM from hibernate configuration xml file 
      (usual named hibernate.cfg.xml) with 'mapping' tags.
      It's usefull in case when you use hibernate as
      implementation of Criteria API 2.0."
  [hb-name, mom-old]
  (hu/gen-mom (map #(Class/forName %) (get-classes hb-name)), mom-old))


(defn gen-mom-from-metamodel
  "Takes EntityManagerFactory and generates mom from metamodel."
  [emf, mom-old]
  (hu/gen-mom (map #(.getJavaType %) (.. emf getMetamodel getEntities)), mom-old))


(defn gen-mom*
  "Generates MOM due to specfied parameters:
    out - name of file for MOM.
    old-mom - name of file with old-mom (empty if not needed).
    append - define whether new mom must be appended to old mom.
    src - source of classes
    classes - list with classes or name of file in case 
      hibernate configuration is used."
  [out old-mom ^Boolean append src classes]
  (let [old-mom (if (and append (not (empty? old-mom))) (hu/mom-from-file old-mom) {})
        mom (case src 
              :hibernate-cfg 
              (gen-mom-from-cfg classes old-mom)
              :persistense 
              (gen-mom-from-metamodel 
                (Persistence/createEntityManagerFactory classes) old-mom)
              :list-classes 
              (hu/gen-mom (map #(Class/forName %) (remove empty? (cst/split classes #"\s"))) 
                       old-mom)
              (throw (Exception. (str "Unexpected type of sources: " src))))]
    (hu/mom-to-file mom out)))


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
   (let [mom-old (if (true? append) (hu/mom-from-file f) {})
         s emf-or-hbcfg-or-mom
         mom (cond
               ; JPA's EntityManagerFactory TODO: replace by ru.petrsu.nest.yz.core.ElementManager
               (instance? EntityManagerFactory s) (gen-mom-from-metamodel s, mom-old)
                   
               ; hibernate.cfg.xml
               (instance? String s) (gen-mom-from-cfg s, mom-old)
                   
               ; Try to use function mom-to-file from hb-utils namespace.
               :else (hu/mom-to-file s f append))]
     (hu/to-file mom f))))
