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

(ns ru.petrsu.nest.yz.functions
  ^{:author "Vyacheslav Dimitrov"
    :doc "Set of functions which is build-in YZ."}
  (:import (java.net InetAddress)
           (ru.petrsu.nest.yz YZUtils)))


(defn maxl
  "Takes a list with tuples which have one element and its
  value is numeric ant returns max element from this list."
  [tuples]
  (reduce max (flatten tuples)))


(defn minl
  "Takes a list with tuples which have one element and its
  value is numeric and returns min element from this list."
  [tuples]
  (reduce min (flatten tuples)))


(defn ip2b 
  "Transforms a string representation of 
  the IP Address to an array of bytes."
  [^String ip]
  (if (nil? ip)
    nil
    (.getAddress (InetAddress/getByName ip))))


(defn ^String ip2name
  "Transforms a byte representation of 
  the IP address to a host name."
  [ip]
  (if (nil? ip)
    nil
    (.getHostName (InetAddress/getByAddress ip))))


(defn ^String ip
  "Transforms a byte representation of 
  the IP address to a string representation."
  [ip]
  (if (nil? ip)
    nil
    (.getHostAddress (InetAddress/getByAddress ip))))


(defn ^String mac
  "Transforms a byte representation of 
  the MAC address to a string representation."
  [mac]
  (if (nil? mac)
    nil
    (reduce #(str %1 ":" %2) 
            (map #(String/format 
                    "%02x" 
                    (into-array [(bit-and 0xFF %)])) mac))))


(defn mac2b
  "Transforms a string representation of the
  MAC address to a byte representation.
  Example: (mac2b \"00:01:02:AA:0F:12\")."
  [^String mac]
  (YZUtils/getMACfromString mac))
