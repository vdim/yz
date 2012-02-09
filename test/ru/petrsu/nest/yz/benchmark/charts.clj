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

(ns ru.petrsu.nest.yz.benchmark.charts
  ^{:author "Vyacheslav Dimitrov"
    :doc "Creates charts for results of benchmarks."}
  (:use incanter.stats
        incanter.charts)
  (:require [clojure.java.io :as cio]
            [ru.petrsu.nest.yz.benchmark.benchmark :as bb]
            [incanter.core :as ic]))


(defn- get-res-from-file
  "Takes file with result of benchmark and collection with
  number of experiments (empty or is not supplied for all)."
  ([f]
   (get-res-from-file f [] nil))
  ([f, num-exps]
   (get-res-from-file f num-exps nil))
  ([f, num-exps, q-or-list]
   (let [ne (set num-exps)
         cr (atom q-or-list)]
     (reduce #(cond (.startsWith %2 ";") 
                    (if (or (nil? q-or-list) (= (subs %2 1) @cr))
                      (do (reset! cr (subs %2 1)) (assoc %1 (subs %2 1) []))
                      %1)
                    (.startsWith %2 "#") %1
                    :else
                    (let [s (read-string (str "[" %2 "]"))]
                      (if (and (not (nil? @cr)) (or (empty? ne) (contains? ne (first s))) (not-empty s) (every? number? s))
                        (assoc %1 @cr  (conj (get %1 @cr) s))
                        %1)))
             {} (line-seq (cio/reader f))))))


(def ^:private characteristics
  "Defines correspondence between human denotes of characteristics and
  its number into vector with values of this characteristics.
  0 is number of experiment."
  {:parsing 1
   :total 2
   :avg 3
   :q5 4
   :q50 5
   :q90 6
   :per-query 7})


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
    - :q5 - quntile 5%
    - :q50 - quntile 50%
    - :q90 - quntile 90%.
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
         lines (for [i (range 0 (count lines)) 
                     l lines 
                     :when (= l (nth lines i))] 
                 [(reverse l) (str (* i per) "-" (* (inc i) per))])]
     (ic/view (get-chart lines cats)))))
