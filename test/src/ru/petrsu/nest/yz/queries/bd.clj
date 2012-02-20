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

(ns ru.petrsu.nest.yz.queries.bd
  ^{:author "Vyacheslav Dimitrov"
    :doc "Pretty BD with Nest model for testing."}
  (:require [ru.petrsu.nest.yz.queries.core :as tc]
            [ru.petrsu.nest.util.utils :as f])
  (:import (ru.petrsu.nest.son SON Building Room Floor
                               Device, IPNetwork, EthernetInterface, 
                               IPv4Interface, UnknownLinkInterface,
                               CompositeOU, SimpleOU, Occupancy)))

;; Define model

;Spatial Structure

(def r101_f1_b1 (doto (Room.) (.setNumber "101")))
(def r102_f1_b1 (doto (Room.) (.setNumber "102")))

(def f1_b1 (doto (Floor.) 
             (.setNumber (Integer. 1))
             (.addRoom r101_f1_b1) 
             (.addRoom r102_f1_b1)))

(def f2_b1 (doto (Floor.) 
             (.setNumber (Integer. 2))
             (.addRoom (doto (Room.) (.setNumber "201"))) 
             (.addRoom (doto (Room.) (.setNumber "202")))))

(def f3_b1 (doto (Floor.) 
             (.setNumber (Integer. 3))))


(def r1001_f1_b2 (doto (Room.) (.setNumber "1001")))
(def f1_b2 (doto (Floor.) 
             (.setNumber (Integer. 1))
             (.addRoom r1001_f1_b2) 
             (.addRoom (doto (Room.) (.setNumber "1002")))))

(def b1 (doto (Building.) (.setName "MB") (.addFloor f1_b1) (.addFloor f2_b1) (.addFloor f3_b1)))
(def b2 (doto (Building.) (.setName "TK") (.addFloor f1_b2)))


;Network Structure

(def net1 (doto (IPNetwork.) 
           (.setAddress (f/ip2b "192.168.112.32")) 
           (.setMask (f/ip2b "255.255.255.224"))))
(def net2 (doto (IPNetwork.) 
           (.setAddress (f/ip2b "172.20.255.108")) 
           (.setMask (f/ip2b "255.255.255.252"))))

(def rd_ei1_ni1 (doto (IPv4Interface.) 
                  (.setInetAddress (f/ip2b "192.168.112.50")) 
                  (.setNetwork net1)))
(def rd_ei1_ni2 (doto (IPv4Interface.) 
                  (.setInetAddress (f/ip2b "172.20.255.109"))
                  (.setNetwork net2)))
(def d1_ei1_ni1 (doto (IPv4Interface.) 
                  (.setInetAddress (f/ip2b "192.168.112.51"))
                  (.setNetwork net1)))

(def rd_ei1 (doto (EthernetInterface.) 
              (.setMACAddress (f/mac2b "00:15:f9:05:24:c5"))
              (.addNetworkInterface rd_ei1_ni1)))
(def rd_ei2 (doto (EthernetInterface.) 
              (.setMACAddress (f/mac2b "00:15:63:a0:ae:0e"))
              (.addNetworkInterface rd_ei1_ni2)))
(def d1_ei1 (doto (EthernetInterface.) 
              (.setMACAddress (f/mac2b "00:15:63:a0:ae:1e"))
              (.addNetworkInterface d1_ei1_ni1)))
(def rd_ei3 (doto (EthernetInterface.) (.setMACAddress (f/mac2b "00:15:f9:05:24:c4"))))
(def rd_ei4 (doto (EthernetInterface.) (.setMACAddress (f/mac2b "00:15:63:a0:ae:0f"))))

(def d1 (doto (Device.)
          (.addLinkInterface d1_ei1)))
(def rootDevice (doto (Device.) 
                  (.addLinkInterface rd_ei1)
                  (.addLinkInterface rd_ei2)
                  (.addLinkInterface rd_ei3)
                  (.addLinkInterface rd_ei4)
                  (.addLinkInterface (UnknownLinkInterface.))))

;Organisation Structure

(def sou1_d1 (doto (SimpleOU.) (.setName "S1_D1")))
(def sou2_d1 (doto (SimpleOU.) (.setName "S2_D1")))
(def cou_d1 (doto (CompositeOU.) 
              (.setName "Departure1")
              (.addOU sou1_d1)
              (.addOU sou2_d1)))

(def sou1_d2 (doto (SimpleOU.) (.setName "S1_D2")))
(def sou2_d2 (doto (SimpleOU.) (.setName "S2_D2")))
(def cou_d2 (doto (CompositeOU.) 
              (.setName "Departure2")
              (.addOU sou1_d2)
              (.addOU sou2_d2)))


(def rootCompositeOU (doto (CompositeOU.)
                       (.setName "Test Enterprise")
                       (.addOU cou_d1)
                       (.addOU cou_d2)))

; Occupancies for linking all structure.

(def o1 (doto (Occupancy.)
         (.addDevice d1)
         (.setOU sou1_d1)
         (.setRoom r102_f1_b1)))

(def o2 (doto (Occupancy.)
         (.addDevice rootDevice)
         (.setOU sou2_d2)
         (.setRoom r1001_f1_b2)))


; SON.

(def son (doto (SON.)
           (.addBuilding b1) 
           (.addBuilding b2)
           (.setRootDevice rootDevice)
           (.setRootOU rootCompositeOU)))

;; Memory Element Manager.
(def mem (tc/create-emm son))
