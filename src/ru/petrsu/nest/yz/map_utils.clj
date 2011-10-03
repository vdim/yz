(ns ru.petrsu.nest.yz.map-utils
  ^{:author "Vyacheslav Dimitrov"
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


(defmacro assoc-in* 
  "Like clojure.core/assoc-in but vector of keys has specified structure: 
  key* repeats n times and then key-in is."
  [m key* n key-in v] 
  `(assoc-in ~m (vec (flatten [(repeat ~n ~key*) ~key-in])) ~v))


(defn insert-in
  "Like assoc-in*, but defines whether value of key* is nil, if
  it is then assoc-in* is called, otherwise original m is returned."
  [m n key* v]
  (loop [m- m n- n]
    (if (<= n- 0)
      (if (nil? (key* m-))
        (assoc-in* m key* n key* v)
        m)
      (recur (key* m-) (dec n-)))))

