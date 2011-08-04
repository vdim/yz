(ns ru.petrsu.nest.yz.test-map-utils
  ^{:author Vyacheslav Dimitrov
    :doc "Tests for MOM's utils."}
  (:use ru.petrsu.nest.yz.map-utils clojure.contrib.test-is))

(def mom {:a {:sn "sn1", :dp "dp1"}, :b {:sn "sn2" :dp, "dp2"}})

(deftest t-get-class
         ^{:doc "Tests find-in-sn function"}
         (is (= (find-in-sn "sn1" mom) :a))
         (is (= (find-in-sn "sn2" mom) :b))
         (is (= (find-in-sn "sn3" mom) nil)))

