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
  (:require [ru.petrsu.nest.yz.benchmark.benchmark :as bb] )
  (:use ru.petrsu.nest.yz.hb-utils))


(defn -main
  "Running benchmark due to specified parameters for benchmark (fmom bd n f l).
  l? defines weather we run bench-for-list-file or bench-to-file."
  ([fmom bd n f]
   (-main fmom bd n f nil))
  ([fmom bd n f l?]
   (let [b-mom (mom-from-file fmom)]
     (if l?
       (bb/bench-list-to-file b-mom (Integer/parseInt bd) (Integer/parseInt n) f)
       (bb/bench-to-file b-mom (Integer/parseInt bd) (Integer/parseInt n) f)))
   (System/exit 0)))

