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

(ns ru.petrsu.nest.yz.yz
  ^{:author "Vyacheslav Dimitrov"
    :doc "Wrapper for using the YZ from Java code."}
  (:require
   (ru.petrsu.nest.yz [core :as yz] [mom-utils :as mu]))
  (:import
    (ru.petrsu.nest.yz.core ElementManager)
    (ru.petrsu.nest.yz Query))
  (:gen-class :name ru.petrsu.nest.yz.YZQuery
              :constructors {; MOM is nil.
                             [ru.petrsu.nest.yz.core.ElementManager] [], 
                             ; String is name of file with MOM.
                             [String ru.petrsu.nest.yz.core.ElementManager] []}
              :methods [[create [String] ru.petrsu.nest.yz.Query]]
              :state state
              :init init))

;; Defines map of the object model (MOM).
(def ^:dynamic *mom* (atom nil))


(defn- create-state
  "Creates state due to em. If f-mom isn't nil then
  MOM is extracted from file."
  [^ElementManager em f-mom]
  (atom {:em em 
         :mom (if (nil? @*mom*) 
                (reset! *mom* 
                        (if (nil? f-mom)
                          (.getMom em)
                          (mu/mom-from-file f-mom)))
                @*mom*)}))


(defn -init
  "Defines constructors."
  ([^ElementManager em]
   [[] (create-state em nil)])
  ([^String f ^ElementManager em]
   [[] (create-state em f)]))


(defn -create
  "Creates object Query for specified string query."
  [this ^String query]
  (let [{:keys [em mom]} @(.state this)]
    (Query. (yz/pquery query mom em))))
