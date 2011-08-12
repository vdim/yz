(ns ru.petrsu.nest.yz.map-utils
  ^{:author Vyacheslav Dimitrov
    :doc "Helper functions for working with map of object model (MOM).
         MOM has simple ini-format and represents by Clojure's map, 
         where key is full name of class (or another block of data, e.g. packages, funcs, scritps) 
         and value is map with values of this class, e,g. short name (\"SN\"), default property
         (\"DP\") and so on."}
  (:require [clojure.set :as cs]))


(defn find-by-sn
  "Finds class by id in short name (\"SN\") fields of 
  classes's definitions in MOM. If search fails then nil is 
  returned, otherwise Class instanse is returned."
  [id, mom]
  (some #(let [[k m] %1] (if (= (:sn m) id) k nil)) mom))

(declare get-paths)

(defn find-path
  "Returns sequence of string which are path
  (from properties) from \"from\" to \"to\"."
  [from to elems f]
  (get-paths from elems f))

(defn get-paths
  "Returns all path for specified element in graph."
  [from elems f]
  (loop [res `((~from)) e (set (remove #(= from %) elems))]
    (let [ee (set (flatten (map #(f (last %)) res)))]
      (if (empty? (filter #(contains? ee %) e))
        res
        (recur
          (reduce cs/union [] 
                  (map (fn [z] (let [[y x] z, a (filter #(contains? e %) y)] 
                                 (if (empty? a) [x] 
                                   (for [a- a] (conj (vec x) a-)))))
                       (map (fn [x] [(f (last x)), x]) res))) 
          (set (remove #(contains? ee %) e)))))))

(find-path 2 7 #{1 2 3 4 5 6 7 8 9} (fn [x] `(~(inc x) ~(+ x 3))))
