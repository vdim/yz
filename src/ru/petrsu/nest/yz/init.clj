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
  (:require [ru.petrsu.nest.yz.benchmark.bd-utils :as bu])
  (:use ru.petrsu.nest.yz.hb-utils
        ru.petrsu.nest.yz.queries.bd
        ru.petrsu.nest.yz.benchmark.benchmark
        ru.petrsu.nest.yz.parsing
        ru.petrsu.nest.yz.core
        
        clojure.repl clojure.pprint clojure.java.io
        clojure.walk
        clojure.core.protocols)

  (:import (ru.petrsu.nest.son SON Building Room Floor
                               Device, IPNetwork, EthernetInterface, 
                               IPv4Interface, UnknownLinkInterface,
                               CompositeOU, SimpleOU, Occupancy)))

;; MOM the SON model from the Nest project.
(def i-mom (mom-from-file "nest.mom"))

;; Vector with all classes from SON model 
;; (Sometimes It is needed for testing functions from the hb-utils namespace.)
(def classes
  [ru.petrsu.nest.son.SonElement 
   ru.petrsu.nest.son.SON 
   ru.petrsu.nest.son.Occupancy 
   ru.petrsu.nest.son.NetworkElement 
   ru.petrsu.nest.son.Device 
   ru.petrsu.nest.son.LinkInterface 
   ru.petrsu.nest.son.UnknownLinkInterface 
   ru.petrsu.nest.son.EthernetInterface 
   ru.petrsu.nest.son.VLANInterface 
   ru.petrsu.nest.son.NetworkInterface 
   ru.petrsu.nest.son.UnknownNetworkInterface 
   ru.petrsu.nest.son.IPv4Interface 
   ru.petrsu.nest.son.Network 
   ru.petrsu.nest.son.UnknownNetwork 
   ru.petrsu.nest.son.IPNetwork 
   ru.petrsu.nest.son.OrganizationalElement 
   ru.petrsu.nest.son.AbstractOU 
   ru.petrsu.nest.son.SimpleOU 
   ru.petrsu.nest.son.CompositeOU 
   ru.petrsu.nest.son.SpatialElement 
   ru.petrsu.nest.son.Room 
   ru.petrsu.nest.son.Floor 
   ru.petrsu.nest.son.Building])
