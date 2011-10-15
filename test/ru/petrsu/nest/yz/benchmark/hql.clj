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
   "select f, r from Floor as f left join f.rooms as r where f.number=1 and r.number='215'"
   "select d from Device as d where d.forwarding=true"
   "select router from Device as d 
          left join d.linkInterfaces as lis
          left join lis.networkInterfaces as nis
          left join nis.network.networkInterfaces as nis2
          left join nis2.linkInterface.device as router
          where router.forwarding = true and d.id = 25"])


(defn- do-q
  "Takes a number of query from 'queries array' and a name of the persistense unit,
  executes query, ant returns time of executing query."
  [num, n]
  (let [em (.createEntityManager (javax.persistence.Persistence/createEntityManagerFactory n))]
    (println (b/run-hql (queries (Integer/parseInt num)) em))
    (.close em)))


(defn -main
  "Takes a number of query and returns time of executing query.
  If name of persistense unit is not supplied then \"bench\" is used."
  ([num]
   (do-q num, "bench"))
  ([num, n]
   (do-q num, n)))

