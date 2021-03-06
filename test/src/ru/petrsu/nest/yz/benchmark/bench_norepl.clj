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

(ns ru.petrsu.nest.yz.benchmark.bench-norepl
  ^{:author "Vyacheslav Dimitrov"
    :doc "Runs benchmark from command line for specified list of parameters."}
  (:require [ru.petrsu.nest.yz.benchmark.benchmark :as bb] 
            [ru.petrsu.nest.yz.benchmark.bd-utils-jpa :as buj] 
            [ru.petrsu.nest.yz.benchmark.bd-utils :as bu] 
            [net.kryshen.planter.store :as store] 
            [ru.petrsu.nest.yz.queries.core :as qc])
  (:use ru.petrsu.nest.yz.mom-utils ru.petrsu.nest.son.local-sm)
  (:import (javax.persistence Persistence)))


(defn -main
  "Runs benchmark due to specified parameters for benchmark (fmom bd n f):
      fmom - path to a file with map of model.
      bd - amount elements of DB.
      n - count of execution times.
      f - file with query (or list of queries) and for result of benchmark.
    Another parameters:
      mod-bd defines type of database (jpa, lsm, lsm-gen mem). 
      func defines type of function (list, ind).
      lang defines language (hql or yz)."
  [fmom bd n f mod-bd func lang]
  (let [b-mom (mom-from-file fmom)
        n (Integer/parseInt n)
        bd (Integer/parseInt bd)
        bd (case mod-bd
             "jpa" (buj/create-bd 
                     bd (.createEntityManager (Persistence/createEntityManagerFactory "nest-old")))
             "lsm" (create-lsm (store/store (str "data-"bd)))
             "lsm-gen" (qc/create-emlm (bu/gen-bd bd))
             "mem" bd
             nil)]
    (cond 
      (and (= func "list") (= lang "yz")) (bb/bench-list-to-file b-mom bd n f) 
      (and (= func "list") (= lang "hql")) (bb/bench-list-to-file-hql bd n f)
      (and (= func "ind") (= lang "yz")) (bb/bench-to-file b-mom bd n f)
      (and (= func "ind") (= lang "hql")) (bb/bench-to-file-hql bd n f)))
  (System/exit 0))

