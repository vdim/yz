(ns ru.petrsu.nest.yz.core
  ^{:author Vyacheslav Dimitrov
    :doc "This code contains core functions of the Clojure's implementation of the YZ language.
         The Parsing of queries does due to the fnparse library."}
  (:require [ru.petrsu.nest.yz.hb-utils :as hb]
            [ru.petrsu.nest.yz.parsing :as p]))

(def 
  ^{:doc "The map of the object model."}
  mom (hb/gen-mom-from-cfg "/home/adim/tsen/clj/libs/yz/test/etc/hibernate.cfg.xml"))

(defn p
  []
  (p/parse "building.room" mom))
