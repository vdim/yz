
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
  (:require [ru.petrsu.nest.yz.queries.core :as tc])
  (:import (ru.petrsu.nest.son SON Building Room Floor)))

;; Define model

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

(def son (doto (SON.)
           (.addBuilding b1) 
           (.addBuilding b2)))

;; Memory Element Manager.
(def mem (tc/create-emm son))
