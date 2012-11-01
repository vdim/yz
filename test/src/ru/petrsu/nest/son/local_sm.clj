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

(ns ru.petrsu.nest.son.local-sm
  ^{:author "Vyacheslav Dimitrov"
    :doc "Clojure's implementation of the LocalSonManager from Nest
         project. This is needed for correct order of the compiling."}
  (:require
    (net.kryshen.planter [store :as ps] [core :as pc]) 
    (ru.petrsu.nest [son :as sn]))
  (:import
    (ru.petrsu.nest.yz.core ElementManager)))


(defn ^ElementManager create-lsm
  "Returns LocalSonManager."
  [store]
  (reify ElementManager
    (^java.lang.Iterable getElems [_ ^Class claz] 
       (ps/instances-of store claz))
    (getMom [_] 
       (throw (UnsupportedOperationException. "Not supported yet.")))
    
    (^Object getPropertyValue [this ^Object o, ^String property]
       (if (= property "id")
         (pc/bean-id o)
         ((keyword property) (pc/properties o))))))
