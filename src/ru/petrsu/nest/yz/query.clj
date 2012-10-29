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

(ns ru.petrsu.nest.yz.query
  ^{:author "Vyacheslav Dimitrov"
    :doc "Wrapper for using queries from Java code.
         Query is helper class which is get Result and
         give methods for using its result."}
  (:import
    (ru.petrsu.nest.yz.core Result))
  (:gen-class :name ru.petrsu.nest.yz.Query
              :constructors {[ru.petrsu.nest.yz.core.Result] []}
              :methods [[getResultList [] java.util.List] 
                        [getSingleResult [] Object]
                        [getFlatResult [] java.util.List]
                        [getResult [] java.util.Map]
                        [getStructuredResult [] java.util.List]
                        [getError [] String]
                        [getColumnsName [] java.util.List]]
              :state state
              :init init))

(defn- getr
  "Returns :res value of state."
  [this]
  (:res @(.state this)))


(defn -init
  "Defines constructor."
  [^Result res]
  [[] (atom {:res res})])


(defn -getResult
  "Returns a map of the result query."
  [this]
  (getr this))


(defn -getResultList
  "Returns rows of query's result."
  [this]
  (:rows (getr this)))


(defn -getStructuredResult
  "Returns a value of :result key from map of the result query."
  [this]
  (:result (getr this)))


(defn -getError
  "Returns value of :error key from query's result."
  [this]
  (:error (getr this)))


(defn -getColumnsName
  "Returns value of :columns key from query's result."
  [this]
  (:columns (getr this)))


(defn -getSingleResult
  "Returns single result. If result is not single then
  exception is thrown."
  [this]
  (let [rows (:rows (getr this))]
    (if (or (not= (count rows) 1) (not= (count (nth rows 0)) 1))
      (throw (Exception. "Result is not single."))
      (nth (nth rows 0) 0))))


(defn -getFlatResult
  "Returns result as flat collection."
  [this]
  (flatten (-getResultList this)))
