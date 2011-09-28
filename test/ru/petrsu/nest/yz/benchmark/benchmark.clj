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

(defn- create-em
  "Creates entity manager, clean database 
  and generates some database's structure."
  []
  (let [emb (.createEntityManager (javax.persistence.Persistence/createEntityManagerFactory "bench"))]
    (do (bu/schema-export)
      (bu/create-bd 1000 emb))
    emb))


(defn run-query
  "Runs specified query ('q') due to specified function ('f')
  which takes string-query and some EntityManager."
  [f q em]
  (if (nil? em)
   (let [em (create-em)
         t (run-query f q em)]
     (.close em)
     t)
    (btime (f q em))))


(def queries
  [{:func run-hql
    :queries ["from Building" 
              "from Room" 
              "select b, r from Building as b left join b.floors as f left join f.rooms as r"]}
   {:func run-yz
    :queries ["building" "room" "building (room)"]}])


(def ncount
  ^{:doc "Defines amount of evaluating set of queries. "}
  (identity 5))

(defn run-queries
  "Runs all queries."
  [em]
  (loop [n ncount, res []]
    (if (= n 0)
      res
      (recur (dec n) 
             (conj res (map (fn [qs] 
                              (vec (map #(run-query (:func qs) % em)
                                        (:queries qs)))) queries))))))


(defn- add-seq
  "Adds two collection."
  [coll1 coll2]
  (map #(vec (map + %1 %2)) coll1 coll2))


(defn avg
  "Calculates average value of evaluating queries."
  ([]
   (avg nil))
  ([em]
   (let [times (run-queries em)
         sums (reduce #(if (empty? %1) %2 (add-seq %1 %2)) [] times)]
     (map #(vec (map (fn [x] (/ x ncount)) %)) sums))))

