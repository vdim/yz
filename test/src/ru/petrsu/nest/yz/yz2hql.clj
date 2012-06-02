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

(ns ru.petrsu.nest.yz.yz2hql
  ^{:author "Vyacheslav Dimitrov"
    :doc "Naive and stupid convertor from YZ query to 
         HQL query. Works only for the SON model and 
         queries from ru.petrsu.nest.yz.test-parsing/qlist."}
  (:use ru.petrsu.nest.yz.parsing))


(defn yz2hql
  "Takes an YZ query and returns proper HQL query."
  [yz-q]
  (let [rp (first (parse yz-q))
        gs-what (name (gensym "g"))
        select (if (:props rp)
                 (reduce #(str %1 " " (first %2)) "" (:props rp))
                 gs-what)
        from (str (.getSimpleName (:what rp)) " as " gs-what)
        where (if (nil? (:where rp))
                ""
                "where ")]
    (str "select " select " from " from where)))
