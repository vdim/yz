(ns ru.petrsu.nest.yz.queries.preds
  ^{:author Vyacheslav Dimitrov
    :doc "Processes queries within some restrictions."}
  (:use ru.petrsu.nest.yz.core)
  (:require [ru.petrsu.nest.yz.benchmark.bd-utils :as bu]
            [ru.petrsu.nest.yz.hb-utils :as hb]))


(def momb (hb/gen-mom-from-cfg "test-resources/hibench.cfg.xml"))

(defmacro btime
  ""
  [expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr]
     (/ (double (- (. System (nanoTime)) start#)) 1000000.0)))

(defn run-yz
  "Runs specified yz's queries."
  [q em]
  (pquery q momb em))


(defn run-hql
  "Runs specified HQL's queries."
  [q em]
  (.. em (createQuery q) getResultList))

(defn run-query
  ""
  [f q]
  (let [emb (.createEntityManager (javax.persistence.Persistence/createEntityManagerFactory "bench"))
        t (do (bu/schema-export)
            (bu/create-bd 1000)
            (btime (f q emb)))]
    (.close emb)
    t))


(def queries
  [{:func run-hql
    :queries ["from Building" 
              "from Room" 
              "select b, r from Building as b left join b.floors as f left join f.rooms as r"]}
   {:func run-yz
    :queries ["building" "room" "building (room)"]}])


(defn run-queries
  "Runs all queries."
  []
  (map (fn [qs] (map #(run-query (:func qs) %) (:queries qs))) queries))
