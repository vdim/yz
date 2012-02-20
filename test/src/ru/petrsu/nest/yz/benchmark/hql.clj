;;
;; Copyright 2011-2012 Vyacheslav Dimitrov <vyacheslav.dimitrov@gmail.com>
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

(ns ru.petrsu.nest.yz.benchmark.hql
    ^{:author "Vyacheslav Dimitrov"
          :doc "HQL queries for benchmark."}
  (:use ru.petrsu.nest.yz.queries.nest-queries)
  (:require [ru.petrsu.nest.yz.benchmark.bd-utils :as bu]))


(def queries
  ["from Building" 
   "from Room" 
   "select b, r from Building as b left join b.floors as f left join f.rooms as r"
   "select r, r.floor.building from Room as r"
   "select b, li from Building as b left join b.floors as f left join f.rooms as r 
           left join r.occupancies as o left join o.devices as d left join d.linkInterfaces as li"
   "select d, d.occupancy.room.floor.building from Device as d"
   "select d, d.occupancy.room, d.occupancy.room.floor, d.occupancy.room.floor.building from Device as d"
   "select f from Floor as f where f.number=1"
   "select f, r from Floor as f left join f.rooms as r where f.number=1 and r.number='215'"
   "select d from Device as d where d.forwarding=true"
   "select router from Device as d 
          left join d.linkInterfaces as lis
          left join lis.networkInterfaces as nis
          left join nis.network.networkInterfaces as nis2
          left join nis2.linkInterface.device as router
          where router.forwarding = true and d.id = 25"
   "select d from Device as d 
          left join d.occupancy.room.floor.building as b where b.name='MB'"
   "select d, r, b from Device as d 
          left join d.occupancy.room as r 
          left join r.floor.building as b 
          where b.name='MB' and r.number='200'"
   "select d, r, b from Device as d 
          left join d.occupancy.room as r 
          left join r.floor.building as b 
          where b.name='MB' and r.number='200' and d.id=25"])

(defn- run-hql
  "Runs specified HQL's queries and returns time of executing query."
  [q em]
  (bu/btime (.. em (createQuery q) getResultList)))


(defn do-q
  "Takes a number of query from 'queries array' and a name of the persistense unit,
  executes query, and prints time of executing query."
  [num, n, m]
  (let [em (.createEntityManager (javax.persistence.Persistence/createEntityManagerFactory n, m))]
    (println (run-hql (queries (Integer/parseInt num)) em))
    (.close em)))


(def individual-queries
  "New variant of the list with HQL queries for benchmarks."
  ["from Device"
   "from Device d where d.name='Device_MB'"
   "from Device d where d.name='Device_MB' and (description='Description.' or description='Simple description.')"
   "select d, d.occupancy.room.floor.building from Device as d"
   "select b, d from Building as b left join b.floors as f left join f.rooms as r 
           left join r.occupancies as o left join o.devices as d"
   "select distinct li, ni.network, d from LinkInterface li join li.networkInterfaces ni join ni.network 
           n join n.networkInterfaces ni2 join ni2.linkInterface.device d"
   "from Device d order by d.description"
   enlivener-queries-hql
   address-info-queries-hql
   ])
