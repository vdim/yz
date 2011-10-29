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

(ns ru.petrsu.nest.yz.test-core
  ^{:author "Vyacheslav Dimitrov"
    :doc "Tests for core functions."}
  (:require [ru.petrsu.nest.yz.hb-utils :as hb]
            [ru.petrsu.nest.yz.core :as c]
            [ru.petrsu.nest.yz.parsing :as p])
  (:use clojure.test))


(def mom 
  ^{:doc "Defines the map of the object model (used Nest's model)"}
  (hb/mom-from-file "nest.mom"))

(comment
(deftest t-get-columns
         ^{:doc "Tests 'get-columns' function."}
         (is (= (c/get-columns (p/parse "building", mom)) ["Building"]))
         (is (= (c/get-columns (p/parse "building (room)", mom)) ["Building" "Room"]))
         (is (= (c/get-columns (p/parse "building (room (floor))", mom)) ["Building" "Room" "Floor"]))
         (is (= (c/get-columns (p/parse "building (room (floor (device)))", mom)) ["Building" "Room" "Floor" "Device"]))
         (is (= (c/get-columns (p/parse "building (room, floor)", mom)) ["Building" "Room, Floor"]))
         (is (= (c/get-columns (p/parse "building[name]", mom)) ["name"]))
         (is (= (c/get-columns (p/parse "building[& name]", mom)) ["Building" "name"]))
         (is (= (c/get-columns (p/parse "building[name floors address]", mom)) ["name" "floors" "address"]))
         (is (= (c/get-columns (p/parse "building[name & floors address]", mom)) ["name" "Building" "floors" "address"]))
         (is (= (c/get-columns (p/parse "building[name floors & address]", mom)) ["name" "floors" "Building" "address"]))
         (is (= (c/get-columns (p/parse "building[name floors address &]", mom)) ["name" "floors" "address" "Building"]))
         (is (= (c/get-columns (p/parse "building[name]", mom)) ["name"]))
         (is (= (c/get-columns (p/parse "building[name]", mom)) ["name"]))
         (is (= (c/get-columns (p/parse "building[name]", mom)) ["name"]))
         (is (= (c/get-columns (p/parse "building[name]", mom)) ["name"]))
         (is (= (c/get-columns (p/parse "building[name]", mom)) ["name"]))
         (is (= (c/get-columns (p/parse "building[name]", mom)) ["name"]))
         (is (= (c/get-columns (p/parse "building[name]", mom)) ["name"]))
         (is (= (c/get-columns (p/parse "building[name]", mom)) ["name"]))
         (is (= (c/get-columns (p/parse "building[name]", mom)) ["name"])))
)
