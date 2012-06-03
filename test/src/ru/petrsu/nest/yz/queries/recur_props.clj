;;
;; Copyright 2011-2012 Vyacheslav Dimitrov <vyacheslav.dimitrov@gmail.com>
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

(ns ru.petrsu.nest.yz.queries.recur-props
  ^{:author "Vyacheslav Dimitrov"
    :doc "Processes queries within recur properties."}
  (:use ru.petrsu.nest.yz.core 
        clojure.test)
  (:require [ru.petrsu.nest.yz.queries.core :as tc])
  (:import (ru.petrsu.nest.son SON Occupancy SimpleOU CompositeOU)))


;; Define model
;;
;; Enterprise
;;    -> IT Department
;;        -> Database
;;            -> MySQL
;;            -> Oracle
;;            -> NoSQL
;;        -> Web-site
;;            -> Online Shop
;;            -> Inner Web
;;        -> Network
;;            -> Local Network
;;    -> Management Department
;;    -> Finance Department
;;        -> Insure Department
;;        -> Fee Department
;;

(def main_cou (doto (CompositeOU.)
                (.setName "Enterprise")))

(def it_cou (doto (CompositeOU.)
              (.setName "IT Department")
              (.setParent main_cou)))

(def man_cou (doto (CompositeOU.)
               (.setName "Management Department")
               (.setParent main_cou)))

(def fin_cou (doto (CompositeOU.)
               (.setName "Finance Department")
               (.setParent main_cou)))

(def bd_cou (doto (CompositeOU.)
              (.setName "Database")
              (.setParent it_cou)))

(def web_cou (doto (CompositeOU.)
              (.setName "Web-site")
              (.setParent it_cou)))

(def net_cou (doto (CompositeOU.)
              (.setName "Network")
              (.setParent it_cou)))

(def mysql_sou (doto (SimpleOU.)
              (.setName "MySQL")
              (.setParent bd_cou)))

(def oracle_sou (doto (SimpleOU.)
              (.setName "Oracle")
              (.setParent bd_cou)))

(def nosql_sou (doto (SimpleOU.)
              (.setName "NoSQL")
              (.setParent bd_cou)))

(def shop_sou (doto (SimpleOU.)
              (.setName "Online Shop")
              (.setParent web_cou)))

(def inner_sou (doto (SimpleOU.)
              (.setName "Inner Web")
              (.setParent web_cou)))

(def local_sou (doto (SimpleOU.)
              (.setName "Local Network")
              (.setParent net_cou)))

(def insure_cou (doto (CompositeOU.)
               (.setName "Insure Department")
               (.setParent fin_cou)))

(def fee_cou (doto (CompositeOU.)
               (.setName "Fee Department")
               (.setParent fin_cou)))




(def son (doto (SON.) (.setRootOU main_cou))) 



;; Define entity manager.

(use-fixtures :once (tc/setup-son son))

;; Common function.
(defn- f1 
  "Compares flattened rows of a specified 
  query and a specified result."
  [q r]
  (tc/eq-colls (flatten (tc/rows-query q)) r))


(defn- f2 
  "Compares result of a specified query and some value."
  [q v] 
  (tc/eq-colls (tc/r-query q) v))


(defn- f3 
  "Compares rows of a specified 
  query and a specified result."
  [q r]
  (tc/eq-colls (tc/rows-query q) r))


;; Define tests

(deftest t-recur-parent
         (is (f1 "sou[parent]" [net_cou web_cou web_cou bd_cou bd_cou bd_cou]))
         (is (f1 "sou#(name=\"Local Network\")[parent]" [net_cou]))
         (is (f1 "sou#(name=\"Local Network\")[*parent]" [net_cou it_cou main_cou]))

         (is (f2 "sou#(name=\"Local Network\")[parent]" [(list net_cou) []]))
         (is (f2 "sou#(name=\"Local Network\")[*parent]" [(list (list net_cou it_cou main_cou)) []]))
         (is (f2 "sou#(name=\"Local Network\")[*parent]" [(list (list main_cou net_cou it_cou)) []]))
         (is (f2 "sou#(name=\"Local Network\")[& *parent]" [(list local_sou (list net_cou it_cou main_cou)) []]))
         (is (f2 "sou#(name=\"Local Network\")[*parent &]" [(list (list net_cou it_cou main_cou) local_sou) []]))
         (is (f2 "sou#(name=\"Local Network\")[name & *parent]" 
                 [(list "Local Network" local_sou (list net_cou it_cou main_cou)) []]))
         (is (f2 "sou#(name=\"Local Network\")[*parent & name]" 
                 [(list (list net_cou it_cou main_cou) local_sou "Local Network") []])))


(deftest t-recur-ous
         (is (f1 "cou#(name~\"base\")[*OUs]" [mysql_sou oracle_sou nosql_sou]))
         (is (f1 "cou#(name~\"base\")[& *OUs]" [bd_cou mysql_sou oracle_sou nosql_sou]))
         (is (f2 "cou#(name~\"base\")[*OUs]" 
                 [(list (list (list mysql_sou oracle_sou nosql_sou))) []]))
         (is (f2 "cou#(name~\"base\")[& *OUs]" 
                 [(list bd_cou (list (list mysql_sou oracle_sou nosql_sou))) []]))
         (is (f3 "cou#(name~\"base\")[*OUs]" [(list mysql_sou oracle_sou nosql_sou)]))
         (is (f3 "cou#(name~\"base\")[& *OUs]" [(list bd_cou mysql_sou oracle_sou nosql_sou)]))


         (is (f1 "cou#(name~\"IT\")[*OUs]" [bd_cou mysql_sou oracle_sou nosql_sou
                                            web_cou shop_sou inner_sou
                                            net_cou local_sou]))
         (is (f1 "cou#(name~\"IT\")[& *OUs]" [it_cou
                                              bd_cou mysql_sou oracle_sou nosql_sou
                                              web_cou shop_sou inner_sou
                                              net_cou local_sou]))
         (is (f2 "cou#(name~\"IT\")[*OUs]" 
                 [(list (list (list web_cou net_cou bd_cou)
                              [shop_sou inner_sou
                               nosql_sou oracle_sou mysql_sou
                               local_sou]))
                  []]))
         (is (f2 "cou#(name~\"IT\")[& *OUs]" 
                 [(list it_cou (list (list web_cou net_cou bd_cou)
                                     [shop_sou inner_sou
                                      nosql_sou oracle_sou mysql_sou
                                      local_sou]))
                  []]))
         (is (f3 "cou#(name~\"IT\")[*OUs]" [(list bd_cou mysql_sou oracle_sou nosql_sou
                                                  web_cou shop_sou inner_sou
                                                  net_cou local_sou)]))
         (is (f3 "cou#(name~\"IT\")[& *OUs]" [(list it_cou bd_cou mysql_sou oracle_sou nosql_sou
                                                    web_cou shop_sou inner_sou
                                                    net_cou local_sou)]))


         (is (f3 "cou[*OUs]" [[it_cou man_cou fin_cou 
                               bd_cou mysql_sou oracle_sou nosql_sou
                               web_cou shop_sou inner_sou
                               net_cou local_sou 
                               insure_cou fee_cou]
                              [bd_cou mysql_sou oracle_sou nosql_sou
                               web_cou shop_sou inner_sou
                               net_cou local_sou]
                              [mysql_sou oracle_sou nosql_sou]
                              [local_sou]
                              [shop_sou inner_sou]
                              [insure_cou fee_cou]
                              [nil]
                              [nil]
                              [nil]]))
         (is (f3 "cou[& *OUs]" [[main_cou
                                 it_cou man_cou fin_cou 
                                 bd_cou mysql_sou oracle_sou nosql_sou
                                 web_cou shop_sou inner_sou
                                 net_cou local_sou 
                                 insure_cou fee_cou]
                                [it_cou
                                 bd_cou mysql_sou oracle_sou nosql_sou
                                 web_cou shop_sou inner_sou
                                 net_cou local_sou]
                                [bd_cou mysql_sou oracle_sou nosql_sou]
                                [net_cou local_sou]
                                [web_cou shop_sou inner_sou]
                                [fin_cou insure_cou fee_cou]
                                [man_cou nil]
                                [insure_cou nil]
                                [fee_cou nil]])))
