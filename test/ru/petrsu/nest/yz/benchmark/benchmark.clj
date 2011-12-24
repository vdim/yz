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
  (:use ru.petrsu.nest.yz.core
        incanter.stats)
  (:require [ru.petrsu.nest.yz.benchmark.bd-utils :as bu]
            [ru.petrsu.nest.yz.hb-utils :as hb]
            [ru.petrsu.nest.yz.parsing :as p]
            [ru.petrsu.nest.yz.benchmark.yz :as yz]
            [ru.petrsu.nest.yz.benchmark.hql :as hql]
            [ru.petrsu.nest.yz.queries.core :as qc]
            [ru.petrsu.nest.yz.test-parsing :as tp]
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

(defn- do-times
  "Returns sequence of time (n times) of execution some function f."
  [f, n]
  (map (fn [_] (bu/btime (f))) (repeat n 0)))


(defn bench-parsing
  "Beanchmark parsing. Executes the parse function
  for specified query and mom 'n' times. Returns total time."
  [n ^String query mom]
    (apply + (do-times (partial p/parse query mom) n)))


(defn bench-quering
  "Beanchmark quering. Executes the pquery function for specified query, 
  bd and mom 'n' times. If bd is nil then bd is generated with 10000 elements.
  Returns sequence: 
    (total time, average time, quntile(5%), quntile(50%), quntile(90%))."
  ([n ^String query mom]
   (bench-quering n query mom nil))
  ([n ^String query mom son-or-em]
   (let [son-or-em (if (nil? son-or-em) (bu/gen-bd 10000) son-or-em)
         em (if (instance? ElementManager son-or-em)
              son-or-em
              (qc/create-emm son-or-em))
         times (do-times (partial pquery query mom em) n)
         s-times (apply + times)]
     (concat [s-times (/ s-times n)] (quantile times :probs [0.05, 0.5 0.9])))))


(defn bench
  "Returns vector with result of parsing (first) and quering (second).
  'n' is a number of the execution the specified 'query'.
  For quering bd is needed. If bd is nil then bd is generated with
  10000 elements."
  ([n ^String query mom]
   (bench n query mom nil))
  ([n ^String query mom son-or-em]
   (let [time-q (bench-quering n query mom son-or-em)
         time-p (bench-parsing n query mom)]
     [time-p time-q])))


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
  [nb ptime [qtime sa p5 p50 p90]] 
  (cp/cl-format nil "~4D ~15,4F ~15,4F ~15,4F ~15,4F ~15,4F ~15,4F~%" 
                nb ptime qtime sa p5 p50 p90))


(defn get-num-bench
  "Returns a previous number of the benchmark."
  [f]
  (some #(if (.startsWith % "#count=")
           (Integer/parseInt (.substring % 7))
           nil) 
        (line-seq (cio/reader f))))


(defn- write-to-file
  "Writes result of benchmark to file with results of benchmark. 
  Parameters:
    n - count of executing each query.
    mom - MOM of an object model.
    bd - SON element or ElementManager or count of 
        elements into BD (which is will be generated due to gen-bd function).
    f - file for result of the benchmark (and, of course, it must contains queries.)
    bench-fn - function which takes some string and returns result of benchmark."
  [n bd mom f bench-fn]
  (let [sdate (Date.) ; Date of starting the benchmark.
        bd ; Database
        (cond (number? bd) (qc/create-emm (bu/gen-bd bd))
              (instance? ElementManager bd) bd
              :else (qc/create-emm bd))
        cbd (ffirst (:rows (pquery "@(count `sonelement')" mom bd)))
        nb (inc (get-num-bench f)) ; Current number of the benchmark.
        new-res (reduce #(str %1 (cond (.startsWith %2 ";") 
                                       (str %2 \newline (bench-fn %2 bd nb))
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
    (cio/copy new-res (cio/file f))))


(def bench-file
  "Name of the default file for results of benchmark."
  "etc/yz-bench-new.txt")


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
   (bench-to-file mom 10000 1000 bench-file))
  ([mom bd]
   (bench-to-file mom bd 1000 bench-file))
  ([mom bd n]
   (bench-to-file mom bd n bench-file))
  ([mom bd n f]
  (write-to-file n bd mom f 
                 #(let [q (.substring %1 1)]
                    (get-fs %3
                            (bench-parsing n q mom) 
                            (bench-quering n q mom %2))))))


(defn- bench-for-list
  "Takes info about benchmark and list with queries and
  returns vector with two elements where first is time of the
  parsing and second is time the querying."
  [mom bd n qlist]
  [(reduce #(+ %1 (bench-parsing n %2 mom)) 0 qlist)
   (reduce #(map + %1 (bench-quering n %2 mom bd)) [0 0 0 0 0] qlist)])


(defn bench-for-nest-queries
  "Benchmark for queries from Nest project + qlist from test-parsing.clj. Parameters: 
      - mom - a map of an object model (mandatory).
      - bd - instance of the SON or instance of ElementManager's implementation 
            (local son manager by default).
      - n - times of execution each list with queries (1 by default)."
  ([mom] 
   (bench-for-nest-queries mom (lsm/create-lsm) 1))
  ([mom bd]
   (bench-for-nest-queries mom bd 1))
  ([mom bd n]
   (let [[p-ai q-ai] (bench-for-list mom bd n nq/address-info-queries)
         [p-e q-e] (bench-for-list mom bd n nq/enlivener-queries)
         [p-tp q-tp] (bench-for-list mom bd n  tp/qlist)]
     (str "ParsingAI: " p-ai \newline "QueringAI: " q-ai \newline 
          "ParsingE: " p-e \newline "QueringE: " q-e \newline 
          "ParsingTP: " p-tp \newline "QueringTP: " q-tp))))


(def bench-list-file
  "Name of default file for result of benchmark list with queries."
  "etc/yz-bench-list.txt")


(defn bench-list-to-file
  "Benchmark for queries from Nest project + qlist from test-parsing.clj. Parameters: 
      - mom - a map of an object model (mandatory).
      - bd - instance of the SON or instance of ElementManager's implementation 
            (local son manager by default) or count of elements into bd which will be 
            generated.
      - n - times of execution each list with queries (1 by default).
      - f - name of file for result (etc/yz-bench-list.txt by default).
  Use nil for indication value of parameter as default."
  ([mom] 
   (bench-list-to-file mom (lsm/create-lsm) 1 bench-list-file))
  ([mom bd]
   (bench-list-to-file mom bd 1 bench-list-file))
  ([mom bd n]
   (bench-list-to-file mom bd n bench-list-file))
  ([mom bd n f]
   (let [bd (if (nil? bd) (lsm/create-lsm) bd)
         n (if (nil? n) 1 n)
         f (if (nil? f) bench-list-file f)]
     (write-to-file n bd mom f
                    #(let [ql (.get (some (fn [ns-] (ns-resolve ns- (symbol (.substring %1 1)))) (all-ns)))
                           rb (bench-for-list mom %2 n ql)] 
                       (get-fs %3 (rb 0) (rb 1)))))))
