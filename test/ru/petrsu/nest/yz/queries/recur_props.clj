(ns ru.petrsu.nest.yz.queries.recur-props
  ^{:author "Vyacheslav Dimitrov"
    :doc "Processes queries within recur properties."}
  (:use ru.petrsu.nest.yz.core 
        clojure.test)
  (:require [ru.petrsu.nest.yz.queries.core :as tc])
  (:import (ru.petrsu.nest.son SON Occupancy SimpleOU CompositeOU)))


;; Define model

(def main_cou (doto (CompositeOU.)
                (.setName "Enterprise")))

(def it_cou (doto (CompositeOU.)
              (.setName "IT Department")
              (.setParent main_cou)))

(def men_cou (doto (CompositeOU.)
               (.setName "Menegement Department")
               (.setParent main_cou)))

(def fin_cou (doto (CompositeOU.)
               (.setName "Finance Department")
               (.setParent main_cou)))

(def bd_cou (doto (CompositeOU.)
              (.setName "Database")
              (.setParent it_cou)))

(def web_cou (doto (CompositeOU.)
              (.setName "Web-site")
              (.setParent it_cou)))

(def net_cou (doto (CompositeOU.)
              (.setName "Network")
              (.setParent it_cou)))

(def mysql_cou (doto (SimpleOU.)
              (.setName "MySQL")
              (.setParent bd_cou)))

(def oracle_cou (doto (SimpleOU.)
              (.setName "Oracle")
              (.setParent bd_cou)))

(def nosql_cou (doto (SimpleOU.)
              (.setName "NoSQL")
              (.setParent bd_cou)))

(def shop_cou (doto (SimpleOU.)
              (.setName "Online Shop")
              (.setParent web_cou)))

(def inner_cou (doto (SimpleOU.)
              (.setName "Inner Web")
              (.setParent web_cou)))

(def local_cou (doto (SimpleOU.)
              (.setName "Local Network")
              (.setParent net_cou)))

(def insure_cou (doto (CompositeOU.)
               (.setName "Insure Department")
               (.setParent fin_cou)))

(def fee_cou (doto (CompositeOU.)
               (.setName "Fee Department")
               (.setParent fin_cou)))




(def son (doto (SON.) (.setRootOU main_cou))) 



;; Define entity manager.

(use-fixtures :once (tc/setup [son]))


;; Define tests

;(deftest select-b1
;         ^{:doc "Selects all Buildings which have b1 name."}
;         (let [q (run-query "simpleou[parent]" tc/mom tc/*em*)]
;           (is (qstruct? (.getName ((q 0) 0)) "b1"))
;           (is (tc/check-query q [[Building []]]))))


