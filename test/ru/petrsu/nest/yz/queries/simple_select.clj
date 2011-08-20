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
           (is (= 1 (count q)))
           (is (= 2 (count (q 0))))
           (is (= 1 (count (.toArray (nth (q 0) 0)))))))

(deftest select-floors
         ^{:doc "Selects all Floor objects."}
         (let [q (run-query "floor" tc/mom em)]
           (is (= 1 (count q)))
           (is (= 3 (count (q 0))))
           (is (= 1 (count (.toArray (nth (q 0) 0)))))))

(deftest select-floors-and-building
         ^{:doc "Selects all Floor and Building objects."}
         (let [q (run-query "floor, building" tc/mom em)]
           (is (= 2 (count q)))
           (is (= 3 (count (q 0))))
           (is (= 2 (count (q 1))))
           (is (= 1 (count (.toArray (nth (q 1) 0)))))
           (is (= 1 (count (.toArray (nth (q 0) 0)))))))

