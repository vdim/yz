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

(ns ru.petrsu.nest.yz.orientdb-em.core
  ^{:author "Vyacheslav Dimitrov"
    :doc "Implementation of the ElementManager for OrientDB"}
  (:require
   (ru.petrsu.nest.yz [core :as yz] [mom-utils :as mu]))
  (:import
    (com.orientechnologies.orient.object.db OObjectDatabaseTx)
    (ru.petrsu.nest.yz.core ElementManager))
  (:gen-class :name ru.petrsu.nest.yz.OrientEM
              :methods [;; JPA's element manager.
                        ^{:static true} 
                        [createOrientElementManager 
                         [com.orientechnologies.orient.object.db.OObjectDatabaseTx] 
                         ru.petrsu.nest.yz.core.ElementManager]]))


;; Implementation of JPA's ElementManager.
(deftype OrientElementManager [db]
  ElementManager
  ;; Implementation getElems's method. Root is created for 
  ;; specified class
  (^java.lang.Iterable getElems [_ ^Class claz]
     (.browseClass db claz))

  ;; Implementation getMom's method. Gets all classes from JPA's metamodel and
  ;; then gerenates MOM.
  (getMom [_] nil)


  ;; Value is got from bean of the object o.
  (^Object getPropertyValue [this ^Object o, ^String property]
     (get (bean o) (keyword property) :not-found)))


(defn ^ElementManager -createOrientElementManager
  "Returns implementation of JPA's ElementManager."
  [^OObjectDatabaseTx db]
  (OrientElementManager. db))


