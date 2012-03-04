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

(ns ru.petrsu.nest.yz.yz
  ^{:author "Vyacheslav Dimitrov"
    :doc "Wrapper for using the YZ from Java code."}
  (:require
   (ru.petrsu.nest.yz [core :as yz] [hb-utils :as hu]))
  (:use ru.petrsu.nest.yz.yz-factory)
  (:import
    (javax.persistence EntityManager)
    (ru.petrsu.nest.yz.core ElementManager))
  (:gen-class :name ru.petrsu.nest.yz.QueryYZ
              :constructors {[ru.petrsu.nest.yz.core.ElementManager] [], 
                             [ru.petrsu.nest.yz.core.ElementManager String] []}
              :methods [[getResultList [String] java.util.List]
                        [getSingleResult [String] Object]
                        [getResult [String] java.util.Map]
                        [getStructuredResult [] java.util.List]
                        [getError [] String]
                        [getColumnsName [] java.util.List]

                        ^{:static true} 
                        [createCollectionQueryYZ
                         [java.util.Collection]
                         ru.petrsu.nest.yz.QueryYZ]

                        ^{:static true} 
                        [createCollectionQueryYZ
                         [java.util.Collection java.util.Collection]
                         ru.petrsu.nest.yz.QueryYZ]]
              :state state
              :init init))

;; Defines map of the object model (MOM).
(def ^:dynamic *mom* (atom nil))


(defn- get-by-key
  "Returns a value from the state for the specified key."
  [key, this]
  (key @(.state this)))


(defn- create-state
  "Creates state due to em. If f-mom isn't nil then
  MOM is extracted from file."
  [^ElementManager em, f-mom]
  (atom {:em em 
         :mom (if (nil? @*mom*) 
                (reset! *mom* 
                        (if (nil? f-mom)
                          (hu/gen-mom (.getClasses em) {})
                          (hu/mom-from-file f-mom)))
                @*mom*)
         :res nil}))


(defn -init
  "Defines constructors."
  ([^ElementManager em]
   [[] (create-state em nil)])
  ([^ElementManager em ^String f]
   [[] (create-state em f)]))


(defn- pq
  "Performs YZ's query."
  [this ^String query]
  (let [res (yz/pquery query (get-by-key :mom this) (get-by-key :em this))
        _ (reset! (.state this) (assoc @(.state this) :res res))]
    (if (nil? (:error res))
      res
      (throw (Exception. (:error res))))))


(defn -getResult
  "Returns a map of the result query."
  [this, ^String query]
  (pq this query))


(defn -getResultList
  "Returns rows of query's result."
  [this, ^String query]
  (:rows (pq this query)))


(defn -getStructuredResult
  "Returns a value of :result key from map of the result query."
  [this]
  (:result (get-by-key :res this)))


(defn -getError
  "Returns value of :error key from query's result."
  [this]
  (:error (get-by-key :res this)))


(defn -getColumnsName
  "Returns value of :columns key from query's result."
  [this]
  (:columns (get-by-key :res this)))


(defn -getSingleResult
  "Returns single result. If result is not single then
  exception is thrown."
  [this, ^String query]
  (let [rows (:rows (pq this query))]
    (if (or (not= (count rows) 1) (not= (count (nth rows 0)) 1))
      (throw (Exception. "Result is not single."))
      (nth (nth rows 0) 0))))


(defn -createCollectionQueryYZ
  "Returns instance of the QueryYZ 
  which works with collection."
  ([^java.util.Collection coll]
   (ru.petrsu.nest.yz.QueryYZ. (c-em coll nil)))
  ([^java.util.Collection coll ^java.util.Collection classes]
   (ru.petrsu.nest.yz.QueryYZ. (c-em coll classes))))
