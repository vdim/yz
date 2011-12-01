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

(ns ru.petrsu.nest.son.local-sm
  ^{:author "Vyacheslav Dimitrov"
    :doc "Clojure's implementation of the LocalSonManager from Nest
         project. This is needed for correct order of the compiling."}
  (:require
    (ru.petrsu.nest.yz [core :as yz] [hb-utils :as hu])
    (net.kryshen.planter [core :as planter]) 
    (ru.petrsu.nest [son :as sn]))
  (:import
    (ru.petrsu.nest.yz.core ElementManager)
    (ru.petrsu.nest.son SON)))


(defn ^ElementManager create-lsm
  "Returns LocalSonManager."
  []
  (reify ElementManager
    (^java.util.Collection getElems [_ ^Class claz] 
       (planter/instances-of claz))
    (getClasses [_] ())
    
    (^Object getPropertyValue [this ^Object o, ^String property]
       (planter/get-value o property))))
