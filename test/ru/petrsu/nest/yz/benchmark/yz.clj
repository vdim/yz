;;
;; Copyright 2011 Vyacheslav Dimitrov <vyacheslav.dimitrov@gmail.com>
;;
;; This file is part of YZ.
;;
;; YZ is free software: you can redistribute it and/or modify it
;; under the terms of the GNU Lesser General Public License version 3
;; only, as published by the Free Software Foundation.
;;
;; YZ is distributed in the hope that it will be useful, but
;; WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
;; Lesser General Public License for more details.
;;
;; You should have received a copy of the GNU Lesser General Public
;; License along with YZ.  If not, see
;; <http://www.gnu.org/licenses/>.
;;

(ns ru.petrsu.nest.yz.benchmark.yz
    ^{:author "Vyacheslav Dimitrov"
          :doc "YZ queries for benchmark."}
  (:use ru.petrsu.nest.yz.core)
  (:require [ru.petrsu.nest.yz.hb-utils :as hu]
            [ru.petrsu.nest.yz.benchmark.bd-utils :as bu]))


(def queries
    ["building" 
     "room" 
     "building (room)" 
     "b (li)"
     "floor#(number = 1)"
     "floor#(number = 1) (room#(number = \"215\"))"
     "d#(forwarding = true)"
     "device#(network.device.id = 25 and forwarding = true)"
     "device (room (building#(name=\"MB\")))"
     "device (room#(number=\"200\") (building#(name=\"MB\")))"])


(defn- run-yz
  "Runs specified yz's queries and returns time of executing query."
  [q em mom]
  (bu/btime (pquery q mom em)))


(defn do-q
  "Takes a number of query from 'queries array' and a name of the persistense unit,
  executes query, and prints time of executing query."
  [num, n, m]
  (let [em (.createEntityManager (javax.persistence.Persistence/createEntityManagerFactory n m))
        mom (hu/mom-from-file "nest.mom")]
    (println (run-yz (queries (Integer/parseInt num)) em mom))
    (.close em)))

