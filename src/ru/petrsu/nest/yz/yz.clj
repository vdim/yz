(ns ru.petrsu.nest.yz.yz
  ^{:author "Vyacheslav Dimitrov"
    :doc "Wrapper for using the YZ into the Java code."}
  (:require
   (ru.petrsu.nest.yz [core :as yz] [hb-utils :as hu]))
  (:import
    (javax.persistence EntityManager))
  (:gen-class :name ru.petrsu.nest.yz.QueryYZ
              :constructors {[javax.persistence.EntityManager], []}
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


(defn -init
  [^EntityManager em]
  [[] (atom {:em em 
             :mom (if (nil? @*mom*) 
                    (reset! *mom* (hu/gen-mom-from-metamodel (.getEntityManagerFactory em)))
                    @*mom*)
             :res nil})])


(defn- pq
  "Performs YZ's query."
  [this ^String query]
  (let [res (yz/pquery query (get-by-key :mom this) (get-by-key :em this))]
    (if (nil? (:error res))
      (do (reset! (.state this) (assoc @(.state this) :res res)) res)
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
