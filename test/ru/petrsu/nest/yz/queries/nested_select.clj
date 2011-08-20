(ns ru.petrsu.nest.yz.queries.nested-select
  ^{:author Vyacheslav Dimitrov
    :doc "Processes queries nested statements like this: 
         object1 (object2 (object3))."}
  (:use ru.petrsu.nest.yz.core 
        clojure.contrib.test-is)
  (:require [ru.petrsu.nest.yz.queries.core :as tc])
  (:import (ru.petrsu.nest.son SON Building Room Floor)))

;; Define model

(def b1 (doto (Building.) (.addFloor (Floor.)) (.addFloor (Floor.))))
(def b2 (doto (Building.) (.addFloor (Floor.))))

(def son (doto (SON.)
           (.addBuilding b1) 
           (.addBuilding b2)))

;; Define entity manager.

(declare *em*)
(defn setup [f]
    (binding [*em* (tc/create-em [son])] (f) (.close *em*)))

(use-fixtures :once setup)


;; Define tests

(deftest select-floors-and-building
         ^{:doc "Selects all Building and its Floor objects."}
         (let [q (run-query "building (floor)" tc/mom *em*)]
           (is (= 1 (count q)))
           (is (= 2 (count (q 0))))
           (is (not (nil? (get (q 0) b1))))
           (is (= 1 (count (get (q 0) b1))))
           (is (= 2 (count ((get (q 0) b1) 0)))))) ; Check number of floor for object b1.

