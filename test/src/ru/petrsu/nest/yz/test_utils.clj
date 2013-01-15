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

(ns ru.petrsu.nest.yz.test-utils
  ^{:author "Vyacheslav Dimitrov"
    :doc "Tests utils functions."}
  (:use ru.petrsu.nest.yz.mom-utils clojure.test ru.petrsu.nest.yz.utils)
  (:require [ru.petrsu.nest.yz.init :as yzi] 
            [ru.petrsu.nest.yz.queries.bd :as bd] 
            [ru.petrsu.nest.yz.core :as yz] [ru.petrsu.nest.yz.queries.core :as tc])
  (:import (ru.petrsu.nest.son SonElement SON, SpatialElement, Building, Room, Floor,
                               Device, Network, IPNetwork, UnknownNetwork,
                               NetworkElement, NetworkInterface, IPv4Interface, UnknownNetworkInterface,
                               EthernetInterface, LinkInterface, VLANInterface, UnknownLinkInterface,
                               OrganizationalElement, AbstractOU, CompositeOU, SimpleOU, Occupancy)))

(def classes #{Floor, Room, Building})

(deftest t-get-ps
         ^{:doc "Tests the get-ps function."}
          (is (= (first (get-ps Room Building classes nil))
                 {:path [Room Floor Building], 
                  :ppath ["floor" "building"]}))
          (is (= (first (get-ps Floor Building classes nil))
                 {:path [Floor Building], 
                  :ppath ["building"]}))
          (is (= (first (get-ps Building Floor classes nil))
                 {:path [Building Floor], 
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


(deftest t-intersection
         ^{:doc "Tests the intersection function."}
         (is (= [] (intersection [1 []] [2 []])))
         
         (is (= [1 []] (intersection [1 []] [1 []])))
         (is (= [1 []] (intersection [1 []] [1 [] 3 []])))
         (is (= [1 []] (intersection [1 []] [3 [] 1 []])))
         (is (= [1 []] (intersection [2 [] 1 []] [1 []])))
         (is (= [1 []] (intersection [1 [] 2 []] [1 []])))
         (is (= [1 []] (intersection [2 [] 1 []] [1 [] 3 []])))
         (is (= [1 []] (intersection [1 [] 2 []] [1 [] 3 []])))

         (is (= [1 [] 2 []] (intersection [1 [] 2 []] [1 [] 2 []])))
         (is (= [1 [] 2 []] (intersection [3 [] 1 [] 2 []] [1 [] 2 []])))
         (is (= [1 [] 2 []] (intersection [1 [] 3 [] 2 []] [1 [] 2 []])))
         (is (= [1 [] 2 []] (intersection [1 [] 2 [] 3 []] [1 [] 2 []])))
         (is (= [1 [] 2 []] (intersection [1 [] 2 []] [3 [] 1 [] 2 []])))
         (is (= [1 [] 2 []] (intersection [1 [] 2 []] [1 [] 3 [] 2 []])))
         (is (= [1 [] 2 []] (intersection [1 [] 2 []] [1 [] 2 [] 3 []])))
         
         (is (= [] (intersection [1 []] [1 [1 []]])))
         (is (= [] (intersection [1 []] [1 [2 []]])))
         (is (= [] (intersection [1 [1 []]] [1 [2 []]])))
         (is (= [1 [2 []]] (intersection [1 [2 []]] [1 [2 []]])))
         (is (= [2 [] 1 [2 []]] (intersection [2 [] 1 [2 []]] [1 [2 []] 2 []])))
         (is (= [2 [] 1 [2 []]] (intersection [3 [] 2 [] 1 [2 []]] [1 [2 []] 2 []])))
         (is (= [2 [] 1 [2 []]] (intersection [2 [] 1 [2 []]] [3 [] 1 [2 []] 2 []])))
         (is (= [2 [] 1 [2 []]] (intersection [3 [1 []] 2 [] 1 [2 []]] [3 [] 1 [2 []] 2 []])))
         (is (= [2 [] 1 [2 []]] (intersection [3 [1 []] 2 [] 1 [2 []]] [3 [2 []] 1 [2 []] 2 []])))
         (is (= [2 [] 1 [2 []]] (intersection [3 [1 [] 2 []] 2 [] 1 [2 []]] [3 [2 []] 1 [2 []] 2 []])))
         (is (= [2 [] 1 [2 []]] (intersection [3 [1 [] 2 []] 2 [] 1 [2 []]] [1 [2 []] 3 [2 []] 2 []])))


         (is (= [2 [1 [4 [] 5 [6 [] 7 [8 []]]]]] (intersection [2 [1 [4 [] 5 [6 [] 7 [8 []]]]]] 
                                                               [2 [1 [4 [] 5 [6 [] 7 [8 []]]]]])))
         (is (= [2 [1 [4 [] 5 [6 [] 7 [8 []]]]]] (intersection [1 [] 2 [1 [4 [] 5 [6 [] 7 [8 []]]]]] 
                                                               [2 [1 [4 [] 5 [6 [] 7 [8 []]]]]])))
         (is (= [1 [] 2 [1 [4 [] 5 [6 [] 7 [8 []]]]]] (intersection [1 [] 2 [1 [4 [] 5 [6 [] 7 [8 []]]]]] 
                                                                    [2 [1 [4 [] 5 [6 [] 7 [8 []]]]] 1 []])))
         (is (= [] (intersection [1 [] 2 [1 [4 [] 5 [6 [] 7 [8 []]]]]] 
                                 [2 [1 [4 [] 5 [6 [] 7 [9 []]]]]])))
         (is (= [1 []] (intersection [1 [] 2 [1 [4 [] 5 [6 [] 7 [8 []]]]]] 
                                     [2 [1 [4 [] 5 [6 [] 7 [9 []]]]] 1 []]))))


(deftest t-gen-mom
         ^{:doc "Tests generating of mom."}
         (let [t-mom (gen-mom yzi/classes)
               f #(= (get-in t-mom [%1 %2]) %3)]
           (is (f Room Floor [["floor"]]))
           (is (f Floor Room [["rooms"]]))
           (is (f Floor Floor [["building" "floors"]]))
           (is (f Room Room [["floor" "rooms"]]))
           (is (f SimpleOU SimpleOU [["parent" "OUs"]]))
           (is (f CompositeOU SimpleOU [["OUs"]]))
           (is (f CompositeOU Room [["OUs" "occupancies" "room"]]))
           (is (f Building Building nil))
           (is (f Building SON nil))
           (is (f SON Room [["buildings" "floors" "rooms"] ["rootDevice" "occupancy" "room"]]))
           (is (every? true? (map #(f SonElement % nil) yzi/classes)))))


(deftest t-gen-mom-without-paths
         ^{:doc "Check elements without paths."}
         (let [t-mom (gen-mom yzi/classes)
               cl-cl (for [cl1 yzi/classes cl2 yzi/classes] [cl1 cl2])
               r (filter (fn [[cl1 cl2]] (let [q (str (.getSimpleName cl1) " (" (.getSimpleName cl2) ")")
                                               thr (:thrwable (yz/pquery q t-mom bd/mem))]
                                           (instance? ru.petrsu.nest.yz.NotFoundPathException thr)))
                         cl-cl)]
           (is (tc/eq-colls r 
                            [[SonElement SON] 
                             [SON SON]
                             [Occupancy SON]
                             [SpatialElement SON]
                             [Building SON]
                             [Room SON]
                             [Floor SON]
                             [NetworkElement SON]
                             [Device SON]
                             [LinkInterface SON]
                             [UnknownLinkInterface SON]
                             [EthernetInterface SON]
                             [VLANInterface SON]
                             [NetworkInterface SON]
                             [UnknownNetworkInterface SON]
                             [IPv4Interface SON]
                             [Network SON]
                             [UnknownNetwork SON]
                             [IPNetwork SON]
                             [OrganizationalElement SON]
                             [CompositeOU SON]
                             [SimpleOU SON]
                             [AbstractOU SON]
                             [Building Building]
                             [IPNetwork IPNetwork]
                             [UnknownNetwork UnknownNetwork]]))))
