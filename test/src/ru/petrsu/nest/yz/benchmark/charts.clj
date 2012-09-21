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
        incanter.charts 
        incanter.pdf
        ru.petrsu.nest.yz.queries.nest-queries)
  (:require [clojure.java.io :as cio] 
            [clojure.string :as cs]
            [ru.petrsu.nest.yz.benchmark.benchmark :as bb]
            [ru.petrsu.nest.yz.benchmark.yz :as yz]
            [ru.petrsu.nest.yz.benchmark.hql :as hql]
            [incanter.core :as ic]))


(def title-queries-ru
  "Russian descriptions of individual-queries  
  (are needed for diagram's titles)."
  ["Простая выборка" ; simple selection
   "Простая выборка с простым условием" ; simple selection with simple filtering
   "Простая выборка со сложным условием" ; simple selection with compose filtering
   "Запрос на соединение: device (building)" ; query with join
   "Запрос на соединение: building (device)" ; query with join
   "Запрос на соединение: li (n (d))" ; query with join
   "Сортировка" ; query with ordering
   "Сценарий: enlivener-queries" ; enlivener-queries scenario
   "Сценарий: address-info-queries" ; address-info-queries scenario
   "Сценарий: tree-queries" ; tree-queries scenario
   ""
   ""
   ""
   ""
   ""
   ""
   ])


(def title-queries-en
  "English descriptions of individual-queries  
  (are needed for diagram's titles)."
  ["Simple selection" ; simple selection
   "Simple selection with simple condition" ; simple selection with simple filtering
   "simple selection with compose condition" ; simple selection with compose filtering
   "Query with join: device (building)" ; query with join
   "Query with join: building (device)" ; query with join
   "Query with join: li (n (d))" ; query with join
   "Ordering" ; query with ordering
   "Scenario: enlivener-queries" ; enlivener-queries scenario
   "Scenario: address-info-queries" ; address-info-queries scenario
   "Scenario: tree-queries" ; tree-queries scenario
   ""
   ""
   ""
   ""
   ""
   ""
   ])


(def title-queries
  "Titles as queries."
  ["device" ; simple selection
   "device#(name=\"Device_MB\")" ; simple selection with simple filtering
   "device#(name=\"Device_MB\" && ..." ; simple selection with compose filtering
   "device (building)" ; query with join
   "building (device)" ; query with join
   "li (n (d))" ; query with join
   "{↑description}device" ; query with ordering
   "enlivener-queries" ; enlivener-queries scenario
   "address-info-queries" ; address-info-queries scenario
   "tree-queries" ; tree-queries scenario
   ""
   ""
   ""
   ""
   ""
   ""
   ])


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
    7 - time per query (for list with queries)."
  {:number 0
   :parsing 1
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
  (amount elements of this list must be equal 
  amount of categories), second element is label."
  [lines cats]
  (let [chart (line-chart cats ((nth lines 0) 0) :series-label ((nth lines 0) 1) :legend true) 
        chart (reduce #(add-categories %1 cats (%2 0) :series-label (%2 1)) chart (next lines))]
    chart))
  

(defn simple-line-chart
  "Creates simple line chart for specified file with result of benchmarks,
  collection with numbers of experiments (all by default) and some
  characteristic of benchmark (quantile 50% by default). Characteristic 
  must be some key from the characteristics map."
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
    ch - characteristic of benchmark (see doc string for the 
        definition of the characteristic map). :q50 by default."
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
              (if (or (empty? labels) (contains? labels (name (s (:legend-label bb/ind-chars)))))
                (conj %1 s)
                %1))
           [] (remove empty? (line-seq (cio/reader f))))))


(defn bar-chart-by-label
  "Creates bar chart (JFreeChart object) where categories is set of databases 
  (in fact amount elements of databases), values is set of 
  some characteristic of query (or list with queries),
  and group-by's category is set of labels (use empty set for all labels).
    f - name of a file with result of benchmark (suppose that it is file with
        benchmark of individual queries).
    ch - characteristic of execution a query (see definition of the characteristic map).
    labels - set of labels which are used for group-by's category of chart.
    [x y title] - vector with x, y labels and title of chart 
                  ([nil nil nil] by default)."
  ([f ch]
   (bar-chart-by-label f ch #{} [nil nil nil]))
  ([f ch labels]
   (bar-chart-by-label f ch labels [nil nil nil]))
  ([f ch labels [x y title]]
   (let [r (remove empty? (get-res-from-ind-file f labels))
         lines (map (fn [l] {:time (l (ch bb/ind-chars)) 
                             :db (l (:amount-elems bb/ind-chars)) 
                             :lang (l (:legend-label bb/ind-chars))})
                    r)
         lines (sort #(let [i (compare (:db %1) (:db %2))]
                        (if (= 0 i)
                          (compare (:time %1) (:time %2))
                          i)) 
                     lines)
         ; Amount of series (in fact amount lines into chart)
         series (count (set (map :lang lines)))]
     (ic/with-data (ic/dataset [:time :db :lang] lines)
                   (reduce 
                     #(set-stroke %1 :series %2 :width 5)
                     (line-chart :db :time :group-by :lang 
                                 :legend true :x-label x
                                 :y-label y :title title)
                     (range 0 series))))))


(defn gen-bar-charts
  "Generates bar charts from files with benchmarks of 
  individual queries (0.txt, 1.txt ...) and saves it to
  corresponding file (0.png, 1.png ...). Options:
    :path-i - path to files with benchmarks.
    :path-c - path for files with charts (if path-c is 
              not supplied then path-i is used instead of).
    :labels - set of labels (group-by's category).
    :mode - type of language. :ru (russia title), :en (english title) 
            and :qs (title is query) are supported now.
    :prefix - prefix for files with benchmark (empty by default).
    :ftype - type of file with chart (support :pdf and :png)
            (png by default)."
  ([path-i & options]
   (let [{:keys [path-c labels mode prefix ftype]} options
         path-c (or path-c path-i)
         ; png by defaul
         ftype (or ftype :png)
         ; Function for saving chart
         fsave (case ftype 
                 :png ic/save
                 :pdf save-pdf
                 (throw (Exception. (str "Unknown type of saving file: " (name ftype)))))

         [x y titles] (case mode 
                        :ru ["Количество элементов" "Время (мс)" title-queries-ru]
                        :en ["Amount Elements" "Time (msecs)" title-queries-en]
                        :qs ["Amount Elements" "Time (msecs)" title-queries]
                        (throw (Exception. (str "Unknown language: " (name mode)))))
         ; Returns bold arial font for specified size.
         font #(java.awt.Font. "Arial" java.awt.Font/BOLD %)
         ; big font for labels 
         l-font (font 34)
         ; middle font for tick label
         a-font (font 22)]
     (map #(let [f (str path-i "/" prefix % ".txt")
                 gf (str path-c "/" prefix % (str "." (name ftype)))]
             (try
               (let [chart (bar-chart-by-label f :q50 labels [x y (titles %)])
                     _ (.. chart getLegend (setItemFont l-font))
                     _ (.. chart getTitle (setFont l-font))
                     plot (.. chart getCategoryPlot)
                     _ (.. plot getDomainAxis (setTickLabelFont a-font))
                     _ (.. plot getDomainAxis (setLabelFont l-font))
                     _ (.. plot getRangeAxis (setTickLabelFont a-font))
                     _ (.. plot getRangeAxis (setLabelFont l-font))]
               
                 (fsave chart gf :width 1024 :height 768))
               (catch java.io.FileNotFoundException e nil)))
          (range 0 (count yz/individual-queries))))))


(defn- localize
  "Localize titles of diagram."
  [mode ru en]
  (case mode 
    :ru ru 
    :en en 
    (throw (Exception. (str "Unknown language: " (name mode))))))


(defn chart-by-vquery
  "Creates bar chart for capacity of querie's text.
  Parameters:
    f - name of file where bar chart will be saved.
    mode - language of titles (:ru and :en are supported now, :ru by default)."
  ([f]
   (chart-by-vquery f :ru))
  ([f mode]
   (let [[y, x, title] (localize 
                         mode 
                         ["Количество символов" "Списки" "Объем текста запросов"] 
                         ["Count of characters" "Lists" "Volume of query's text"])
         m (fn [l s v] {:lang l :scenario s :volume (reduce + (map count v))})
         data [(m "yz" "address-info" address-info-queries-jpa) 
               (m "hql" "address-info" address-info-queries-hql) 
               (m "yz" "enlivener" enlivener-queries-jpa) 
               (m "hql" "enlivener" enlivener-queries-hql)
               (m "yz" "treenest" tree-queries-jpa) 
               (m "hql" "treenest" tree-queries-hql)
               (m "yz" "individual" (filter string? yz/individual-queries)) 
               (m "hql" "individual" (filter string? hql/individual-queries))]]
     (ic/save (ic/with-data 
                (ic/dataset [:lang :scenario :volume] data) 
                (bar-chart :scenario :volume 
                           :group-by :lang
                           :legend true 
                           :x-label x :y-label y :title title)) 
              f :width 1024 :height 768))))


(def c-labels
  "Vector with sets of labels which are compared."
  [#{"yz-hdd-derby" "hql-hdd-derby" "yz-hdd-lsm"} 
   #{"yz-hdd-hsqldb" "hql-hdd-hsqldb" "yz-hdd-lsm"}
   #{"yz-hdd-h2" "hql-hdd-h2" "yz-hdd-lsm"}
   #{"yz-ram-derby" "hql-ram-derby" "yz-ram-mem"}
   #{"yz-ram-hsqldb" "hql-ram-hsqldb" "yz-ram-mem"}
   #{"yz-ram-h2" "hql-ram-h2" "yz-ram-mem"}])


(defn chart-by-memory
  "Creates JFreeChart object which is represented
  result of benchmark memory usage. Axis of X is count of elements in DB,
  Axis of Y is amount of memory (MBytes), grouping is done by label 
  (label is (str \"lang\"-\"db\"-\"db-type\")).
  Parameters:
    file-or-files - name of file with result of benchmark or vector with
                    name of files with result of benchmark.
    i - number of query.
    patterns - list of patterns which is used for filtering labels (empty by default).
    mode - language of diagram's titles (:ru or :en (:en by default))."
  ([file-or-files i]
   (chart-by-memory file-or-files i () :en))
  ([file-or-files i patterns]
   (chart-by-memory file-or-files i patterns :en))
  ([file-or-files i patterns mode]
   (let [fs file-or-files
         get-lines #(cs/split-lines (slurp %))
         lines (if (sequential? fs) 
                 (flatten (map #(get-lines %) fs)) 
                 (get-lines fs))
         data (reduce #(if (empty? %2)
                         %1
                         (let [words (cs/split %2 #"\s") 
                               [lang db q db-type n-db size] words
                               ; Size is measured in Bytes, so we get MBytes.
                               size (double (/ (read-string size) 1024 1024))]
                           (if (= (read-string q) i)
                             (conj %1 {:mem size :n-db n-db :label (str lang "-" db "-" db-type)})
                             %1)))
                      [] lines)
         data (if (empty? patterns)
                data
                (filter #(some (fn [reg] (re-find (re-pattern reg) (:label %))) patterns) data))
         [x y title] (localize 
                       mode 
                       ["Количество элементов" "Память (МБ)" "Потребление памяти"] 
                       ["Count elements" "Memory (M)" "Heap Memory Usage"])]
     (ic/with-data (ic/dataset [:mem :n-db :label] data)
                   (bar-chart :n-db :mem :group-by :label
                              :legend true :x-label x
                              :y-label y :title title)))))


(defn save-charts-by-memory
  "Saves memory charts for all queries (0-6)
  in current directory with names 0.png 1.png 
  and so on. Parameters:
    fs - list of files.
    patterns - list of patterns for label."
  [fs, patterns]
  (map #(ic/save (doto (chart-by-memory fs % patterns :ru) 
                   (.setTitle (title-queries-ru %))) 
                 (str % ".png"))
       (range 0 7)))

