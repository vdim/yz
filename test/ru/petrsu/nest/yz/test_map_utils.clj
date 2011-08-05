(ns ru.petrsu.nest.yz.test-map-utils
  ^{:author Vyacheslav Dimitrov
    :doc "Tests for MOM's utils."}
  (:use ru.petrsu.nest.yz.map-utils clojure.contrib.test-is))

(def mom {:a {:sn "sn1", :dp "dp1"}, :b {:sn "sn2" :dp, "dp2"}})

(deftest t-find-in-sn
         ^{:doc "Tests find-in-sn function"}
         (is (= (find-in-sn "sn1" mom) :a))
         (is (= (find-in-sn "sn2" mom) :b))
         (is (= (find-in-sn "sn3" mom) nil)))


(def nest-mom {ru.petrsu.nest.son.Room {:sn "r", :dp "number"}, 
               ru.petrsu.nest.son.Floor {:sn "f" :dp, "number"}})

(deftest t-find-in-sn-nest
         ^{:doc "Tests find-in-sn function for nest MOM."}
         (is (= (find-in-sn "r" nest-mom) ru.petrsu.nest.son.Room))
         (is (= (find-in-sn "f" nest-mom) ru.petrsu.nest.son.Floor))
         (is (= (find-in-sn "s" nest-mom) nil)))

(deftest t-find-in-sn-some-nest
         ^{:doc "Tests find-in-sn function for nest MOM."}
         (is (= (find-in-sn-some "r" nest-mom) ru.petrsu.nest.son.Room))
         (is (= (find-in-sn-some "f" nest-mom) ru.petrsu.nest.son.Floor))
         (is (= (find-in-sn-some "s" nest-mom) nil)))


