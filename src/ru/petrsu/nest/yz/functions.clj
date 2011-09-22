(ns ru.petrsu.nest.yz.functions
  ^{:author Vyacheslav Dimitrov
    :doc "Set of functions which is build-in YZ."}
  (:use clojure.core))


(defn maxl
  "Takes a list with tuples which have one element and its
  value is numeric ant returns max element from this list."
  [tuples]
  (reduce max (flatten tuples)))

(defn minl
  "Takes a list with tuples which have one element and its
  value is numeric and returns min element from this list."
  [tuples]
  (reduce min (flatten tuples)))
