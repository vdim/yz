(ns ru.petrsu.nest.yz.benchmark.yz
    ^{:author "Vyacheslav Dimitrov"
          :doc "YZ queries for benchmark."}
    (:require [ru.petrsu.nest.yz.hb-utils :as hu]
              [ru.petrsu.nest.yz.benchmark.benchmark :as b]))



(def queries
    ["building" 
     "room" 
     "building (room)" 
     "b (li)"
     "floor#(number=1)"
     "floor#(number=1) (room#(number=\"215\"))"])

(defn -main
  [n]
  (let [em (.createEntityManager (javax.persistence.Persistence/createEntityManagerFactory "bench"))
        mom (hu/mom-from-file "nest.mom")]
    (println (b/run-yz (queries (Integer/parseInt n)) em mom))
    (.close em)))
