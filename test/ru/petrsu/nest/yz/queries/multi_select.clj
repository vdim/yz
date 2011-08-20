(ns ru.petrsu.nest.yz.queries.multi-select
  ^{:author Vyacheslav Dimitrov
    :doc "Processes queries within multi selects like this:
         building, room"}
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

(deftest select-floors-and-building
         ^{:doc "Selects all Floor and Building objects."}
         (let [q (run-query "floor, building" tc/mom *em*)]
           (is (= 2 (count q)))
           (is (= 3 (count (q 0))))
           (is (= 2 (count (q 1))))
           (is (every? empty? (vals (q 0))))
           (is (every? empty? (vals (q 1))))
           (is (every? #(instance? Floor %) (keys (q 0))))
           (is (every? #(instance? Building %) (keys (q 1))))))

