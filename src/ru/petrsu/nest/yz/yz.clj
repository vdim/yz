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

(ns ru.petrsu.nest.yz.yz
  ^{:author "Vyacheslav Dimitrov"
    :doc "Wrapper for using the YZ into the Java code."}
  (:require
   (ru.petrsu.nest.yz [core :as yz] [hb-utils :as hu]))
  (:import
    (javax.persistence EntityManager))
  (:gen-class :name ru.petrsu.nest.yz.QueryYZ
              :constructors {[javax.persistence.EntityManager], []
                             [javax.persistence.EntityManager String], []}
              :methods [[getResultList [String] java.util.List]
                        [getResult [String] java.util.Map]
                        [getError [] String]
                        [getColumnsName [] java.util.List]]
              :state state
              :init init))

;; Defines map of the object model (MOM).
(def ^:dynamic *mom* (atom nil))


(defn- get-by-key
  "Returns a value getting from the state for the specified key."
  [key, this]
  (key @(.state this)))

(defn- create-state
  "Creates state due to em. If f-mom is'not nil then
  mom is extracted from file."
  [em, f-mom]
  (atom {:em em 
         :mom (if (nil? @*mom*) 
                (reset! *mom* 
                        (if (nil? f-mom)
                          (hu/gen-mom-from-metamodel (.getEntityManagerFactory em))
                          (hu/mom-from-file f-mom)))
                @*mom*)
         :res nil}))

(defn -init
  "Defines constructors."
  ([^EntityManager em]
   [[] (create-state em nil)])

  ([^EntityManager em ^String f]
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


(defn -getError
  "Returns value of :error key from query's result."
  [this]
  (:error (get-by-key :res this)))


(defn -getColumnsName
  "Returns value of :columns key from query's result."
  [this]
  (:columns (get-by-key :res this)))
