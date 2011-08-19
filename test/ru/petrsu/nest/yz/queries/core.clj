(ns ru.petrsu.nest.yz.queries.core
  ^{:author Vyacheslav Dimitrov
    :doc "Helper functions for testing YZ's queries."}
  (:use ru.petrsu.nest.yz.core clojure.contrib.test-is)
  (:require [ru.petrsu.nest.yz.hb-utils :as hb])
  (:import (javax.persistence EntityManagerFactory Persistence EntityManager)
           (ru.petrsu.nest.son SON Building Room Floor)))


(def 
  ^{:doc "The map of the object model."}
  mom (hb/gen-mom-from-cfg "test-resources/hibernate.cfg.xml"))

(defn create-em
  "Returns entity manager by specified name ('n', 'test-model' name 
  is used by default) and persists each object of model from 'elems'."
  ([elems]
   (create-em elems "test-model"))
  ([elems, n]
   (let [em (.createEntityManager (Persistence/createEntityManagerFactory n))
         _ (do (.. em getTransaction begin) 
             (dotimes [i (count elems)]
               (.persist em (elems i)))
             (.. em getTransaction commit))]
     em)))

