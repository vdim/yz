(ns ru.petrsu.nest.yz.test-parsing
  ^{:author Vyacheslav Dimitrov
    :doc "Tests for parsing functions."}
  (:use ru.petrsu.nest.yz.parsing 
        ru.petrsu.nest.yz.hb-utils 
        clojure.contrib.test-is)
  (:import (ru.petrsu.nest.son Building)))

(def mom (gen-mom-from-cfg "/home/adim/tsen/clj/libs/yz/test/etc/hibernate.cfg.xml"))

(deftest t-find-class
         ^{:doc "Tests 'find-class' function."}
         (is (= (find-class "ru.petrsu.nest.son.Building", mom) Building))
         (is (= (find-class "ru.petrsu.nest.son.building", mom) Building))
         (is (= (find-class "building", mom) Building))
         (is (= (find-class "b", mom) Building))
         (is (= (find-class "build", mom) Building)))

(deftest t-parse
         ^{:doc "Tests 'parse' function."}
          (is (= (parse "building", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props nil
                   :pred nil
                   :then nil
                   :next nil}])))
