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

(ns ru.petrsu.nest.yz.benchmark.benchmark
  ^{:author "Vyacheslav Dimitrov"
    :doc "System for benchmarking different types of queries."}
  (:use ru.petrsu.nest.yz.core)
  (:require [ru.petrsu.nest.yz.benchmark.bd-utils :as bu]
            [ru.petrsu.nest.yz.hb-utils :as hb]
            [ru.petrsu.nest.yz.parsing :as p]
            [ru.petrsu.nest.yz.benchmark.yz :as yz]
            [ru.petrsu.nest.yz.benchmark.hql :as hql]
            [ru.petrsu.nest.yz.queries.core :as qc]
            [ru.petrsu.nest.yz.queries.nest-queries :as nq]
            [ru.petrsu.nest.son.local-sm :as lsm]
            [clojure.java.io :as cio]
            [clojure.java.shell :as sh]
            [clojure.pprint :as cp])
  (:import (java.util Date)
           (ru.petrsu.nest.yz.core ElementManager)))


(defn- ^javax.persistence.EntityManager create-em
  "Returns EntityManager due to specified name (bench is default)."
  ([]
   (create-em "bench"))
  ([n]
   (.createEntityManager (javax.persistence.Persistence/createEntityManagerFactory n))))


(defn ^javax.persistence.EntityManager get-clean-bd
  "Creates entity manager, clean database 
  and generates some database's structure."
  []
  (let [emb (create-em)]
    (do (bu/schema-export)
      (bu/create-bd 1000 emb))
    emb))


(defn run-query
  "Runs specified query ('q') due to specified function ('f')
  which takes string-query and some EntityManager."
  [f q ^javax.persistence.EntityManager em]
  (if (nil? em)
   (let [em (get-clean-bd)
         t (run-query f q em)]
     (.close em)
     t)
    (f q em)))


(def ncount
  ^{:doc "Defines amount of evaluating set of queries. "}
  (identity 100))


(defn- add-seq
  "Adds two collection."
  [coll1 coll2]
  (map #(vec (map + %1 %2)) coll1 coll2))


(defn- getf
  "Returns function."
  [f]
  (resolve (symbol (str "ru.petrsu.nest.yz.benchmark." f "/do-q"))))

(comment
(defn -main
  "Takes a number of query and some settings and prints time of executing query."
  ([num, n, url, dialect, driver, ql]
   ((getf ql)
      num, n, 
      {"hibernate.connection.url" url, 
       "hibernate.dialect" dialect, 
       "hibernate.connection.driver_class" driver}))
  ([num, n, ql]
   ((getf ql) num, n, {})))
)

(defn bench-parsing
  "Beanchmark parsing. Executes parsing 
  for specified query 'n' times. "
  [n ^String query mom]
  (bu/btime (dotimes [_ n]
              (p/parse query mom))))


(defn bench-quering
  "Beanchmark quering. Executes parsing 
  for specified query 'n' times. "
  ([n ^String query mom]
   (bench-quering n query mom nil))
  ([n ^String query mom son-or-em]
   (let [son-or-em (if (nil? son-or-em) (bu/gen-bd 10000) son-or-em)
         em (if (instance? ElementManager son-or-em)
              son-or-em
              (qc/create-emm son-or-em))]
     (bu/btime (dotimes [_ n]
                 (pquery query mom em))))))


(defn bench
  "Returns string with result of parsing and quering.
  'n' is numbers of the execution the specified 'query'."
  ([n ^String query mom]
   (bench n query mom nil))
  ([n ^String query mom son-or-em]
   (let [time-q (bench-quering n query mom son-or-em)
         time-p (bench-parsing n query mom)]
     (str "Parsing: " time-p \newline
          "Quering: " time-q))))


(defn pr-bench
  "Prints result of benchmark for list of queries
  from yz/queries."
  [n mom]
  (let [son (bu/gen-bd 10000)]
    (println (reduce #(str %1 "q: " %2 \newline (bench n %2 mom son) \newline) "" yz/queries))))


(def ^:dynamic *f* "etc/yz-bench.txt")
(def ^:dynamic *q* (ref "")) 
(defn bench-to-file-old
  "Writes result of benchmark to file yz-bench.txt."
  [n mom]
  (let [son (bu/gen-bd 10000)
        new-res (reduce (fn [^String r, ^String line]
                          (str r (cond (.startsWith line ";") 
                                       (str ";" (dosync (ref-set *q* (.substring line 1))) \newline)
                                       (.startsWith line "Parsing") 
                                       (str line " " (bench-parsing n @*q* mom) \newline)
                                       (.startsWith line "Quering") 
                                       (str line " " (bench-quering n @*q* mom son) \newline)
                                       :else (str line \newline))))
                        "" 
                        (line-seq (cio/reader *f*)))]
    (cio/copy new-res (cio/file *f*))))


(defn -main
  "Main function for running benchmark from command 
  line (not repl). Need for jvisualvm."
  []
  (let [son (bu/gen-bd 10000)
        mom (hb/mom-from-file "nest.mom")]
    (reduce #(str %1 (bench 15 %2 mom son)) "" yz/queries)))


;; 
;; New version of structure of the file with benchmark's results.
;;
;; We will be creating two files for each result.
;;
;; First file tries represents result of the benchmark into queries versus
;; time of parsing and time of quering view:
;;  - first column is number of the benchmark.
;;  - second column is result of the parsing (in msec)
;;  - third column is result of the quering (in msec)
;;
;; Example:
;; ;query1 
;;  10. 244647.120965   7029.25291 
;;   9.   2047.120965 544729.25291 
;; ....
;;
;; ;query2
;;  10.    047.120965   7029.25291 
;;   9.   2047.120965 544729.25291 
;;
;;


(defn- get-fs
  "Returns formatted string with number of the benchmark, 
  amount of elements into bd, count of execution
  time of the parsing and time of the quering."
  [nb ptime qtime] 
  (cp/cl-format nil "~4D ~15,4F ~15,4F~%" nb ptime qtime))

(defn- get-num-bench
  "Returns a previous number of the benchmark."
  [f]
  (some #(if (.startsWith % "#count=")
           (Integer/parseInt (.substring % 7))
           nil) 
        (line-seq (cio/reader f))))


(defn bench-to-file
  "Writes result of benchmark to yz-bench-new.txt 
  file (by default) with results of benchmark. 
  Parameters:
    n - count of executing each query.
    mom - MOM of an object model.
    bd - SON element or ElementManager or count of 
        elements into BD (which is will be generated due to gen-bd function).
    f - file for result of the benchmark (and, of course, it must contains queries.)"
  ([mom]
   (bench-to-file 1000 10000 mom "etc/yz-bench-new.txt"))
  ([n mom]
   (bench-to-file n 10000 mom "etc/yz-bench-new.txt"))
  ([n bd mom]
   (bench-to-file n bd mom "etc/yz-bench-new.txt"))
  ([n bd mom f]
  (let [sdate (Date.) ; Date of starting the benchmark.
        bd ; Database
        (cond (number? bd) (qc/create-emm (bu/gen-bd bd))
              (instance? ElementManager bd) bd
              :else (qc/create-emm bd))
        cbd (ffirst (:rows (pquery "@(count `sonelement')" mom bd)))
        nb (inc (get-num-bench f)) ; Current number of the benchmark.
        new-res (reduce #(str %1 (cond (.startsWith %2 ";") 
                                       (str %2 \newline
                                            (let [q (.substring %2 1)]
                                              (get-fs nb 
                                                      (bench-parsing n q mom) 
                                                      (bench-quering n q mom bd))))
                                       (.startsWith %2 "#count=") (str "#count=" nb \newline)
                                       :else (str %2 \newline)))
                        "" 
                        (line-seq (cio/reader f)))
        edate (Date.) ; Date of ending the benchmark.

        ; Write info about benchmark in the end of file.
        new-res 
        (str new-res "# " nb ". " n 
             " " cbd " \"" sdate "\" \"" edate "\" " (class bd) " "
             ; Find a sha1 of the last commit and info about current machine.
             (try (:out (sh/sh "./info.sh"))
               (catch Exception e ""))
             \newline)]
    (cio/copy new-res (cio/file f)))))


(defn bench-for-nest-queries
  "Benchmark for queries from Nest project."
  ([mom] 
   (bench-for-nest-queries mom (lsm/create-lsm)))
  ([mom bd]
   (let [bp #(bench-parsing 1 % mom) 
         bq #(bench-quering 1 % mom bd)
         f (fn [bf queries] (reduce  #(+ %1 (bf %2)) 0 queries))
         ptime-ai (f bp nq/address-info-queries)
         qtime-ai (f bq nq/address-info-queries)
         ptime-e (f bp nq/enlivener-queries)
         qtime-e (f bq nq/enlivener-queries)]
     (str "ParsingAI: " ptime-ai \newline "QueringAI: " qtime-ai \newline
          "ParsingE: " ptime-e \newline "QueringE: " qtime-e))))

