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
  (:use ru.petrsu.nest.yz.core)
  (:use ru.petrsu.nest.yz.functions)
  (:require [ru.petrsu.nest.yz.queries.core :as tc])
  (:import (ru.petrsu.nest.son SON Building Room Floor
                               Device, IPNetwork, EthernetInterface, IPv4Interface)))

;; Define model

;Spatial Structure
(def f1_b1 (doto (Floor.) 
             (.setNumber (Integer. 1))
             (.addRoom (doto (Room.) (.setNumber "101"))) 
             (.addRoom (doto (Room.) (.setNumber "102")))))

(def f2_b1 (doto (Floor.) 
             (.setNumber (Integer. 2))
             (.addRoom (doto (Room.) (.setNumber "201"))) 
             (.addRoom (doto (Room.) (.setNumber "202")))))

(def f1_b2 (doto (Floor.) 
             (.setNumber (Integer. 1))
             (.addRoom (doto (Room.) (.setNumber "1001"))) 
             (.addRoom (doto (Room.) (.setNumber "1002")))))

(def b1 (doto (Building.) (.setName "MB") (.addFloor f1_b1) (.addFloor f2_b1)))
(def b2 (doto (Building.) (.setName "TK") (.addFloor f1_b2)))


;Network Structure

(def net1 (doto (IPNetwork.) 
           (.setAddress (ip2b "192.168.112.32")) 
           (.setMask (ip2b "255.255.255.224"))))
(def net2 (doto (IPNetwork.) 
           (.setAddress (ip2b "172.20.255.108")) 
           (.setMask (ip2b "255.255.255.252"))))

(def rd_ei1_ni1 (doto (IPv4Interface.) 
                  (.setInetAddress (ip2b "192.168.112.50")) 
                  (.setNetwork net1)))
(def rd_ei1_ni2 (doto (IPv4Interface.) 
                  (.setInetAddress (ip2b "172.20.255.109"))
                  (.setNetwork net2)))
(def d1_ei1_ni1 (doto (IPv4Interface.) 
                  (.setInetAddress (ip2b "192.168.112.51"))
                  (.setNetwork net1)))

(def rd_ei1 (doto (EthernetInterface.) 
              (.setMACAddress (mac2b "0015f90524c5")) 
              (.addNetworkInterface rd_ei1_ni1)))
(def rd_ei2 (doto (EthernetInterface.) 
              (.setMACAddress (mac2b "001563a0ae0e"))
              (.addNetworkInterface rd_ei1_ni2)))
(def d1_ei1 (doto (EthernetInterface.) 
              (.setMACAddress (mac2b "001563a0ae1e"))
              (.addNetworkInterface d1_ei1_ni1)))
(def rd_ei3 (doto (EthernetInterface.) (.setMACAddress (mac2b "0015f90524c4"))))
(def rd_ei4 (doto (EthernetInterface.) (.setMACAddress (mac2b "001563a0ae0f"))))

(def d1 (doto (Device.)
          (.addLinkInterface d1_ei1)))
(def rootDevice (doto (Device.) 
                  (.addLinkInterface rd_ei1)
                  (.addLinkInterface rd_ei2)
                  (.addLinkInterface rd_ei3)
                  (.addLinkInterface rd_ei4)))

  
(def son (doto (SON.)
           (.addBuilding b1) 
           (.addBuilding b2)
           (.setRootDevice rootDevice)))

;; Memory Element Manager.
(def mem (tc/create-emm son))
