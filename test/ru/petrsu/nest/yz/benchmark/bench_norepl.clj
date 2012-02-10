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

(ns ru.petrsu.nest.yz.benchmark.bench-norepl
  ^{:author "Vyacheslav Dimitrov"
    :doc "Running benchmark from command line for specified list of commits."}
  (:require [ru.petrsu.nest.yz.benchmark.benchmark :as bb] 
            [ru.petrsu.nest.yz.benchmark.bd-utils-old :as buo] 
            [ru.petrsu.nest.yz.benchmark.bd-utils :as bu] 
            [ru.petrsu.nest.yz.queries.core :as qc])
  (:use ru.petrsu.nest.yz.hb-utils)
  (:import (javax.persistence Persistence)))


(defn -main
  "Running benchmark due to specified parameters for benchmark (fmom bd n f l).
  modificator defines whether we run bench-for-list-file or bench-to-file."
  ([fmom bd n f]
   (-main fmom bd n f ""))
  ([fmom bd n f modificator]
   (let [b-mom (mom-from-file fmom)]
     (case modificator
       "list" (bb/bench-list-to-file b-mom (Integer/parseInt bd) (Integer/parseInt n) f)

       "jpa-list" (let [em (.createEntityManager (Persistence/createEntityManagerFactory "nest-old"))
                        _ (buo/create-bd (Integer/parseInt bd) em)] 
                    (bb/bench-list-to-file b-mom em (Integer/parseInt n) f))

       "jpa" (let [em (.createEntityManager (Persistence/createEntityManagerFactory "nest-old"))
                   _ (buo/create-bd (Integer/parseInt bd) em)] 
               (bb/bench-to-file b-mom em (Integer/parseInt n) f))
       
       "lsm-list" (do (qc/create-emlm (bu/gen-bd (Integer/parseInt bd))) 
                    (bb/bench-list-to-file b-mom nil (Integer/parseInt n) f))
       
       "lsm" (do (qc/create-emlm (bu/gen-bd (Integer/parseInt bd))) 
                    (bb/bench-to-file b-mom nil (Integer/parseInt n) f))
       
       "hql" (bb/bench-list-to-file-hql (Integer/parseInt bd) (Integer/parseInt n) f)

       (bb/bench-to-file b-mom (Integer/parseInt bd) (Integer/parseInt n) f)))
   (System/exit 0)))

