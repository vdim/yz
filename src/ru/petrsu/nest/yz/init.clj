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


(ns ru.petrsu.nest.yz.init
  ^{:author "Vyacheslav Dimitrov"
    :doc "Code for starting repl."}
  (:use ru.petrsu.nest.yz.functions 
        ru.petrsu.nest.yz.hb-utils
        ru.petrsu.nest.yz.queries.bd
        ru.petrsu.nest.yz.benchmark.benchmark
        ru.petrsu.nest.yz.parsing
        ru.petrsu.nest.yz.core
        
        clojure.repl)
  (:import (ru.petrsu.nest.son SON Building Room Floor
                               Device, IPNetwork, EthernetInterface, 
                               IPv4Interface, UnknownLinkInterface,
                               CompositeOU, SimpleOU, Occupancy)))

(def i-mom (mom-from-file "nest.mom"))
