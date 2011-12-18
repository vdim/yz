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

(ns ru.petrsu.nest.yz.queries.exact-select
  ^{:author "Vyacheslav Dimitrov"
    :doc "Tests queries with exact modificator (^)."}
  (:use clojure.test)
  (:require [ru.petrsu.nest.yz.queries.core :as tc]
            [ru.petrsu.nest.util.utils :as f])
  (:import (ru.petrsu.nest.son SON, Device, UnknownNetwork IPNetwork, 
                               EthernetInterface, UnknownLinkInterface, VLANInterface,
                               IPv4Interface, UnknownNetworkInterface)))


;; Define model

(def net1 (doto (IPNetwork.) 
           (.setAddress (f/ip2b "192.168.112.32")) 
           (.setMask (f/ip2b "255.255.255.224"))))
(def net2 (doto (IPNetwork.) 
           (.setAddress (f/ip2b "172.20.255.108")) 
           (.setMask (f/ip2b "255.255.255.252"))))
(def net3 (doto (UnknownNetwork.)))

(def rd_ei1_ni1 (doto (IPv4Interface.) 
                  (.setInetAddress (f/ip2b "192.168.112.50")) 
                  (.setNetwork net1)))
(def rd_ei1_ni2 (doto (IPv4Interface.) 
                  (.setInetAddress (f/ip2b "172.20.255.109"))
                  (.setNetwork net2)))
(def d1_ei1_ni1 (doto (IPv4Interface.) 
                  (.setInetAddress (f/ip2b "192.168.112.51"))
                  (.setNetwork net1)))
(def d1_uli_uni (doto (UnknownNetworkInterface.)
                  (.setNetwork net3)))

(def rd_ei1 (doto (EthernetInterface.) 
              (.setMACAddress (f/mac2b "00:15:f9:05:24:c5"))
              (.addNetworkInterface rd_ei1_ni1)))
(def rd_ei2 (doto (EthernetInterface.) 
              (.setMACAddress (f/mac2b "00:15:63:a0:ae:0e"))
              (.addNetworkInterface rd_ei1_ni2)))
(def d1_ei1 (doto (EthernetInterface.) 
              (.setMACAddress (f/mac2b "00:15:63:a0:ae:1e"))
              (.addNetworkInterface d1_ei1_ni1)))
(def d1_uli (doto (UnknownLinkInterface.)
              (.addNetworkInterface d1_uli_uni)))
(def rd_ei3 (doto (EthernetInterface.) (.setMACAddress (f/mac2b "00:15:f9:05:24:c4"))))
(def rd_ei4 (doto (EthernetInterface.) (.setMACAddress (f/mac2b "00:15:63:a0:ae:0f"))))

(def d1 (doto (Device.)
          (.addLinkInterface d1_ei1)
          (.addLinkInterface (VLANInterface.))
          (.addLinkInterface d1_uli)))

(def rootDevice (doto (Device.) 
                  (.addLinkInterface rd_ei1)
                  (.addLinkInterface rd_ei2)
                  (.addLinkInterface rd_ei3)
                  (.addLinkInterface rd_ei4) 
                  (.addLinkInterface (VLANInterface.))
                  (.addLinkInterface (UnknownLinkInterface.))))


(def son (doto (SON.)
           (.setRootDevice rootDevice)))

;; Define entity manager.

(use-fixtures :once (tc/setup-son son))


;; Define tests
(deftest exact-test
         (let [cr #(= (count (tc/rows-query %1)) %2)]
           (is (cr "vlani" 2))))
