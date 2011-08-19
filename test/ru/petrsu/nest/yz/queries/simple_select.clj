(ns ru.petrsu.nest.yz.queries.simple-select
  ^{:author Vyacheslav Dimitrov
    :doc "Processes simple select queries."}
  (:use ru.petrsu.nest.yz.core 
        clojure.contrib.test-is)
  (:require [ru.petrsu.nest.yz.queries.core :as tc])
  (:import (ru.petrsu.nest.son SON Building Room Floor)))


(def son (doto (SON.)
           (.addBuilding (doto (Building.) (.addFloor (Floor.)) (.addFloor (Floor.)))) 
           (.addBuilding (doto (Building.) (.addFloor (Floor.))))))

(def em (tc/create-em [son]))

(deftest select-buildings
         ^{:doc "Selects all Building objects."}
         (let [q (run-query "building" tc/mom em)]
           (is (= 2 (count q)))
           (is (= 1 (count (.toArray (nth q 0)))))))

(deftest select-floors
         ^{:doc "Selects all Floor objects."}
         (let [q (run-query "floor" tc/mom em)]
           (is (= 3 (count q)))
           (is (= 1 (count (.toArray (nth q 0)))))))

