(ns ru.petrsu.nest.yz.test-core
  ^{:author Vyacheslav Dimitrov
    :doc "Tests for the core of the implementation of YZ."}
  (:use ru.petrsu.nest.yz.core clojure.contrib.test-is)
  (:require [ru.petrsu.nest.yz.hb-utils :as hb])
  (:import (javax.persistence EntityManagerFactory Persistence EntityManager)
           (ru.petrsu.nest.son SON Building Room Floor)))


(def 
  ^{:doc "The map of the Nest object model."}
  mom (hb/gen-mom-from-cfg "test-resources/hibernate.cfg.xml"))

(def son (doto (SON.) (.addBuilding (Building.)) (.addBuilding (Building.))))

(deftest simple-query-son
         ^{:doc ""}
         (let [em (.createEntityManager (Persistence/createEntityManagerFactory "nest"))
               _ (do (.. em getTransaction begin) (.persist em son) (.. em getTransaction commit))]
           (is (= 2 (count (run-query "building" mom em))))))


(def son1 (doto (SON.) (.addBuilding (Building.))))

(deftest simple-query-son-1
         ^{:doc ""}
         (let [em (.createEntityManager (Persistence/createEntityManagerFactory "nest"))
               _ (do (.. em getTransaction begin) (.persist em son1) (.. em getTransaction commit))]
           (is (= 1 (count (run-query "building" mom em))))))

