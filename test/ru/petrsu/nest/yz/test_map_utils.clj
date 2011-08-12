(ns ru.petrsu.nest.yz.test-map-utils
  ^{:author Vyacheslav Dimitrov
    :doc "Tests for MOM's utils."}
  (:use ru.petrsu.nest.yz.map-utils clojure.contrib.test-is))

(def mom {:a {:sn "sn1", :dp "dp1"}, :b {:sn "sn2" :dp, "dp2"}})

(deftest t-find-in-sn
         ^{:doc "Tests find-by-sn function"}
         (is (= (find-by-sn "sn1" mom) :a))
         (is (= (find-by-sn "sn2" mom) :b))
         (is (= (find-by-sn "sn3" mom) nil)))


(def nest-mom {ru.petrsu.nest.son.Room {:sn "r", :dp "number"}, 
               ru.petrsu.nest.son.Floor {:sn "f" :dp, "number"}})

(deftest t-find-by-sn-nest
         ^{:doc "Tests find-by-sn function for nest MOM."}
         (is (= (find-by-sn "r" nest-mom) ru.petrsu.nest.son.Room))
         (is (= (find-by-sn "f" nest-mom) ru.petrsu.nest.son.Floor))
         (is (= (find-by-sn "s" nest-mom) nil)))

(deftest t-find-path
         ^{:doc "Tests find-path function."}
         (is (= (find-path 2 7 #{1 2 3 4 5 6 7 8 9} (fn [x] `(~(inc x) ~(+ x 3))))
                '([2 3 6 9] [2 3 6 7] [2 3 4 7] [2 5 8 9] [2 5 6 7] [2 5 6 9])))
         (is (= (find-path 2 7 #{1 2 3 4 5 6 7 8 9} (fn [x] `(~(inc x) ~(dec x))))
                '([2 1] [2 3 4 5 6 7 8 9]))))
