(ns ru.petrsu.nest.yz.core
  ^{:author Vyacheslav Dimitrov
    :doc "This code contains core functions of the Clojure's implementation of the YZ language.
         The Parsing of queries does due to the fnparse library.
         See the code for the parsing queries in the parsing.clj file."}
  (:require [ru.petrsu.nest.yz.parsing :as p])
  (:import (javax.persistence.criteria CriteriaQuery)))


(defn run-query
  "Returns result of 'query' based on specified map of object model ('mom')
  and instance of javax.persistence.EntityManager ('em')."
  [query mom em]
  (let [parse-res (p/parse query mom)
        cr (.. em getCriteriaBuilder createTupleQuery)
        root (. cr (from (:what (parse-res 0)) ))]
    (.. em (createQuery (doto cr (.multiselect [root]))) getResultList)))
