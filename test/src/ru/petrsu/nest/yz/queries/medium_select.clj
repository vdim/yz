;;
;; Copyright 2012-2013 Vyacheslav Dimitrov <vyacheslav.dimitrov@gmail.com>
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

(ns ru.petrsu.nest.yz.queries.medium-select
  ^{:author "Vyacheslav Dimitrov"
    :doc "Tests queries with selecting all medium object 
         something like this: building->room."}
  (:use ru.petrsu.nest.yz.core 
        clojure.test)
  (:require [ru.petrsu.nest.yz.queries.core :as tc] 
            [ru.petrsu.nest.util.utils :as f]
            [ru.petrsu.nest.yz.queries.bd :as bd])
  (:import (ru.petrsu.nest.son 
             SON, Building, Room, Floor, Network,
             Device, IPNetwork, EthernetInterface, NetworkInterface,
             LinkInterface, IPv4Interface, UnknownLinkInterface)
           (ru.petrsu.nest.yz NotFoundElementException)))

;; Define entity manager.

(use-fixtures :once (tc/setup-son bd/son))


;; Define tests

(deftest medium-select
         (is (tc/eq-colls (tc/rows-query "building->room")
                          [[bd/b1 bd/f1_b1 bd/r101_f1_b1]
                           [bd/b1 bd/f1_b1 bd/r102_f1_b1]
                           [bd/b1 bd/f2_b1 bd/r201_f2_b1]
                           [bd/b1 bd/f2_b1 bd/r202_f2_b1]
                           [bd/b2 bd/f1_b2 bd/r1001_f1_b2]
                           [bd/b2 bd/f1_b2 bd/r1002_f1_b2]]))
         (is (tc/eq-colls (tc/rows-query "building->room#(number=\"1001\")")
                           [[bd/b1]
                           [bd/b2 bd/f1_b2 bd/r1001_f1_b2]]))
         (is (tc/eq-colls (tc/rows-query "building->room#(number=\"1001\") (floor)")
                           [[bd/b1]
                           [bd/b2 bd/f1_b2 bd/r1001_f1_b2 bd/f1_b2]]))
         (is (tc/eq-colls (tc/rows-query "ni#(inetAddress=@(ip2b \"172.20.255.109\")).building->room")
                           [[bd/b2 bd/f1_b2 bd/r1001_f1_b2]
                            [bd/b2 bd/f1_b2 bd/r1002_f1_b2]]))
         (is (tc/eq-colls (tc/rows-query "floor (building->room#(number=\"1001\"))")
                           [[bd/f1_b1 bd/b1] 
                            [bd/f2_b1 bd/b1]
                            [bd/f3_b1 bd/b1]
                            [bd/f1_b2 bd/b2 bd/f1_b2 bd/r1001_f1_b2]]))
         (is (tc/eq-colls (tc/rows-query "floor.building->room#(number=\"1001\")")
                           [[bd/b1] 
                            [bd/b1]
                            [bd/b1]
                            [bd/b2 bd/f1_b2 bd/r1001_f1_b2]])))
