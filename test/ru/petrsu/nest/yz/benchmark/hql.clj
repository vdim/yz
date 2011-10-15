(ns ru.petrsu.nest.yz.benchmark.hql
    ^{:author "Vyacheslav Dimitrov"
          :doc "HQL queries for benchmark."}
    (:require [ru.petrsu.nest.yz.benchmark.benchmark :as b]))

(def queries
  ["from Building" 
   "from Room" 
   "select b, r from Building as b left join b.floors as f left join f.rooms as r"
   "select b, li from Building as b left join b.floors as f left join f.rooms as r 
           left join r.occupancies as o left join o.devices as d left join d.linkInterfaces as li"
   "select f from Floor as f where f.number=1"
   "select f, r from Floor as f left join f.rooms as r where f.number=1 and r.number='215'"])

(defn -main
  [n]
  (let [em (.createEntityManager (javax.persistence.Persistence/createEntityManagerFactory "bench"))]
    (println (b/run-hql (queries (Integer/parseInt n)) em))
    (.close em)))
