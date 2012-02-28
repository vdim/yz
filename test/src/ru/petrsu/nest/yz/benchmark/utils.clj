;;
;; Copyright 2012 Vyacheslav Dimitrov <vyacheslav.dimitrov@gmail.com>
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


(in-ns 'ru.petrsu.nest.yz.benchmark.benchmark)

(defn avg
  "Takes name of a file (f-in) with some benchmark's 
  results, counts average result and writes this result to
  specified file (f-out)."
  [f-in, f-out]
  (let [n 50 ; amount of executing list of queries.
        nbd 7 ; amount of databases (1000, 5000, 10000, 15000, 20000, 50000, 100000)
        cur-list (atom "")
        res 
        (reduce #(cond (.startsWith %2 ";") 
                       (do (reset! cur-list %2) %1)
                       (.startsWith %2 "#") %1
                       :else
                       (let [s (read-string (str "[" %2 "]"))]
                         (if (and (not-empty s) (every? number? s) (= (count s) 8))
                           (assoc %1 @cur-list (conj (vec (get %1 @cur-list)) (s 2)))
                           %1)))
                {} 
                (line-seq (cio/reader f-in)))
        times (reduce (fn [m [k v]] (assoc m k (partition n (apply interleave (partition nbd v))))) {} res)
        res (reduce (fn [m [k v]] 
                      (assoc m k (map #(let [s-times (apply + %)]
                                         (concat [s-times (/ s-times n)] 
                                                 (quantile % :probs vprobs))) v))) 
                    {} times)
        res (reduce (fn [s [k v]] 
                      (str s k \newline (reduce str "" (map #(get-fs 1 0 %) v)) \newline)) 
                    "" res)]
    (cio/copy res (cio/file f-out))))


(defn add-time-per-query
  "Takes a file with benchmarks of lists, adds time per query to it 
  (for each benchmark) and saves its file."
  [f]
  (let [cur-list (atom "")
        new-res 
        (reduce 
          #(str %1 (cond (.startsWith %2 ";") 
                         (do (reset! cur-list (get-def %2)) (str %2 \newline))
                         (.startsWith %2 "#") (str %2 \newline)
                         :else
                         (let [s (read-string (str "[" %2 "]"))]
                           (if (and (not-empty s) (every? number? s) (= (count s) 7))
                             (get-fs (s 0) (s 1) (concat (drop 2 s) (list (/ (s 3) (count @cur-list)))))
                             (str %2 \newline)))))
            "" 
            (line-seq (cio/reader f)))] 
    (cio/copy new-res (cio/file f))))


(defn avg-for-ind
  "Evaluates average values for file with result of benchmarks
  for individual queries. Parameters: 
    f-src - file with result of benchmark.
    f-dest - file for average values."
  [f-src f-dest]
  (let [; r is map where key is string from amount 
        ; elements of DB, some label and type of measurement, and
        ; value is set of result of benchmark.
        r (reduce #(let [v (read-string (str "[" %2 "]"))
                         k (str (v (:amount-elems ind-chars)) 
                                " " (v (:legend-label ind-chars))
                                " " (v (:measure ind-chars)))
                         new-value (conj (get %1 k) (v (:total ind-chars)))]
                     (assoc %1 k new-value))
                  {}
                  (line-seq (cio/reader f-src)))
        r (map (fn [[k v]] (get-fs 0 0 (conj (vec (concat [(apply + v) (/ (apply + v) (count v))] 
                                                          (quantile v :probs vprobs))) k) false))
               r)
        ; sorts lines by amount elements in DB.
        r (sort-by #((read-string (str "[" % "]")) (:amount-elems ind-chars)) r)]
    (with-open [wrtr (cio/writer f-dest)]
      (.write wrtr (reduce str r)))))


(defn avg-for-inds
  "Creates files with averages (due to the avg-for-ind function) 
  values of result of benchmark."
  [path-src path-dest]
  (map #(let [f (str "/" % ".txt")
              fs (str path-src f)
              fd (str path-dest f)]
          (try
            (avg-for-ind fs fd)
            (catch java.io.FileNotFoundException e nil))) 
          (range 0 (count yz/individual-queries))))

