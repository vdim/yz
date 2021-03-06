;;
;; Copyright 2011-2013 Vyacheslav Dimitrov <vyacheslav.dimitrov@gmail.com>
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
  (:require [ru.petrsu.nest.yz.benchmark.bd-utils :as bu] 
            [ru.petrsu.nest.yz.benchmark.bd-utils-jpa :as buj]
            [ru.petrsu.nest.yz.queries.bd-jpa :as bjpa]
            [incanter.core :as ic]
            [ru.petrsu.nest.yz.queries.core :as qc] 
            [ru.petrsu.nest.yz.jpa-em.core :as hem])
  (:use ru.petrsu.nest.yz.mom-utils
        ru.petrsu.nest.yz.queries.bd
        ru.petrsu.nest.yz.queries.uni-bd
        ru.petrsu.nest.yz.benchmark.benchmark
        ru.petrsu.nest.yz.parsing
        ru.petrsu.nest.yz.core
        ru.petrsu.nest.yz.yz-factory
        ru.petrsu.nest.yz.utils
        ru.petrsu.nest.son.local-sm
        
        clojure.repl clojure.pprint clojure.java.io
        clojure.walk
        clojure.core.protocols)

  (:import (ru.petrsu.nest.son SonElement SON, SpatialElement, Building, Room, Floor,
                               Device, Network, IPNetwork, UnknownNetwork,
                               NetworkElement, NetworkInterface, IPv4Interface, UnknownNetworkInterface,
                               EthernetInterface, LinkInterface, VLANInterface, UnknownLinkInterface,
                               OrganizationalElement, AbstractOU, CompositeOU, SimpleOU, Occupancy)
           (university.model Course, Faculty, Student Company Person Address)))

;; MOM of the SON model from the Nest project (lsm version).
(def i-mom (mom-from-file "nest.mom"))

;; MOM of the SON model from the Nest project (jpa version).
(def jpa-mom (mom-from-file "nest_jpa.mom"))

;; Create JPA element manager from the bjpa/jpa-em EntityManager.
(def jpa-em (hem/-createJPAElementManager bjpa/jpa-em))

;; Vector with all classes from SON model 
;; (Sometimes It is needed for testing functions from the mom-utils namespace.)
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

;; Vector with all classes from University model.
(def uni-classes
  [Course, Faculty, Student, Company, Person, Address])

;; MOM of the University model
(def uni-mom (gen-mom uni-classes))

;; Vector with all classes from extented SON model.
(def ex-classes
  [ru.petrsu.nest.yz.yz_son.SonElement 
   ru.petrsu.nest.yz.yz_son.SON 
   ru.petrsu.nest.yz.yz_son.Occupancy 
   ru.petrsu.nest.yz.yz_son.NetworkElement 
   ru.petrsu.nest.yz.yz_son.Device 
   ru.petrsu.nest.yz.yz_son.LinkInterface 
   ru.petrsu.nest.yz.yz_son.UnknownLinkInterface 
   ru.petrsu.nest.yz.yz_son.EthernetInterface 
   ru.petrsu.nest.yz.yz_son.VLANInterface 
   ru.petrsu.nest.yz.yz_son.NetworkInterface 
   ru.petrsu.nest.yz.yz_son.UnknownNetworkInterface 
   ru.petrsu.nest.yz.yz_son.IPv4Interface 
   ru.petrsu.nest.yz.yz_son.Network 
   ru.petrsu.nest.yz.yz_son.UnknownNetwork 
   ru.petrsu.nest.yz.yz_son.IPNetwork 
   ru.petrsu.nest.yz.yz_son.OrganizationalElement 
   ru.petrsu.nest.yz.yz_son.AbstractOU 
   ru.petrsu.nest.yz.yz_son.SimpleOU 
   ru.petrsu.nest.yz.yz_son.MiddleOU 
   ru.petrsu.nest.yz.yz_son.CompositeOU 
   ru.petrsu.nest.yz.yz_son.SpatialElement 
   ru.petrsu.nest.yz.yz_son.Room 
   ru.petrsu.nest.yz.yz_son.Floor 
   ru.petrsu.nest.yz.yz_son.Building
   ru.petrsu.nest.yz.yz_son.NonSensicalElement
   ru.petrsu.nest.yz.yz_son.Some])

;; MOM of the extented SON model.
(def ex-mom (gen-mom ex-classes))

;; Vector with all classes from SON model (jpa version).
(def jpa-classes
  [ru.petrsu.nest.son.jpa.Room 
   ru.petrsu.nest.son.jpa.Floor 
   ru.petrsu.nest.son.jpa.Building

   ru.petrsu.nest.son.jpa.Occupancy 

   ru.petrsu.nest.son.jpa.AbstractOU 
   ru.petrsu.nest.son.jpa.SimpleOU 
   ru.petrsu.nest.son.jpa.CompositeOU 

   ru.petrsu.nest.son.jpa.Device 
   ru.petrsu.nest.son.jpa.NetworkInterface 
   ru.petrsu.nest.son.jpa.IPv4Interface 
   ru.petrsu.nest.son.jpa.LinkInterface 
   ru.petrsu.nest.son.jpa.VLANInterface 
   ru.petrsu.nest.son.jpa.EthernetInterface 
   ru.petrsu.nest.son.jpa.Network 
   ru.petrsu.nest.son.jpa.IPNetwork 
   
   ru.petrsu.nest.son.jpa.SonElement 
   ru.petrsu.nest.son.jpa.SON 
   
   ru.petrsu.nest.son.jpa.AbstractSonModificationOccurence 
   ru.petrsu.nest.son.jpa.SonPropertyModificationOccurence 
   ru.petrsu.nest.son.jpa.SonReferenceModificationOccurence])

(def son-matrix
  ; B F R O SOU COU D LI VLAN NI N
  [[0 1 0 0 0 0 0 0 0 0 0]
   [1 0 1 0 0 0 0 0 0 0 0]
   [0 1 0 1 0 0 0 0 0 0 0]
   [0 0 1 0 1 0 1 0 0 0 0]
   [0 0 0 1 0 1 0 0 0 0 0]
   [0 0 0 1 0 1 0 0 0 0 0]
   [0 0 0 0 1 1 0 0 0 0 0]
   [0 0 0 0 0 0 1 1 1 1 0]
   [0 0 0 0 0 0 0 1 0 0 0]
   [0 0 0 0 0 0 0 1 0 0 1]
   [0 0 0 0 0 0 0 0 0 1 0]])

(def deg
  ; 
  [[1 0 0 0 0 0 0 0 0 0 0]
   [0 2 0 0 0 0 0 0 0 0 0]
   [0 0 2 0 0 0 0 0 0 0 0]
   [0 0 0 3 0 0 0 0 0 0 0]
   [0 0 0 0 2 0 0 0 0 0 0]
   [0 0 0 0 0 2 0 0 0 0 0]
   [0 0 0 0 0 0 2 0 0 0 0]
   [0 0 0 0 0 0 0 4 0 0 0]
   [0 0 0 0 0 0 0 0 1 0 0]
   [0 0 0 0 0 0 0 0 0 2 0]
   [0 0 0 0 0 0 0 0 0 0 2]])

(def son-d (ic/det (ic/minus deg son-matrix)))
