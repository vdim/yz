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

(ns ru.petrsu.nest.yz.pquery
  ^{:author "Vyacheslav Dimitrov"
    :doc "Wrapper for using parametrized queries from Java code."}
  (:require
    (ru.petrsu.nest.yz [core :as yz] [mom-utils :as mu] [parsing :as p]))
  (:import
    (ru.petrsu.nest.yz.core ElementManager) 
    (ru.petrsu.nest.yz Query) 
    (java.util List))
  (:gen-class :name ru.petrsu.nest.yz.ParametrizedYZQuery
              :constructors {; MOM is nil, String is query
                             [ru.petrsu.nest.yz.core.ElementManager String] [], 
                             ; First string is name file for MOM, second string is query.
                             [String ru.petrsu.nest.yz.core.ElementManager String] []}
              :methods [[execute [Object] ru.petrsu.nest.yz.Query]]
              :state state
              :init init))


;; Defines map of the object model (MOM).
(def ^:dynamic *mom* (atom nil))

(defn- create-state
  "Creates state due to em. If f-mom isn't nil then
  MOM is extracted from file."
  [^ElementManager em, f-mom, ^String query]
  (let [mom (if (nil? @*mom*) 
              (reset! *mom* 
                      (if (nil? f-mom)
                        (.getMom em)
                        (mu/mom-from-file f-mom)))
              @*mom*)]
    (atom {:em em 
           :mom mom
           :parse-res (p/parse query mom)})))


(defn -init
  "Defines constructors."
  ([^ElementManager em ^String query]
   [[] (create-state em nil query)])
  ([^String mom ^ElementManager em ^String query]
   [[] (create-state em mom query)]))


(defn -execute
  [this params]
  (let [params (if (coll? params) params [params])
        _ (reset! p/query-params params)
        {:keys [parse-res em mom]} @(.state this)]
    (Query. (yz/get-qr parse-res mom em))))
