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

(ns ru.petrsu.nest.yz.benchmark.charts
  ^{:author "Vyacheslav Dimitrov"
    :doc "Creates charts for results of benchmarks."}
  (:use incanter.stats
        incanter.charts)
  (:require [clojure.java.io :as cio]
            [ru.petrsu.nest.yz.benchmark.benchmark :as bb]
            [ru.petrsu.nest.yz.benchmark.yz :as yz]
            [incanter.core :as ic]))


(defn- get-res-from-file
  "Takes name of a file with result of benchmark and collection with
  number of experiments (empty for all) and returns map where 
  key is value of string such as \";...\" (in fact query or name of list 
  with queries), and value is list with result of benchmark. 
  You can specify query or name of list with queries by suppling q-or-list.
  if q-or-list is not supplied then function returns result of benchmark for 
  all strings after ';' from file."
  ([f]
   (get-res-from-file f [] :all))
  ([f, num-exps]
   (get-res-from-file f num-exps :all))
  ([f, num-exps, q-or-list]
   (let [ne (set num-exps)
         cr (atom q-or-list)]
     (reduce #(cond (.startsWith %2 ";") 
                    (if (or (= :all q-or-list) (= (subs %2 1) @cr))
                      (do (reset! cr (subs %2 1)) (assoc %1 (subs %2 1) []))
                      %1)
                    (.startsWith %2 "#") %1
                    :else
                    (let [s (read-string (str "[" %2 "]"))]
                      (if (and (not (nil? @cr)) (or (empty? ne) (contains? ne (first s))) (not-empty s)) ;(every? number? s))
                        (assoc %1 @cr (conj (get %1 @cr) s))
                        %1)))
             {} (line-seq (cio/reader f))))))


(def ^:private characteristics
  "Defines correspondence between human denotes of characteristics and
  its number into vector with values of this characteristics.
    0 - number of experiment.
    1 - time of parsing.
    2 - total time of execution.
    3 - average time of execution.
    4 - quantile 5%.
    5 - quantile 50%.
    6 - quantile 90%.
    7 - time per query (for list with queries).
    7 - amount elements from database (truly for individual queries).
    8 - label for legend of bar chart (truly for individual queries)."
  {:number 0
   :parsing 1
   :total 2
   :avg 3
   :q5 4
   :q50 5
   :q90 6
   :per-query 7
   :amount-elems 7
   :legend-label 8})


(defn- get-chart
  "Takes set of lines and categories and returns 
  line-chart from incanter library.
  Lines must be vector where first element is list with data 
  (by categories, amount elements of this list must be equal 
  amount of categories), second element is label."
  [lines cats]
  (let [chart (line-chart cats ((nth lines 0) 0) :series-label ((nth lines 0) 1) :legend true) 
        chart (reduce #(add-categories %1 cats (%2 0) :series-label (%2 1)) chart (next lines))]
    chart))
  

(defn simple-line-chart
  "Creates simple line chart for specified file with result of benchmarks,
  collection with numbers of experiments (all by default) and some
  characteristic of benchmark (quntile 50% by default). Characteristic's
  keys are:
    - :parsing - total time of parsing
  Characteristics for quering are below:
    - :total - total time
    - :avg - average time per query (or list with queries)
    - :q5 - quantile 5%
    - :q50 - quantile 50%
    - :q90 - quantile 90%
    - :per-query - time per query (truly for list with queries)."
  ([f]
   (simple-line-chart f [] :q50))
  ([f num-exps]
   (simple-line-chart f num-exps :q50))
  ([f, num-exps ch]
   (let [r (get-res-from-file f num-exps) ; Result of benchmark.
         cats (if (empty? num-exps) (range 1 (inc (bb/get-num-bench f))) num-exps) ; Categories
         lines (map (fn [[k v]] [(reverse (map #(% (ch characteristics)) v)) k]) r)]
       (ic/view (get-chart lines cats)))))


(defn line-chart-by-expers
  "Creates simple line chart for specified period 
  of number experiments and query (or list's name with queries).
  Parameters:
    f - file with results of benchmarks;
    per - period of number experiments;
    q-or-list - query or list's name with queries 
        (more simple q-or-list is string after ';' symbol from file with benchmarks).
    ch - characteristic of benchmark (see doc string for simple-line-chart).
        :q50 by default."
  ([f per q-or-list]
   (line-chart-by-expers f per q-or-list :q50))
  ([f per q-or-list ch]
   (let [r (get (get-res-from-file f [] q-or-list) q-or-list)
         cats (range 0 per) ; list with categories is list from 0 to period.
         lines (partition per (map #(% (ch characteristics)) r))
         lines (map-indexed (fn [i l] [(reverse l) (str (* i per) "-" (* (inc i) per))]) (reverse lines))]
     (ic/view (get-chart lines cats)))))


(defn- get-res-from-ind-file
  "Returns lines from specified file with result
  of benchmark of individual queries which labels
  are in set with labels."
  ([f]
   (get-res-from-ind-file f #{}))
  ([f, labels]
   (reduce #(let [s (read-string (str "[" %2 "]"))]
              (if (or (empty? labels) (contains? labels (name (last s))))
                (conj %1 s)
                %1))
           [] (line-seq (cio/reader f)))))


(defn bar-chart-by-lang
  "Creates bar chart (JFreeChart object) where categories is set of databases 
  (in fact amount elements of databases), values is set of 
  times of execution query (or list with queries),
  and group-by's category is set languages (yz vs hql) times of executions."
  [f ch labels]
  (let [r (get-res-from-ind-file f labels)
        ; Here we use file with result of benchmarks where there are
        ; some addition fields besides the characteristics map: 
        ; 7 is amount elements from db.
        ; 8 is some label (for comparative diagramm). Example:
        ; 1 0.0000 72344.5430 1446.8909 1018.0772 1407.5066 1800.0106 1000 "hql"
        lines (map (fn [l] {:time (l (ch characteristics)) 
                            :db (l (:amount-elems characteristics)) 
                            :lang (l (:legend-label characteristics))}) 
                   r)
        lines (sort #(let [i (compare (:db %1) (:db %2))]
                       (if (= 0 i)
                         (compare (name (:lang %1)) (name (:lang %2)))
                         i)) 
                    lines)]
    (ic/with-data (ic/dataset [:time :db :lang] lines)
                  (bar-chart :db :time :group-by :lang 
                             :legend true :x-label "Amount elements"
                             :y-label "Time (msecs)"))))


(defn gen-bar-charts
  "Generates bar charts from files with benchmarks of 
  individual queries (0.txt, 1.txt ...) and saves it to
  corresponding file (0.png, 1.png ...). Parameters:
    path-i - path to files with benchmarks.
    path-c - path for files with charts (if path-c is 
             not supplied then path-i is used instead of.)."
  ([path-i]
   (gen-bar-charts path-i path-i #{}))
  ([path-i path-c]
   (gen-bar-charts path-i path-c #{}))
  ([path-i path-c labels]
   (map #(let [f (str path-i "/" % ".txt")
               gf (str path-c "/" % ".png")] 
          (ic/save (set-title (bar-chart-by-lang f :q50 labels) 
                               (yz/title-queries %))
                    gf :width 1024 :height 768))
        (range 0 (count yz/individual-queries)))))

