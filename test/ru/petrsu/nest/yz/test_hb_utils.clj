(ns ru.petrsu.nest.yz.test-hb-utils
  ^{:author "Vyacheslav Dimitrov"
    :doc "Tests for functions which generate MOM."}
  (:use ru.petrsu.nest.yz.hb-utils clojure.contrib.test-is)
  (:import (ru.petrsu.nest.son Floor Room Building)))

(def classes #{Floor, Room, Building})

(deftest t-get-paths
         ^{:doc "Tests get-paths function."}
          (is (= (first (get-paths Room Building classes))
                 {:path [ru.petrsu.nest.son.Room ru.petrsu.nest.son.Floor ru.petrsu.nest.son.Building], 
                  :ppath ["floor" "building"]}))
          (is (= (first (get-paths Floor Building classes))
                 {:path [ru.petrsu.nest.son.Floor ru.petrsu.nest.son.Building], 
                  :ppath ["building"]}))
          (is (= (first (get-paths Building Floor classes))
                 {:path [ru.petrsu.nest.son.Building ru.petrsu.nest.son.Floor], 
                  :ppath ["floors"]})))

(deftest t-get-short-name
         ^{:doc "Tests 'get-short-name' function."}
         (is (= "f" (get-short-name Floor)))
         (is (= "b" (get-short-name Building)))
         (is (= "ni" (get-short-name ru.petrsu.nest.son.NetworkInterface)))
         (is (= "ip4i" (get-short-name ru.petrsu.nest.son.IPv4Interface))))

