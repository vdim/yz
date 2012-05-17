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

(ns ru.petrsu.nest.yz.test-hb-utils
  ^{:author "Vyacheslav Dimitrov"
    :doc "Tests for functions which generate MOM."}
  (:use ru.petrsu.nest.yz.hb-utils clojure.test ru.petrsu.nest.yz.utils)
  (:require [ru.petrsu.nest.yz.init :as yzi])
  (:import (ru.petrsu.nest.son Floor Room Building)))

(def classes #{Floor, Room, Building})

(deftest t-get-paths
         ^{:doc "Tests the get-paths function."}
          (is (= (first (get-paths Room Building classes))
                 {:path [ru.petrsu.nest.son.Room ru.petrsu.nest.son.Floor ru.petrsu.nest.son.Building], 
                  :ppath ["floor" "building"]}))
          (is (= (first (get-paths Floor Building classes))
                 {:path [ru.petrsu.nest.son.Floor ru.petrsu.nest.son.Building], 
                  :ppath ["building"]}))
          (is (= (first (get-paths Building Floor classes))
                 {:path [ru.petrsu.nest.son.Building ru.petrsu.nest.son.Floor], 
                  :ppath ["floors"]})))

(deftest t-get-short-name
         ^{:doc "Tests the get-short-name function."}
         (is (= "f" (get-short-name Floor)))
         (is (= "b" (get-short-name Building)))
         (is (= "ni" (get-short-name ru.petrsu.nest.son.NetworkInterface)))
         (is (= "ip4i" (get-short-name ru.petrsu.nest.son.IPv4Interface))))

(deftest t-children
         ^{:doc "Tests the children function."}
         (is (= (children yzi/classes) 
                {ru.petrsu.nest.son.LinkInterface
                  #{ru.petrsu.nest.son.EthernetInterface
                       ru.petrsu.nest.son.UnknownLinkInterface
                       ru.petrsu.nest.son.VLANInterface},
                  ru.petrsu.nest.son.NetworkInterface
                  #{ru.petrsu.nest.son.IPv4Interface
                       ru.petrsu.nest.son.UnknownNetworkInterface},
                  ru.petrsu.nest.son.Network
                  #{ru.petrsu.nest.son.IPNetwork ru.petrsu.nest.son.UnknownNetwork},
                  ru.petrsu.nest.son.NetworkElement
                  #{ru.petrsu.nest.son.IPNetwork ru.petrsu.nest.son.NetworkInterface
                       ru.petrsu.nest.son.IPv4Interface
                       ru.petrsu.nest.son.EthernetInterface
                       ru.petrsu.nest.son.UnknownNetworkInterface ru.petrsu.nest.son.Device
                       ru.petrsu.nest.son.UnknownLinkInterface
                       ru.petrsu.nest.son.VLANInterface ru.petrsu.nest.son.Network
                       ru.petrsu.nest.son.LinkInterface ru.petrsu.nest.son.UnknownNetwork},
                  ru.petrsu.nest.son.SpatialElement
                  #{ru.petrsu.nest.son.Building ru.petrsu.nest.son.Floor
                       ru.petrsu.nest.son.Room},
                  ru.petrsu.nest.son.OrganizationalElement
                  #{ru.petrsu.nest.son.SimpleOU ru.petrsu.nest.son.AbstractOU
                       ru.petrsu.nest.son.CompositeOU},
                  ru.petrsu.nest.son.SonElement
                  #{ru.petrsu.nest.son.SimpleOU ru.petrsu.nest.son.Building
                       ru.petrsu.nest.son.SON ru.petrsu.nest.son.NetworkElement
                       ru.petrsu.nest.son.IPNetwork ru.petrsu.nest.son.NetworkInterface
                       ru.petrsu.nest.son.Occupancy ru.petrsu.nest.son.Floor
                       ru.petrsu.nest.son.IPv4Interface ru.petrsu.nest.son.AbstractOU
                       ru.petrsu.nest.son.EthernetInterface
                       ru.petrsu.nest.son.UnknownNetworkInterface ru.petrsu.nest.son.Room
                       ru.petrsu.nest.son.CompositeOU ru.petrsu.nest.son.Device
                       ru.petrsu.nest.son.UnknownLinkInterface
                       ru.petrsu.nest.son.VLANInterface ru.petrsu.nest.son.Network
                       ru.petrsu.nest.son.OrganizationalElement
                       ru.petrsu.nest.son.LinkInterface ru.petrsu.nest.son.UnknownNetwork
                       ru.petrsu.nest.son.SpatialElement},
                  ru.petrsu.nest.son.AbstractOU
                  #{ru.petrsu.nest.son.SimpleOU ru.petrsu.nest.son.CompositeOU}}))

         (is (= (children yzi/jpa-classes) 
                {ru.petrsu.nest.son.jpa.Network #{ru.petrsu.nest.son.jpa.IPNetwork}
                  
                 ru.petrsu.nest.son.jpa.NetworkInterface #{ru.petrsu.nest.son.jpa.IPv4Interface}
                
                 ru.petrsu.nest.son.jpa.LinkInterface
                 #{ru.petrsu.nest.son.jpa.EthernetInterface
                   ru.petrsu.nest.son.jpa.VLANInterface},
                 
                 ru.petrsu.nest.son.jpa.AbstractSonModificationOccurence
                 #{ru.petrsu.nest.son.jpa.SonPropertyModificationOccurence
                   ru.petrsu.nest.son.jpa.SonReferenceModificationOccurence},
                 
                 ru.petrsu.nest.son.jpa.SonElement
                 #{ru.petrsu.nest.son.jpa.Room 
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
                   ru.petrsu.nest.son.jpa.SON},

                 ru.petrsu.nest.son.jpa.AbstractOU
                 #{ru.petrsu.nest.son.jpa.CompositeOU ru.petrsu.nest.son.jpa.SimpleOU}})))

