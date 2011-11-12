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

(ns ru.petrsu.nest.yz.queries.pp-from-mom
  ^{:author "Vyacheslav Dimitrov"
    :doc "Tests queries which imply processing properties from MOM."}
  (:use clojure.test)
  (:require [ru.petrsu.nest.yz.queries.core :as tc]
            [ru.petrsu.nest.yz.functions :as f]
            [ru.petrsu.nest.yz.queries.bd :as bd])
  (:import (ru.petrsu.nest.son SON Building Room Floor NetworkInterface IPv4Interface)))


;; Define model

(def son (doto (SON.)
           (.setRootDevice bd/rootDevice)
           (.setRootOU bd/rootCompositeOU)))

;; Define entity manager.

(use-fixtures :once (tc/setup-son son))

;; Define helper functions.

(defn- ip=
  "Returns true if an inetAddress of the ip4i equals
  specified IP."
  [ip4i, ip]
  (= (f/ip (.getInetAddress ip4i)) ip))

(defn- check-ip
  "Takes rows of a query and returns true, if each
  element of specified column has IP address with
  specified value."
  [rows, n, ip]
  (every? true? (map #(if (>= n (count %)) true (ip= (nth % n) ip)) rows)))


;; Define tests

(deftest select-ei-mac
         (let [rows (tc/rows-query "ei#(MACAddress=\"00:15:63:a0:ae:0e\")")]
           (is (= (count rows) 1))
           (is (= (f/mac (.getMACAddress ((nth rows 0) 0))) "00:15:63:a0:ae:0e")))
         (let [rows (tc/rows-query "ei#(MACAddress=\"00:15:63:a0:ae:1e\")")]
           (is (= (count rows) 1))
           (is (= (f/mac (.getMACAddress ((nth rows 0) 0))) "00:15:63:a0:ae:1e")))
         (let [rows (tc/rows-query "ei#(MACAddress=\"00:15:63:a0:ae:2e\")")]
           (is (= (count rows) 0))))

(deftest select-ei-mac-with-f
         (let [rows (tc/rows-query "ei#(MACAddress=@(mac2b \"00:15:63:a0:ae:0e\"))")]
           (is (= (count rows) 1))
           (is (= (f/mac (.getMACAddress ((nth rows 0) 0))) "00:15:63:a0:ae:0e")))
         (let [rows (tc/rows-query "ei#(MACAddress=@(mac2b \"00:15:63:a0:ae:1e\"))")]
           (is (= (count rows) 1))
           (is (= (f/mac (.getMACAddress ((nth rows 0) 0))) "00:15:63:a0:ae:1e")))
         (let [rows (tc/rows-query "ei#(MACAddress=@(mac2b \"00:15:63:a0:ae:2e\"))")]
           (is (= (count rows) 0))))


(deftest select-ip4i-ip
         (let [rows (tc/rows-query "ip4i#(inetAddress=\"192.168.112.50\")")]
           (is (= (count rows) 1))
           (is (check-ip rows, 0 "192.168.112.50")))
         (let [rows (tc/rows-query "ip4i#(inetAddress=\"192.168.112.51\")")]
           (is (= (count rows) 1))
           (is (check-ip rows, 0 "192.168.112.51")))
         (let [rows (tc/rows-query "ip4i#(inetAddress=\"192.168.112.33\")")]
           (is (= (count rows) 0))))


(deftest select-ip4i-ip-with-f
         (let [rows (tc/rows-query "ip4i#(inetAddress= @(ip2b \"192.168.112.50\"))")]
           (is (= (count rows) 1))
           (is (check-ip rows, 0 "192.168.112.50")))
         (let [rows (tc/rows-query "ip4i#(inetAddress= @(ip2b \"192.168.112.51\"))")]
           (is (= (count rows) 1))
           (is (check-ip rows, 0 "192.168.112.51")))
         (let [rows (tc/rows-query "ip4i#(inetAddress= @(ip2b \"192.168.112.33\"))")]
           (is (= (count rows) 0))))


(deftest select-ipn-address
         (let [rows (tc/rows-query "ipn#(address=\"192.168.112.32\")")]
           (is (= (count rows) 1))
           (is (= (f/ip (.getAddress ((nth rows 0) 0))) "192.168.112.32")))
         (let [rows (tc/rows-query "ipn#(address=\"192.168.112.33\")")]
           (is (= (count rows) 0))))


(deftest select-ipn-address-f
         (let [rows (tc/rows-query "ipn#(address= @(ip2b \"192.168.112.32\"))")]
           (is (= (count rows) 1))
           (is (= (f/ip (.getAddress ((nth rows 0) 0))) "192.168.112.32")))
         (let [rows (tc/rows-query "ipn#(address= @(ip2b \"192.168.112.33\"))")]
           (is (= (count rows) 0))))


(deftest select-device-ip4i-ip
         (let [rows (tc/rows-query "device (ip4i#(inetAddress=\"192.168.112.50\"))")]
           (is (check-ip rows, 1 "192.168.112.50")))
         (let [rows (tc/rows-query "device (ip4i#(inetAddress=\"192.168.112.51\"))")]
           (is (check-ip rows, 1 "192.168.112.51")))
         (let [rows (tc/rows-query "device (ip4i#(inetAddress=\"192.168.112.33\"))")]
           (is (every? true? (map  #(= (count %) 1) rows)))))

(deftest complex-predicate
         (let [rows (tc/rows-query "ip4i#(inetAddress=(\"192.168.112.50\" || \"192.168.112.51\"))")]
           (is (= (count rows) 2))
           (is (and 
                 (or (ip= (nth (nth rows 0) 0) "192.168.112.50")
                     (ip= (nth (nth rows 0) 0) "192.168.112.51"))
                 (or (ip= (nth (nth rows 1) 0) "192.168.112.50") 
                     (ip= (nth (nth rows 1) 0) "192.168.112.51")))))
         (let [rows (tc/rows-query "ip4i#(inetAddress=(\"192.168.112.50\" || \"192.168.112.52\"))")]
           (is (= (count rows) 1))
           (is (ip= (nth (nth rows 0) 0) "192.168.112.50")))
         (let [rows (tc/rows-query "ip4i#(inetAddress=(\"192.168.112.50\" && \"192.168.112.52\"))")]
           (is (= (count rows) 0)))
         (let [rows (tc/rows-query "ip4i#(inetAddress=(\"192.168.112.50\" || \"192.168.112.51\" || \"192.168.112.52\"))")]
           (is (= (count rows) 2))
           (is (and 
                 (or (ip= (nth (nth rows 0) 0) "192.168.112.50")
                     (ip= (nth (nth rows 0) 0) "192.168.112.51"))
                 (or (ip= (nth (nth rows 1) 0) "192.168.112.50") 
                     (ip= (nth (nth rows 1) 0) "192.168.112.51"))))))

(deftest select-inheritance-preds
         ^{:doc "Tests query like this device#(ei.MACAddress=...). 
                (Problem with inheritance into predicates.)"}
         (let [rows (tc/rows-query "device#(ei.MACAddress=\"00:15:63:a0:ae:0e\")")]
           (is (= (count rows) 1)))
         (let [rows (tc/rows-query "device#(ei.MACAddress=\"00:15:63:a0:ae:2e\")")]
           (is (= (count rows) 0)))
         (let [f #(let [rows (tc/rows-query %)]
                    (and (= (count rows) 1) (= (nth (nth rows 0) 0) bd/b2)))]
           (is (f "building#(ei.MACAddress=\"00:15:63:a0:ae:0e\")"))
           (is (f "building#(device.ei.MACAddress=\"00:15:63:a0:ae:0e\")"))
           (is (f "building#(occupancy.ei.MACAddress=\"00:15:63:a0:ae:0e\")"))
           (is (f "building#(room.device.ei.MACAddress=\"00:15:63:a0:ae:0e\")"))
           (is (f "building#(floor.device.ei.MACAddress=\"00:15:63:a0:ae:0e\")"))
           (is (f "building#(room.floor.ei.MACAddress=\"00:15:63:a0:ae:0e\")"))
           (is (f "building#(floor.room.device.ei.MACAddress=\"00:15:63:a0:ae:0e\")"))
           (is (f "building#(floor.room.ei.MACAddress=\"00:15:63:a0:ae:0e\")"))

           (is (f "building#(ip4i.inetAddress=\"192.168.112.50\")"))
           (is (f "building#(room.ip4i.inetAddress=\"192.168.112.50\")"))
           (is (f "building#(floor.room.ip4i.inetAddress=\"192.168.112.50\")"))
           (is (f "building#(room.floor.ip4i.inetAddress=\"192.168.112.50\")"))
           (is (f "building#(device.ip4i.inetAddress=\"192.168.112.50\")"))
           (is (f "building#(floor.device.ip4i.inetAddress=\"192.168.112.50\")"))
           (is (f "building#(floor.room.device.ip4i.inetAddress=\"192.168.112.50\")"))
           (is (f "building#(floor.building.room.ip4i.inetAddress=\"192.168.112.50\")"))
          
           (is (f "building#(ip4i.inetAddress=(\"192.168.112.50\" || \"192.168.112.55\"))"))
           (is (f "building#(room.ip4i.inetAddress=(\"192.168.112.50\" || \"192.168.112.55\"))"))
           (is (f "building#(floor.room.ip4i.inetAddress=(\"192.168.112.50\" || \"192.168.112.55\"))"))
           (is (f "building#(room.floor.ip4i.inetAddress=(\"192.168.112.50\" || \"192.168.112.55\"))"))
           (is (f "building#(device.ip4i.inetAddress=(\"192.168.112.50\" || \"192.168.112.55\"))"))
           (is (f "building#(floor.device.ip4i.inetAddress=(\"192.168.112.50\" || \"192.168.112.55\"))"))
           (is (f "building#(floor.room.device.ip4i.inetAddress=(\"192.168.112.50\" || \"192.168.112.55\"))"))
           (is (f "building#(floor.building.room.ip4i.inetAddress=(\"192.168.112.50\" || \"192.168.112.55\"))"))

           (is (f "building#(ip4i.inetAddress=(\"192.168.112.50\" || \"192.168.112.55\" || \"192.168.112.56\"))"))
           (is (f "building#(room.ip4i.inetAddress=(\"192.168.112.50\" || \"192.168.112.55\" || \"192.168.112.56\"))"))
           (is (f "building#(floor.room.ip4i.inetAddress=(\"192.168.112.50\" || \"192.168.112.55\" || \"192.168.112.56\"))"))
           (is (f "building#(room.floor.ip4i.inetAddress=(\"192.168.112.50\" || \"192.168.112.55\" || \"192.168.112.56\"))"))
           (is (f "building#(device.ip4i.inetAddress=(\"192.168.112.50\" || \"192.168.112.55\" || \"192.168.112.56\"))"))
           (is (f "building#(floor.device.ip4i.inetAddress=(\"192.168.112.50\" || \"192.168.112.55\" || \"192.168.112.56\"))"))
           (is (f "building#(floor.room.device.ip4i.inetAddress=(\"192.168.112.50\" || \"192.168.112.55\" || \"192.168.112.56\"))"))
           (is (f "building#(floor.building.room.ip4i.inetAddress=(\"192.168.112.50\" || \"192.168.112.55\" || \"192.168.112.56\"))"))))

