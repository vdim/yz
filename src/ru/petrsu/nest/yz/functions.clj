(ns ru.petrsu.nest.yz.functions
  ^{:author Vyacheslav Dimitrov
    :doc "Set of functions which is build-in YZ."}
  (:import (java.lang String)))


(defn maxl
  "Takes a list with tuples which have one element and its
  value is numeric."
  [tuples]
  (reduce max (flatten tuples)))
