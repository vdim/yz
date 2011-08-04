(ns ru.petrsu.nest.yz.test-core
  ^{:author Vyacheslav Dimitrov
    :doc "Tests for the core of the implementation of YZ."}
  (:use ru.petrsu.nest.yz.core clojure.contrib.test-is))

(deftest t-get-class
         ^{:doc "Tests get-class function"}
         (is (= (get-class "java.lang.String") java.lang.String))
         (is (= (get-class "java.lang.MyString") nil))
         (is (= (get-class "ru.petrsu.nest.son.Room") ru.petrsu.nest.son.Room)))

