(ns ru.petrsu.nest.yz.benchmark.benchmark
  ^{:author "Vyacheslav Dimitrov"
    :doc "System for benchmarking different types of queries."}
  (:use ru.petrsu.nest.yz.core)
  (:require [ru.petrsu.nest.yz.benchmark.bd-utils :as bu]
            [ru.petrsu.nest.yz.hb-utils :as hb]))


(declare momb)
(defmacro btime
  "Like Clojure's macros time, but doesn't have side effect 
  (something prints) and returns time which is taken for
  evaluating an expr."
  [expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr]
     (/ (double (- (. System (nanoTime)) start#)) 1000000.0)))


(defn run-yz
  "Runs specified yz's queries."
  [q em]
  (if (nil? (resolve (symbol "momb")))
    (def momb (hb/gen-mom-from-metamodel em)))
  (btime (pquery q momb em)))


(defn run-hql
  "Runs specified HQL's queries."
  [q em]
  (btime (.. em (createQuery q) getResultList)))

(defn- create-em
  "Returns EntityManager due to specified name (bench is default)."
  ([]
   (create-em "bench"))
  ([n]
   (.createEntityManager (javax.persistence.Persistence/createEntityManagerFactory n))))


(defn get-clean-bd
  "Creates entity manager, clean database 
  and generates some database's structure."
  []
  (let [emb (create-em)]
    (do (bu/schema-export)
      (bu/create-bd 1000 emb))
    emb))


(defn run-query
  "Runs specified query ('q') due to specified function ('f')
  which takes string-query and some EntityManager."
  [f q em]
  (if (nil? em)
   (let [em (get-clean-bd)
         t (run-query f q em)]
     (.close em)
     t)
    (f q em)))


(def queries
  [{:func run-hql
    :queries ["from Building" 
              "from Room" 
              "select b, r from Building as b left join b.floors as f left join f.rooms as r"
              "select b, li from Building as b left join b.floors as f left join f.rooms as r 
              left join r.occupancies as o left join o.devices as d left join d.linkInterfaces as li"
              "select f from Floor as f where f.number=1"
              "select f, r from Floor as f left join f.rooms as r where f.number=1 and r.number='215'"]}
   {:func run-yz
    :queries ["building" "room" "building (room)" "b (li)"
              "floor#(number=1)"
              "floor#(number=1) (room#(number=\"215\"))"]}])


(def ncount
  ^{:doc "Defines amount of evaluating set of queries. "}
  (identity 100))

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


(defn avg-each
  "Calculates average value of evaluating queries. 
   Creates and cleans bd for each query."
  ([]
   (avg-each nil))
  ([em]
   (let [times (run-queries em)
         sums (reduce #(if (empty? %1) %2 (add-seq %1 %2)) [] times)]
     (map #(vec (map (fn [x] (/ x ncount)) %)) sums))))


(defn avg-one
  "Like avg-each, but creates and cleans bd one times."
  []
  (avg-each (get-clean-bd)))


(defn avg-exist
  "Evaluates averages values for existing database."
  []
  (avg-each (create-em)))

