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

(ns ru.petrsu.nest.yz.benchmark.benchmark
  ^{:author "Vyacheslav Dimitrov"
    :doc "System for benchmarking different types of queries."}
  (:use ru.petrsu.nest.yz.core 
        ru.petrsu.nest.yz.yz-factory 
        ru.petrsu.nest.yz.hb-utils
        incanter.stats)
  (:require [ru.petrsu.nest.yz.benchmark.bd-utils :as bu] 
            [ru.petrsu.nest.yz.benchmark.bd-utils-jpa :as buj]
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
            [net.kryshen.planter.store :as store]
            [clojure.pprint :as cp]
            [clojure.string :as cs]
            [criterium.core :as cr])
  (:import (java.util Date)
           (ru.petrsu.nest.yz.core ElementManager) 
           (java.lang.management ManagementFactory)
           (javax.persistence Persistence EntityManager)))


(def vprobs
  "Defines vector with probabilities."
  [0.05 0.5 0.9])


(defn ^javax.persistence.EntityManager create-em
  "Returns EntityManager due to specified name (bench is default)."
  ([]
   (create-em "bench" {}))
  ([n]
   (create-em n {}))
  ([n m]
   (if (empty? m)
     (.createEntityManager (Persistence/createEntityManagerFactory n)))
     (.createEntityManager (Persistence/createEntityManagerFactory n m))))


(defn ^javax.persistence.EntityManager get-clean-bd
  "Creates entity manager, clean database 
  and generates some database's structure."
  []
  (let [emb (create-em)]
    (do (buj/schema-export)
      (buj/create-bd 1000 emb))
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

(def ^:dynamic *measurement* 
  "Defines measurement for benchmarking. It may be:
    - :time (by default);
    - :thread-time-cpu; 
    - :thread-time-user;
    - :memory."
  :time)


(def ^:dynamic *idle-count* 
  "Defines count of idle calling query. 0 by default"
  0)


(def ^:dynamic *cr?*
  "Defines whether benchmark is done 
  through criterium library. false by default."
  false)


(defn- do-times-q
  "Returns sequence of time (n times) of execution some function f."
  [f, n]
  (dorun (repeatedly *idle-count* f)) ; idle calls of function f
  (repeatedly n #(let [[t r] (case *measurement*
                               :thread-time-cpu (bu/thread-time (f) :cpu)
                               :thread-time-user (bu/thread-time (f) :user)
                               :memory (bu/thread-memory (f))
                               :time (if *cr?*
                                       (let [b (cr/benchmark (f))]
                                         ; :mean returns vector where first element is mean in nanoseconds
                                         ; and second element is confidence interval. 
                                         ; We must have milliseconds.
                                         [(* 1000000 ((:mean b) 0)) 
                                          (f)])
                                       (bu/brtime (f)))
                               (throw (Exception. (str "Unknown type of measurement: " (name *measurement*)))))]
                   (if (nil? (:error r)) t (throw (Exception. (:error r)) )))))


(defn bench-parsing
  "Beanchmark parsing. Executes the parse function
  for specified query and mom 'n' times. Returns total time."
  [n ^String query mom]
  (apply + (repeatedly n #(bu/btime (p/parse query mom)))))


(defn bench-quering
  "Beanchmark quering. Executes the pquery function for specified query, 
  bd and mom 'n' times. If bd is nil then bd is generated with 10000 elements.
  Returns sequence: 
    (total time, average time, quntile(5%), quntile(50%), quntile(90%))."
  ([n ^String query mom]
   (bench-quering n query mom nil))
  ([n ^String query mom son-or-em]
   (let [son-or-em (cond (nil? son-or-em) (bu/gen-bd 10000) 
                         (number? son-or-em) (bu/gen-bd son-or-em)
                         :else son-or-em)
         em (if (instance? ElementManager son-or-em)
              son-or-em
              (qc/em-memory son-or-em mom))

         times (do-times-q (partial pquery query mom em) n)
         s-times (apply + times)
         c-times (count times)]
     (concat [s-times (/ s-times c-times)] (quantile times :probs vprobs)))))


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

(comment
(defn -main
  "Main function for running benchmark from command 
  line (not repl). Need for jvisualvm."
  []
  (let [son (bu/gen-bd 10000)
        mom (hb/mom-from-file "nest.mom")]
    (reduce #(str %1 (bench 15 %2 mom son)) "" yz/queries)))
)

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


(defn- get-fs
  "Returns formatted string with specified characteristic.
  If format? is supplied and false then returns simple
  unformatted string with charactiristics througth space."
  ([nb ptime charas]
   (get-fs nb ptime charas true))
  ([nb ptime charas format?]
   (if format?
     (apply cp/cl-format 
            nil (str "~4D ~15,4F" (apply str (repeat (count charas) " ~15,4F")) "~%") 
            nb ptime charas)
     (str (reduce #(str %1 " " %2) (str nb " " ptime) charas) \newline))))


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
    hql? - defines whether benchmark is done for HQL language (if hql? is then bd must be number).
    bench-fn - function which takes some string and returns result of benchmark."
  [n bd mom f hql? bench-fn]
  (let [sdate (Date.) ; Date of starting the benchmark.
        bd ; Database
        (cond (or hql? (instance? ElementManager bd)) bd
              (number? bd) (qc/em-memory (bu/gen-bd bd) mom) 
              (instance? EntityManager bd) (-createJPAElementManager bd)
              :else (qc/em-memory bd mom))
        cbd (if hql? bd (((:result (pquery "@(count `sonelement')" mom bd)) 0) 0))
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
   (let [bd (if (nil? bd) (lsm/create-lsm (store/store "data")) bd)
         n (if (nil? n) 1 n)
         f (if (nil? f) bench-file f)]
     (write-to-file n bd mom f false
                    #(let [q (.substring %1 1)]
                       (get-fs %3
                               (bench-parsing n q mom) 
                               (bench-quering n q mom %2)))))))


(defn bench-for-list
  "Takes info about benchmark and list with queries and
  returns vector with two elements where first is time of
  parsing and second is vector with characteristics of querying."
  [n qlist mom bd]
  [(reduce #(+ %1 (bench-parsing n %2 mom)) 0 qlist)
   (reduce #(map + %1 (bench-quering n %2 mom bd)) [0 0 0 0 0] qlist)])


(defn bench-for-nest-queries
  "Benchmark for queries from Nest project + qlist from test-parsing.clj. Parameters: 
      - mom - a map of an object model (mandatory).
      - bd - instance of the SON or instance of ElementManager's implementation 
            (local son manager by default).
      - n - times of execution each list with queries (1 by default)."
  ([mom] 
   (bench-for-nest-queries mom (lsm/create-lsm (store/store "data")) 1))
  ([mom bd]
   (bench-for-nest-queries mom bd 1))
  ([mom bd n]
   (let [[p-ai q-ai] (bench-for-list n nq/address-info-queries mom bd)
         [p-e q-e] (bench-for-list n nq/enlivener-queries mom bd)
         [p-tp q-tp] (bench-for-list n tp/qlist mom bd)]
     (str "ParsingAI: " p-ai \newline "QueringAI: " q-ai \newline 
          "ParsingE: " p-e \newline "QueringE: " q-e \newline 
          "ParsingTP: " p-tp \newline "QueringTP: " q-tp))))


(def bench-list-file
  "Name of default file for result of benchmark list with queries (YZ language)."
  "etc/yz-bench-list.txt")

(def bench-list-file-hql
  "Name of default file for result of benchmark list with queries (HQL language)."
  "etc/hql-bench-list.txt")


(defn- get-def
  "Returns value of definition for specified name."
  [name]
  (.get (some (fn [ns-] (ns-resolve ns- (symbol (.substring name 1)))) (all-ns))))


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
   (bench-list-to-file mom (lsm/create-lsm (store/store "data")) 1 bench-list-file))
  ([mom bd]
   (bench-list-to-file mom bd 1 bench-list-file))
  ([mom bd n]
   (bench-list-to-file mom bd n bench-list-file))
  ([mom bd n f]
   (let [bd (if (nil? bd) (lsm/create-lsm (store/store "data")) bd)
         n (if (nil? n) 1 n)
         f (if (nil? f) bench-list-file f)]
     (write-to-file n bd mom f false
                    #(let [ql (get-def %1)
                           [rb0 rb1] (bench-for-list n ql mom %2)
                           avg_time (nth rb1 1)] ; Need for counting time per query.
                       (get-fs %3 rb0 (concat rb1 (list (/ avg_time (count ql))))))))))


(defn bench-list-tpq
  "Takes list with queries (qlist) and parameters of database (mom bd n) 
  and returns vector where each element is vector
  where first element is execution time and second element is query
  (result vector is sorted by time for query)."
  [mom bd n qlist] 
  (let [bd (if (number? bd) (qc/em-memory (bu/gen-bd bd) mom) bd)]
    (sort-by #(% 1) (map (fn [q] [q (first (bench-quering n q mom bd))]) qlist))))


(defn bench-quering-hql
  "Beanchmark HQL quering. 
  Returns sequence: 
    (total time, average time, quntile(5%), quntile(50%), quntile(90%))."
  [n ^String query em]
  (let [f #(.. em (createQuery %1) getResultList)
        times (do-times-q (partial f query) n)
        s-times (apply + times)
        c-times (count times)]
    (concat [s-times (/ s-times c-times)] (quantile times :probs vprobs))))


(defn bench-for-list-hql
  "Takes list with HQL's queries and
  returns time of the querying."
  [n qlist em]
  (reduce #(map + %1 (bench-quering-hql n %2 em)) [0 0 0 0 0] qlist))


(defn- bench-hql
  "Benchmark for queries HQL's queries."
  [bd-n n f list?]
  (let [bd-n (if (nil? bd-n) 1000 bd-n)
        n (if (nil? n) 1 n)
        f (if (nil? f) bench-list-file-hql f)]
    (write-to-file n bd-n nil f true
                   #(let [ql (if list? (get-def %1) [(subs %1 1)])
                          rb1 (bench-for-list-hql n ql %2)
                          avg_time (nth rb1 1)] ; Need for counting time per query.
                      (get-fs %3 0 (concat rb1 (list (/ avg_time (count ql)))))))))


(defn bench-list-to-file-hql
  "Benchmark for queries from Nest project + qlist from test-parsing.clj. Parameters: 
      - bd-n - amount elements of a DB.
      - n - times of execution each list with queries (1 by default).
      - f - name of file for result (etc/yz-bench-list.txt by default).
  Use nil for indication value of parameter as default."
  [bd-n n f]
  (bench-hql bd-n n f true))


(defn bench-to-file-hql
  "Benchmark for individual HQL's queries. 
  See bench-list-to-file-hql doc string for more details about parameters."
  [bd-n n f]
  (bench-hql bd-n n f false))


(defn- create-hm
  "Returns map with hibernate settings: url, dialect, 
  driver and hbm2ddl.auto (empty by default)"
  ([url dialect driver]
   (create-hm url dialect driver ""))
  ([url dialect driver hbm2ddl]
   {"hibernate.connection.url" url
    "hibernate.dialect" dialect
    "hibernate.connection.driver_class" driver
    "hibernate.hbm2ddl.auto" hbm2ddl}))


(defn bench-ind-query
  "Benchmark queries from yz's and hql's individual list queries. 
  Parameters:
      lang defines languages of benchmark (yz or hql).
      q-num defines index of query from vector (use -1 for all queries).
        If parameter query is not empty, then q-num is ignored.
      db-type defines type of database (ram, hdd). It is suppose that
        if type is ram then we must generate database otherwise we only
        should connect to database.
      conn-s - connection string (must contain url (hibernate.connection.url), 
        dialect (hibernate.dialect), driver (hibernate.connection.driver_class) 
        for hql and path to directory with data for yz).
      legend-label - lable for the chart's legend.
      db-n - amount elements of DB.
      f-prefix - defines prefix for file in which result of benchmark is saved.
      measurement - type of measurement (time, thread-time-cpu, thread-time-user, memory).
      idle-count - idle count of calling query before executing measurement.
      cr? - defines whether benchmarking must be used the cr library.
      query - In case query is specified then q-num is ignored.
      lqueries - name of list of queires. If it is specified then q-num and query 
        parameters are ignored.

  Note #1: result of benchmark is saved to the f-prefixnumber_query.txt file.
  Note #2: benchmark is run once."
  [lang q-num db-type conn-s legend-label db-n f-prefix measurement idle-count cr? query lqueries]
  (let [jdbc? (.startsWith conn-s "jdbc")
        ram? (= "ram" db-type)
        yz? (= lang "yz")

        ; Define storage.
        em (if-not jdbc?
             (if ram?
               (if (.startsWith conn-s "lsm:")
                 (qc/create-emlm (bu/gen-bd db-n)) ; generate and save lsm DB, and then use it
                 (bu/gen-bd db-n)) ; generate and use MemoryElementManager (mem) DB.
               (lsm/create-lsm (store/store (subs conn-s 4)))) ; use existing lsm DB.
             (let [[url dialect driver] (cs/split conn-s #"\s")
                   
                   em (create-em "nest-old" 
                                 (create-hm url dialect driver (if ram? "create-drop" "")))
                   ; For RAM database we must create structure of database.
                   _ (if ram? (buj/create-bd db-n em))
                   em (if yz? (-createJPAElementManager em) em)]
               em))
        
        ; Define vector with queries.
        qs (if yz?
             (if jdbc?
               yz/individual-queries-jpa
               yz/individual-queries)
             hql/individual-queries)
        qs (cond (not-empty lqueries) [(eval (symbol lqueries))]
                 (not-empty query) [query]
                 (= q-num -1) qs 
                 :else [(qs q-num)])

        ; Count of execution.
        n 1 
        
        ; Map of model (needed for YZ language).
        mom (let [f (if jdbc? "nest_jpa.mom" "nest.mom")] 
              (mom-from-file f))]
    
    (map-indexed #(let [f (str f-prefix (cond (not-empty lqueries) (name (symbol lqueries))
                                              (not-empty query) "01_others" 
                                              (= q-num -1) %1 
                                              :else q-num) ".txt")]
                    (with-open [wrtr (cio/writer f :append true)]
                      (binding [*measurement* (keyword measurement)
                                *idle-count* idle-count
                                *cr?* cr?]
                        (.write wrtr (get-fs 0 0 (flatten (concat (if yz?
                                                                    (if (vector? %2)
                                                                      (next (bench-for-list n %2 mom em)) ; next excludes result of parsing.
                                                                      (bench-quering n %2 mom em))
                                                                    (if (vector? %2)
                                                                      (bench-for-list-hql n %2 em) 
                                                                      (bench-quering-hql n %2 em)))
                                                                  [db-n legend-label *measurement* query lqueries])) 
                                             false)))))
                 qs)))


(defn generate-bd
  "Takes a number of elements and connection 
  string and generates database. 

  DON'T REMOVE THIS FUNCTION: it is used into the cr_bd.sh script."
  [num conn-s]
  (if (.startsWith conn-s "jdbc")
    (let [[url dialect driver] (cs/split conn-s #"\s")
          m (create-hm url dialect driver "create-drop")]
      (buj/create-bd num (create-em "nest-old" m)))
    (qc/create-emlm (bu/gen-bd num) conn-s))

  (System/exit 0))


(def ind-chars
  "Defines correspondence between human denotes of characteristics from
  files with benchmark of individual queries and
  its number into vector with values of this characteristics.
    0 - number of experiment.
    1 - time of parsing.
    2 - total time of execution.
    3 - average time of execution.
    4 - quantile 5%.
    5 - quantile 50%.
    6 - quantile 90%.
    7 - amount elements from database.
    8 - label for legend of bar chart.
    9 - type of measure."
  {:number 0
   :parsing 1
   :total 2
   :avg 3
   :q5 4
   :q50 5
   :q90 6
   :amount-elems 7
   :legend-label 8
   :measure 9})
