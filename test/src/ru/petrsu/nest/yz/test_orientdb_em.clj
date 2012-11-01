;;
;; Copyright 2012 Vyacheslav Dimitrov <vyacheslav.dimitrov@gmail.com>
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

(ns ru.petrsu.nest.yz.test-orientdb-em
  ^{:author "Vyacheslav Dimitrov"
    :doc "Tests oriend db element manager."}
  (:use clojure.test 
        ru.petrsu.nest.yz.orientdb-em.core)
  (:require
   (ru.petrsu.nest.yz [init :as i]) (ru.petrsu.nest.yz.queries [uni-bd :as ubd]))
  (:import (com.orientechnologies.orient.object.db OObjectDatabaseTx)
           (university.model Student Course Faculty)))


(def o-db (doto (OObjectDatabaseTx. "memory:uni") (.create)))
        
(def o-em
  "OrientDB ElementManager"
  (let [_ (.registerEntityClass (.getEntityManager o-db) Student)
        _ (.registerEntityClass (.getEntityManager o-db) Course)
        _ (.registerEntityClass (.getEntityManager o-db) Faculty)
        _ (.save o-db ubd/alg)]
    (-createOrientElementManager o-db)))

