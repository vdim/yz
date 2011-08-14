(ns ru.petrsu.nest.yz.core
  ^{:author Vyacheslav Dimitrov
    :doc "This code contains core functions of the Clojure's implementation of the YZ language.
         The Parsing of queries does due to the fnparse library."}
  (:require [ru.petrsu.nest.yz.hb-utils :as hb]))

(def 
  ^{:doc "The map of the object model."}
  mom (hb/gen-mom-from-cfg "/home/adim/tsen/clj/libs/yz/test/etc/hibernate.cfg.xml"))

(defn get-class
  "Gets name of class, finds this class and returns instance of java.lang.Class.
  If search of class fails then nil is returned."
  [id]
  (try 
    (Class/forName id) 
    (catch ClassNotFoundException _ nil)))

