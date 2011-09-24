(ns ru.petrsu.nest.yz.queries.preds
  ^{:author Vyacheslav Dimitrov
    :doc "Processes queries within some restrictions."}
  (:use ru.petrsu.nest.yz.core 
        clojure.contrib.test-is)
  (:require [ru.petrsu.nest.yz.queries.core :as tc])
  (:import (ru.petrsu.nest.son SON Building Room Floor)))


;; Define model


(def f1_b1 (doto (Floor. 1) 
             (.addRoom (Room. "101")) 
             (.addRoom (Room. "102"))))

(def f2_b1 (doto (Floor. 2) 
             (.addRoom (Room. "201")) 
             (.addRoom (Room. "202"))))

(def f3_b1 (doto (Floor. 3) 
             (.addRoom (Room. "301")) 
             (.addRoom (Room. "302"))))

(def f4_b1 (doto (Floor. 4) 
             (.addRoom (Room. "101")) 
             (.addRoom (Room. "401")) 
             (.addRoom (Room. "402"))))


(def f1_b2 (doto (Floor. 1) 
             (.addRoom (Room. "101")) 
             (.addRoom (Room. "1001")) 
             (.addRoom (Room. "201")) 
             (.addRoom (Room. "1002"))))


(def b1 (doto (Building.) (.setName "b1") (.setAddress "Street1") 
          (.addFloor f1_b1) (.addFloor f2_b1)
          (.addFloor f3_b1) (.addFloor f4_b1)))
(def b2 (doto (Building.) (.setName "b2") (.setAddress "Street2") (.addFloor f1_b2)))

(def b3 (doto (Building.) (.setName "b3") (.setAddress "Street1")))

(def son (doto (SON.)
           (.addBuilding b1) 
           (.addBuilding b2)
           (.addBuilding b3))) 



;; Define entity manager.

(use-fixtures :once (tc/setup [son]))


;; Define tests

(deftest select-b1
         ^{:doc "Selects all Buildings which have b1 name."}
         (let [q (tc/r-query "building#(name=\"b1\")")]
           (is (= (.getName ((q 0) 0)) "b1"))
           (is (tc/check-query q [[Building []]]))))

(deftest select-b2
         ^{:doc "Selects all Buildings which have b2 name."}
         (let [q (tc/r-query "building#(name=\"b2\")")]
           (is (= (.getName ((q 0) 0)) "b2"))
           (is (tc/check-query q [[Building []]]))))

(deftest select-b1-or-b2
         ^{:doc "Selects all Buildings which have either b1 or b2 name."}
         (is (tc/check-query "building#(name=\"b1\" or name=\"b2\")"
                             [[Building [], Building []]])))

(deftest select-b1-and-b2
         ^{:doc "Selects all Buildings which have b1 and b2 name. 
                Of cource this situation is nonsence, but it is enough
                for the testing 'and' clause."}
         (is (tc/check-query "building#(name=\"b1\" and name=\"b2\")"
                             [[]])))

(deftest select-b1-and-street1
         ^{:doc "Selects all Buildings which have b1 name and Street1 address."}
         (let [q (tc/r-query "building#(name=\"b1\" and address=\"Street1\")")]
           (is (= (.getName ((q 0) 0)) "b1"))
           (is (= (.getAddress ((q 0) 0)) "Street1"))
           (is (tc/check-query q [[Building []]]))))

(deftest select-street1
         ^{:doc "Selects all Buildings which have address Street1."}
         (let [q (tc/r-query "building#(address=\"Street1\")")]
           (is (= (.getAddress ((q 0) 0)) "Street1"))
           (is (= (.getAddress ((q 0) 2)) "Street1"))
           (is (tc/check-query q [[Building [], Building[]]]))))

(deftest select-street1-b1-b2
         ^{:doc "Selects all Buildings which have 
                address Street1 and name either b1 or b2"}
         (let [q (tc/r-query 
                   "building#(address=\"Street1\" and (name=\"b1\" or name=\"b2\"))")]
           (is (= (.getAddress ((q 0) 0)) "Street1"))
           (is (or (= (.getName ((q 0) 0)) "b1") (= (.getName ((q 0) 0)) "b2")))
           (is (tc/check-query q [[Building []]]))))

(deftest select-f1
         ^{:doc "Selects all Floors which have number 1."}
         (let [q (tc/r-query "floor#(number=1)")]
           (is (= (.getNumber ((q 0) 0)) 1))
           (is (empty? ((q 0) 1)))
           (is (= (.getNumber ((q 0) 2)) 1))
           (is (empty? ((q 0) 3)))
           (is (tc/check-query q [[Floor [], Floor []]]))))

(deftest select-f2
         ^{:doc "Selects all Floors which have number 2."}
         (let [q (tc/r-query "floor#(number=2)")]
           (is (= (.getNumber ((q 0) 0)) 2))
           (is (empty? ((q 0) 1)))
           (is (tc/check-query q [[Floor []]]))))

(deftest select-f1-nest-r101
         ^{:doc "Selects all floors which have 1 number 
                and its rooms which have 101 number."}
         (let [q (tc/r-query "floor#(number=1) (room#(number=\"101\"))")]
           (is (= (.getNumber ((q 0) 0)) 1))
           (is (= (.getNumber ((((q 0) 1) 0) 0)) "101"))
           (is (= (.getNumber ((q 0) 2)) 1))
           (is (= (.getNumber ((((q 0) 3) 0) 0)) "101"))
           (is (tc/check-query q [[Floor [[Room []]], Floor [[Room []]]]]))))

(deftest select-r101
         ^{:doc "Selects Rooms which have 101 number."}
         (is (tc/check-query  "room#(number=\"101\")"
                             [[Room [], Room [], Room []]])))

(deftest select-f1-then-r101
         ^{:doc "Selects Rooms which have 101 number and are located on the first floors."}
         (is (tc/check-query "floor#(number=1).room#(number=\"101\")"
                             [[Room [], Room []]])))

(deftest select-f1-r101-b3
         ^{:doc "Selects building which has name b3 and 
                rooms which have 101 number and are located on the first floors"}
         (is (tc/check-query "floor#(number=1).room#(number=\"101\").building#(name=\"b3\")"
                             [[]])))

(deftest select-f11-r101-b3
         ^{:doc "Selects building which has name b3 and 
                rooms which have 101 number and are located on the eleventh floors"}
         (is (tc/check-query "floor#(number=11).room#(number=\"101\").building#(name=\"b3\")"
                             [[]])))

(deftest select-f1-r101-then-b1
         ^{:doc "Selects rooms (with number 101) on the first floors and its
                buildings which have name b1."}
         (is (let [q (tc/r-query "floor#(number=1).room#(number=\"101\") (building#(name=\"b1\"))")]
               (or (tc/check-query q [[Room [[]], Room [[Building []]]]])
                   (tc/check-query q [[Room [[Building []]], Room [[]]]])))))


(deftest select-f1-r101-then-b2
         ^{:doc "Selects rooms (with number 101) on the first floors and its
                buildings which have name b2."}
         (is (let [q (tc/r-query "floor#(number=1).room#(number=\"101\") (building#(name=\"b2\"))")]
               (or (tc/check-query q [[Room [[]], Room [[Building []]]]])
                   (tc/check-query q [[Room [[Building []]], Room [[]]]])))))

(deftest select-f1-r101-then-b3
         ^{:doc "Selects rooms (with number 101) on the first floors and then
                buildings which have name b3."}
         (is (tc/check-query "floor#(number=1).room#(number=\"101\") (building#(name=\"b3\"))"
                             [[Room [[]], Room [[]]]])))

(deftest select-b3-and-flnum1
         ^{:doc ""}
         (is (tc/check-query "building#(name=\"b3\" and floor.number=1)"
                             [[]])))

(deftest select-b1-and-flnum4
         ^{:doc ""}
         (is (tc/check-query "building#(name=\"b1\" and floor.number=4)"
                             [[Building []]])))

(deftest select-b1-or-b2-and-flnum4
         ^{:doc ""}
         (is (tc/check-query "building#((name=\"b1\" or name=\"b2\") and floor.number=4)"
                             [[Building []]])))

(deftest select-b1-or-b2-and-flnum1
         ^{:doc ""}
         (is (tc/check-query "building#((name=\"b1\" or name=\"b2\") and floor.number=1)"
                             [[Building [], Building []]])))

(deftest select-b3-or-flnum1
         ^{:doc ""}
         (is (tc/check-query "building#(name=\"b3\" or floor.number=1)"
                             [[Building [], Building [], Building []]])))

(deftest select-flnumgt0
         ^{:doc ""}
         (is (tc/check-query "building#(floor.number>=0)"
                             [[Building [], Building []]])))

(deftest select-flnumgt3
         ^{:doc ""}
         (is (tc/check-query "building#(floor.number>=3)"
                             [[Building []]])))

(deftest select-b-flnum1-nest-r
         ^{:doc ""}
         (is (tc/check-query 
               "building#(floor.number=1) (room#(number=\"201\"))"
               [[Building [[Room []]], Building [[Room []]]]])))

(deftest select-b-nest-fngt5-or-fnlt1
         ^{:doc ""}
         (is (tc/check-query 
               "building (floor#(number>5 or number<1))"
               [[Building [[]] Building [[]] Building [[]]]])))



;; Cherks sign

(deftest select-b-fnumgt1
         ^{:doc ""}
         (is (tc/check-query "building#(floor.number>1)" [[Building []]])))

(deftest select-fnumgt4
         ^{:doc ""}
         (is (tc/check-query "floor#(number>4)" [[]])))

(deftest select-fnumgt-or-eq4
         ^{:doc ""}
         (is (tc/check-query "floor#(number>=4)" [[Floor []]])))

(deftest select-fnumlt1
         ^{:doc ""}
         (is (tc/check-query "floor#(number<1)" [[]])))

(deftest select-fnumlt-or-eq1
         ^{:doc ""}
         (is (tc/check-query "floor#(number<=1)" [[Floor [], Floor []]])))


(deftest select-fnumgt4-ws
         ^{:doc ""}
         (is (tc/check-query "floor#(number  >  4)" [[]])))

(deftest select-fnumgt-or-eq4-ws
         ^{:doc ""}
         (is (tc/check-query "floor#(number    >= 4)" [[Floor []]])))

(deftest select-fnumlt1-ws
         ^{:doc ""}
         (is (tc/check-query "floor#(number <1)" [[]])))

(deftest select-fnumlt-or-eq1-ws
         ^{:doc ""}
         (is (tc/check-query "floor#(number<= 1)" [[Floor [], Floor []]])))



;; Checks reduced restrictions.

(deftest select-reduced-preds
         ^{:doc ""}
         (is (tc/check-query "floor#(number=(1 or 2))" [[Floor [], Floor [], Floor []]]))
         (is (tc/check-query "floor#(number=(1 or 2 or 3))" [[Floor [], Floor [], Floor [], Floor []]]))
         (is (tc/check-query "floor#(number=(1 or >4))" [[Floor [], Floor []]]))
         (is (tc/check-query "floor#(number=(1 or =5))" [[Floor [], Floor []]]))
         (is (tc/check-query "floor#(number=(<1 or >4))" [[]]))
         (is (tc/check-query "floor#(number=(1 and <2))" [[Floor [], Floor []]]))
         (is (tc/check-query "floor#(number=(1 and 4))" [[]]))
         (is (tc/check-query "floor#(number=(1 and =4))" [[]]))
         (is (tc/check-query "floor#(number=(=1 and 4))" [[]]))
         (is (tc/check-query "floor#(number=(=1 and =4))" [[]]))
         (is (tc/check-query "floor#(number=(1 or (>3 and <5)))" [[Floor [], Floor [], Floor []]]))
         (is (tc/check-query "floor#(number=(1 or (>3 and <5)))" [[Floor [], Floor [], Floor []]]))
         (is (tc/check-query "floor#(number=(1 or (>3 and <5)) or building.name=\"b2\")"
                             [[Floor [], Floor [], Floor []]]))
         (is (tc/check-query "floor#(number=(<1 or (>3 and <5)) and building.name=\"b3\")"
                             [[]])))

