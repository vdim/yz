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

(ns ru.petrsu.nest.yz.yz-factory
  ^{:author "Vyacheslav Dimitrov"
    :doc "Factory for creating different types of the ElementManager 
         (see code from the core.clj file.).

         At the present moment factory supports the following ElementManagers:
          - JPA's ElementManager (method createJPAElementManager). For getting it
            you must pass your javax.persistence.EntityManager.

          - Memory's ElementManager (method createMemoryElementManager). 
            It is simple element manager which keeps elements
            direct in memory. It may be useful for the testing your model or some YZ's query.
            For getting it you must pass your list of classes (for creating the MOM)."}
  (:require
   (ru.petrsu.nest.yz [core :as yz] [hb-utils :as hu]))
  (:import
    (javax.persistence EntityManager)
    (javax.persistence.criteria Root CriteriaBuilder)
    (ru.petrsu.nest.yz.core ElementManager)
    (java.util List))
  (:gen-class :name ru.petrsu.nest.yz.ElementManagerFactory
              :methods [;; JPA's element manager.
                        ^{:static true} 
                        [createJPAElementManager 
                         [javax.persistence.EntityManager] 
                         ru.petrsu.nest.yz.core.ElementManager]

                        ;; Memory's element manager.
                        ^{:static true} 
                        [createMemoryElementManager 
                         [java.util.List] 
                         ru.petrsu.nest.yz.core.ElementManager]]))


(defn ^EntityManager -createJPAElementManager
  "Implementation of JPA's ElementManager."
  [^EntityManager em]
  (reify ElementManager

    ;; Implementation getElems's method. Root is created for 
    ;; specified class
    (^java.util.Collection getElems [_ ^Class claz] 
       (let [^CriteriaBuilder cb (.getCriteriaBuilder em)
             cr (.createTupleQuery cb)
             ^Root root (. cr (from claz))
             cr (.. cr (multiselect [root]) (distinct true))]
         (map #(.get % 0) (.. em (createQuery cr) getResultList))))

    ;; Implementation getClasses's method. Gets all classes from JPA's metamodel.
    (getClasses [_] (map #(.getJavaType %) 
                         (.. em getEntityManagerFactory getMetamodel getEntities)))))


(defn ^EntityManager -createMemoryElementManager
  "Implementation of memory's ElementManager."
  [^List classes]
  (reify ElementManager
    (^java.util.Collection getElems [_ ^Class claz] 
       (throw (UnsupportedOperationException. "Not supported yet.")))
    (getClasses [_] classes)))


