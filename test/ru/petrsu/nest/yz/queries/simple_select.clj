(ns ru.petrsu.nest.yz.queries.simple-select
  ^{:author Vyacheslav Dimitrov
    :doc "Processes simple select queries."}
  (:use ru.petrsu.nest.yz.core 
        clojure.contrib.test-is)
  (:require [ru.petrsu.nest.yz.queries.core :as tc])
  (:import (ru.petrsu.nest.son SON Building Room Floor)))


;; Define model

(def son (doto (SON.)
           (.addBuilding (doto (Building.) (.addFloor (Floor.)) (.addFloor (Floor.)))) 
           (.addBuilding (doto (Building.) (.addFloor (Floor.))))))


;; Define entity manager.

(declare *em*)
(defn setup [f]
    (binding [*em* (tc/create-em [son])] (f) 
      (.close *em*)))

(use-fixtures :once setup)


;; Define tests

(deftest select-buildings
         ^{:doc "Selects all Building objects.
                Result should be (for our son) like this: [{#<Building Building> [], #<Building Building> []}]"}
         (let [q (run-query "building" tc/mom *em*)]
           (is (= 1 (count q)))
           (is (= 2 (count (q 0))))
           (is (every? empty? (vals (q 0))))
           (is (every? #(instance? Building %) (keys (q 0))))))

(deftest select-floors
         ^{:doc "Selects all Floor objects.
                Result should be like this: 
                [{#<Floor Floor 2> [], #<Floor Floor 1> [], 
                #<Floor Floor 3> [], #<Floor Floor 2> [], #<Floor Floor 1> []}]"}
         (let [q (run-query "floor" tc/mom *em*)]
           (is (= 1 (count q)))
           (is (= 3 (count (q 0))))
           (is (every? empty? (vals (q 0))))
           (is (every? #(instance? Floor %) (keys (q 0))))))

