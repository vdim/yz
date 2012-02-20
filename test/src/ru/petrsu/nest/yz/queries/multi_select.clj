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

(ns ru.petrsu.nest.yz.queries.multi-select
  ^{:author "Vyacheslav Dimitrov"
    :doc "Processes queries within multi selects like this:
         building, room"}
  (:use ru.petrsu.nest.yz.core 
        clojure.test)
  (:require [ru.petrsu.nest.yz.queries.core :as tc])
  (:import (ru.petrsu.nest.son SON Building Room Floor)))


;; Define model

(def son (doto (SON.)
           (.addBuilding (doto (Building.) (.addFloor (Floor.)) (.addFloor (Floor.)))) 
           (.addBuilding (doto (Building.) (.addFloor (Floor.))))))


;; Define entity manager.

(use-fixtures :once (tc/setup-son son))


;; Define tests

(deftest select-floors-and-buildings
         ^{:doc "Selects all Floor and Building objects."}
         (is (tc/check-query "floor, building" 
                             [[Floor [], Floor [], Floor[]], [Building [], Building []]])))

(deftest select-buildings-and-floors
         ^{:doc "Selects all Building and Floor objects."}
         (is (tc/check-query "building, floor"
                             [[Building [], Building []], [Floor [], Floor [], Floor []]])))

(deftest select-buildings-and-rooms
         ^{:doc "Selects all Building and Room objects."}
         (is (tc/check-query "building, room"
                             [[Building [], Building []], []])))

(deftest select-floors-and-rooms
         ^{:doc "Selects all Floor and Room objects."}
         (is (tc/check-query "floor, room" 
                             [[Floor [], Floor [], Floor[]], []])))

(deftest select-b-f-r
         ^{:doc "Selects all Building, Floor and Room objects."}
         (is (tc/qstruct? "building, floor, room" 
                          [[Building []], [Floor []], [nil nil]])))

(deftest select-b-r
         ^{:doc "Selects all Building and Floor objects."}
         (is (tc/qstruct? "building, floor"
                          [[Building []], [Floor []]])))


