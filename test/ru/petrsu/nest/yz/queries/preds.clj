(ns ru.petrsu.nest.yz.queries.preds
  ^{:author Vyacheslav Dimitrov
    :doc "Processes queries within some restrictions."}
  (:use ru.petrsu.nest.yz.core 
        clojure.contrib.test-is)
  (:require [ru.petrsu.nest.yz.queries.core :as tc])
  (:import (ru.petrsu.nest.son SON Building Room Floor)))


;; Define model

(def son (doto (SON.)
           (.addBuilding (doto (Building.) (.setName "b1") 
                           (.addFloor (Floor.)) (.addFloor (Floor.)))) 
           (.addBuilding (doto (Building.) (.setName "b2") 
                           (.addFloor (Floor.))))))




;; Define entity manager.

(use-fixtures :once (tc/setup [son]))


;; Define tests

(deftest select-b1
         ^{:doc "Selects all Floor and Building objects."}
         (is (tc/check-query (run-query "building#(name=\"b1\")" tc/mom tc/*em*) 
                             [[Building []]])))

