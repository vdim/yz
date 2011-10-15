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
