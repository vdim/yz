;;
;; Copyright 2011-2012 Vyacheslav Dimitrov <vyacheslav.dimitrov@gmail.com>
;;
;; This file is part of YZ.
;;
;; YZ is free software: you can redistribute it and/or modify it
;; under the terms of the GNU Lesser General Public License version 3
;; only, as published by the Free Software Foundation.
;;
;; YZ is distributed in the hope that it will be useful, but
;; WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
;; Lesser General Public License for more details.
;;
;; You should have received a copy of the GNU Lesser General Public
;; License along with YZ.  If not, see
;; <http://www.gnu.org/licenses/>.
;;

(ns ru.petrsu.nest.yz.queries.preds
  ^{:author "Vyacheslav Dimitrov"
    :doc "Processes queries within some restrictions."}
  (:use ru.petrsu.nest.yz.core 
        clojure.test)
  (:require [ru.petrsu.nest.yz.queries.core :as tc] 
            [ru.petrsu.nest.yz.yz-factory :as yzf] 
            [ru.petrsu.nest.yz.core :as c])
  (:import (ru.petrsu.nest.son SON Building Room Floor)))


;; Define model


(def f1_b1 (doto (Floor.) 
             (.setNumber (Integer. 1))
             (.addRoom (doto (Room.) (.setNumber "101"))) 
             (.addRoom (doto (Room.) (.setNumber "102")))))

(def f2_b1 (doto (Floor.) 
             (.setNumber (Integer. 2))
             (.addRoom (doto (Room.) (.setNumber "201"))) 
             (.addRoom (doto (Room.) (.setNumber "202")))))

(def f3_b1 (doto (Floor.) 
             (.setNumber (Integer. 3))
           (.addRoom (doto (Room.) (.setNumber "301"))) 
             (.addRoom (doto (Room.) (.setNumber "302")))))

(def f4_b1 (doto (Floor.) 
             (.setNumber (Integer. 4))
             (.addRoom (doto (Room.) (.setNumber "101"))) 
             (.addRoom (doto (Room.) (.setNumber "401"))) 
             (.addRoom (doto (Room.) (.setNumber "402")))))


(def f1_b2 (doto (Floor.) 
             (.setNumber (Integer. 1))
             (.addRoom (doto (Room.) (.setNumber "101"))) 
             (.addRoom (doto (Room.) (.setNumber "1001"))) 
             (.addRoom (doto (Room.) (.setNumber "1002"))) 
             (.addRoom (doto (Room.) (.setNumber "201")))))

(def b1 (doto (Building.) (.setName "b1") 
          (.setAddress "Street1") 
          (.setDescription "Some desc")
          (.addFloor f1_b1) (.addFloor f2_b1)
          (.addFloor f3_b1) (.addFloor f4_b1)))
(def b2 (doto (Building.) (.setName "b2") (.setAddress "Street2") (.addFloor f1_b2)))

(def b3 (doto (Building.) (.setName "b3") (.setAddress "Street1")))

(def son (doto (SON.)
           (.addBuilding b1) 
           (.addBuilding b2)
           (.addBuilding b3))) 



;; Define entity manager.

(use-fixtures :once (tc/setup-son son))


;; Define tests

(deftest select-b1
         ^{:doc "Selects all Buildings which have b1 name."}
         (let [q (tc/r-query "building#(name=\"b1\")")]
           (is (= (.getName (q 0)) "b1"))
           (is (tc/check-query q [Building []]))))

(deftest select-b2
         ^{:doc "Selects all Buildings which have b2 name."}
         (let [q (tc/r-query "building#(name=\"b2\")")]
           (is (= (.getName (q 0)) "b2"))
           (is (tc/check-query q [Building []]))))

(deftest select-b1-or-b2
         ^{:doc "Selects all Buildings which have either b1 or b2 name."}
         (is (tc/check-query "building#(name=\"b1\" or name=\"b2\")"
                             [Building [], Building []])))

(deftest select-b1-and-b2
         ^{:doc "Selects all Buildings which have b1 and b2 name. 
                Of cource this situation is nonsence, but it is enough
                for the testing 'and' clause."}
         (is (tc/check-query "building#(name=\"b1\" and name=\"b2\")"
                             [])))

(deftest select-b1-and-street1
         ^{:doc "Selects all Buildings which have b1 name and Street1 address."}
         (let [q (tc/r-query "building#(name=\"b1\" and address=\"Street1\")")]
           (is (= (.getName (q 0)) "b1"))
           (is (= (.getAddress (q 0)) "Street1"))
           (is (tc/check-query q [Building []]))))

(deftest select-street1
         ^{:doc "Selects all Buildings which have address Street1."}
         (let [q (tc/r-query "building#(address=\"Street1\")")]
           (is (= (.getAddress (q 0)) "Street1"))
           (is (= (.getAddress (q 2)) "Street1"))
           (is (tc/check-query q [Building [], Building[]]))))

(deftest select-street1-b1-b2
         ^{:doc "Selects all Buildings which have 
                address Street1 and name either b1 or b2"}
         (let [q (tc/r-query 
                   "building#(address=\"Street1\" and (name=\"b1\" or name=\"b2\"))")]
           (is (= (.getAddress (q 0)) "Street1"))
           (is (or (= (.getName (q 0)) "b1") (= (.getName (q 0)) "b2")))
           (is (tc/check-query q [Building []]))))

(deftest select-f1
         ^{:doc "Selects all Floors which have number 1."}
         (let [q (tc/r-query "floor#(number=1)")]
           (is (= (.getNumber (q 0)) 1))
           (is (empty? (q 1)))
           (is (= (.getNumber (q 2)) 1))
           (is (empty? (q 3)))
           (is (tc/check-query q [Floor [], Floor []]))))

(deftest select-f2
         ^{:doc "Selects all Floors which have number 2."}
         (let [q (tc/r-query "floor#(number=2)")]
           (is (= (.getNumber (q 0)) 2))
           (is (empty? (q 1)))
           (is (tc/check-query q [Floor []]))))

(deftest select-f1-nest-r101
         ^{:doc "Selects all floors which have 1 number 
                and its rooms which have 101 number."}
         (let [q (tc/r-query "floor#(number=1) (room#(number=\"101\"))")]
           (is (= (.getNumber (q 0)) 1))
           (is (= (.getNumber ((q 1) 0)) "101"))
           (is (= (.getNumber (q 2)) 1))
           (is (= (.getNumber ((q 3) 0)) "101"))
           (is (tc/check-query q [Floor [Room []], Floor [Room []]]))))

(deftest select-r101
         ^{:doc "Selects Rooms which have 101 number."}
         (is (tc/check-query  "room#(number=\"101\")"
                             [Room [], Room [], Room []])))

(deftest select-f1-then-r101
         ^{:doc "Selects Rooms which have 101 number and are located on the first floors."}
         (is (tc/check-query "floor#(number=1).room#(number=\"101\")"
                             [Room [], Room []])))

(deftest select-f1-r101-b3
         ^{:doc "Selects building which has name b3 and 
                rooms which have 101 number and are located on the first floors"}
         (is (tc/check-query "floor#(number=1).room#(number=\"101\").building#(name=\"b3\")"
                             [])))

(deftest select-f11-r101-b3
         ^{:doc "Selects building which has name b3 and 
                rooms which have 101 number and are located on the eleventh floors"}
         (is (tc/check-query "floor#(number=11).room#(number=\"101\").building#(name=\"b3\")"
                             [])))

(deftest select-f1-r101-then-b1
         ^{:doc "Selects rooms (with number 101) on the first floors and its
                buildings which have name b1."}
         (is (let [q (tc/r-query "floor#(number=1).room#(number=\"101\") (building#(name=\"b1\"))")]
               (or (tc/check-query q [Room [], Room [Building []]])
                   (tc/check-query q [Room [Building []], Room []])))))


(deftest select-f1-r101-then-b2
         ^{:doc "Selects rooms (with number 101) on the first floors and its
                buildings which have name b2."}
         (is (let [q (tc/r-query "floor#(number=1).room#(number=\"101\") (building#(name=\"b2\"))")]
               (or (tc/check-query q [Room [], Room [Building []]])
                   (tc/check-query q [Room [Building []], Room []])))))

(deftest select-f1-r101-then-b3
         ^{:doc "Selects rooms (with number 101) on the first floors and then
                buildings which have name b3."}
         (is (tc/check-query "floor#(number=1).room#(number=\"101\") (building#(name=\"b3\"))"
                             [Room [], Room []])))

(deftest select-b3-and-flnum1
         ^{:doc ""}
         (is (tc/check-query "building#(name=\"b3\" and floor.number=1)"
                             [])))

(deftest select-b1-and-flnum4
         ^{:doc ""}
         (is (tc/check-query "building#(name=\"b1\" and floor.number=4)"
                             [Building []])))

(deftest select-b1-or-b2-and-flnum4
         ^{:doc ""}
         (is (tc/check-query "building#((name=\"b1\" or name=\"b2\") and floor.number=4)"
                             [Building []])))

(deftest select-b1-or-b2-and-flnum1
         ^{:doc ""}
         (is (tc/check-query "building#((name=\"b1\" or name=\"b2\") and floor.number=1)"
                             [Building [], Building []])))

(deftest select-b3-or-flnum1
         ^{:doc ""}
         (is (tc/check-query "building#(name=\"b3\" or floor.number=1)"
                             [Building [], Building [], Building []])))

(deftest select-flnumgt0
         ^{:doc ""}
         (is (tc/check-query "building#(floor.number>=0)"
                             [Building [], Building []])))

(deftest select-flnumgt3
         ^{:doc ""}
         (is (tc/check-query "building#(floor.number>=3)"
                             [Building []])))

(deftest select-b-flnum1-nest-r
         ^{:doc ""}
         (is (tc/check-query 
               "building#(floor.number=1) (room#(number=\"201\"))"
               [Building [Room []], Building [Room []]])))

(deftest select-b-nest-fngt5-or-fnlt1
         ^{:doc ""}
         (is (tc/check-query 
               "building (floor#(number>5 or number<1))"
               [Building [] Building [] Building []])))


;; Check not= and !=
(deftest check-not=
         (is (tc/check-query "floor#(number not= 1)"
                             [Floor [] Floor [] Floor []]))
         (is (tc/check-query "floor#(number != 1)"
                             [Floor [] Floor [] Floor []]))
         (is (tc/check-query "floor#(number != 1 && number != 2)"
                             [Floor [] Floor []]))
         (is (tc/check-query "floor#(number != 1 && number != 2 && number != 3)"
                             [Floor []]))
         (is (tc/check-query "floor#(number != 1 && number != 2 && number != 3 && number != 4)"
                             []))
         (is (tc/check-query "floor#(number != 1 || number != 2)"
                             [Floor [] Floor [] Floor [] Floor [] Floor []]))
         (is (tc/check-query "floor#(number != 1 || number != 2 && number != 3)"
                             [Floor [] Floor [] Floor [] Floor [] Floor []]))
         (is (tc/check-query "floor#((number != 1 || number != 2) && number != 3)"
                             [Floor [] Floor [] Floor [] Floor []]))
         (is (tc/check-query "floor#(number!=(1 && 2))"
                             [Floor [] Floor []]))
         (is (tc/check-query "floor#(number!=(1 && 2 && 3))"
                             [Floor []]))
         (is (tc/check-query "floor#(number!=(1 && 2 && 3 && 4))"
                             []))
         (is (tc/check-query "floor#(number!=(1 || 2))"
                             [Floor [] Floor [] Floor [] Floor [] Floor []]))
         (is (tc/check-query "floor#(number!=(1 || 2 && 3))"
                             [Floor [] Floor [] Floor [] Floor [] Floor []]))
         (is (tc/check-query "floor#((number!=(1 || 2) && 3))"
                             [Floor [] Floor [] Floor [] Floor []]))
         (is (tc/check-query "floor#(number!=(4 && >2))"
                             [Floor []]))
         (is (tc/check-query "floor#(number!=(1 && =2))"
                             [Floor []]))
         (is (tc/check-query "floor#(number!=(1 && (=2 || =3)))"
                             [Floor [] Floor []]))
         (is (tc/check-query "floor#(number!=(1 && =2 && 3))"
                             [Floor []])))


;; Checks sign
(deftest select-b-fnumgt1
         (is (tc/check-query "building#(floor.number>1)" [Building []]))
         (is (tc/check-query "floor#(number>4)" []))
         (is (tc/check-query "floor#(number>=4)" [Floor []]))
         (is (tc/check-query "floor#(number<1)" []))
         (is (tc/check-query "floor#(number<=1)" [Floor [], Floor []]))
         (is (tc/check-query "floor#(number  >  4)" []))
         (is (tc/check-query "floor#(number    >= 4)" [Floor []]))
         (is (tc/check-query "floor#(number <1)" []))
         (is (tc/check-query "floor#(number<= 1)" [Floor [], Floor []])))



;; Checks reduced restrictions.

(deftest select-reduced-preds
         ^{:doc ""}
         (is (tc/check-query "floor#(number=(1 or 2))" [Floor [], Floor [], Floor []]))
         (is (tc/check-query "floor#(number=(1 or 2 or 3))" [Floor [], Floor [], Floor [], Floor []]))
         (is (tc/check-query "floor#(number=(1 or >4))" [Floor [], Floor []]))
         (is (tc/check-query "floor#(number=(1 or =5))" [Floor [], Floor []]))
         (is (tc/check-query "floor#(number=(<1 or >4))" []))
         (is (tc/check-query "floor#(number=(1 and <2))" [Floor [], Floor []]))
         (is (tc/check-query "floor#(number=(1 and 4))" []))
         (is (tc/check-query "floor#(number=(1 and =4))" []))
         (is (tc/check-query "floor#(number=(=1 and 4))" []))
         (is (tc/check-query "floor#(number=(=1 and =4))" []))
         (is (tc/check-query "floor#(number=(1 or (>3 and <5)))" [Floor [], Floor [], Floor []]))
         (is (tc/check-query "floor#(number=(1 or (>3 and <5)))" [Floor [], Floor [], Floor []]))
         (is (tc/check-query "floor#(number=(1 or (>3 and <5)) or building.name=\"b2\")"
                             [Floor [], Floor [], Floor []]))
         (is (tc/check-query "floor#(number=(<1 or (>3 and <5)) and building.name=\"b3\")"
                             [])))

;; Checks nil

(deftest select-nil
         ^{:doc "Tests keyword nil into predicates."}
         (is (tc/check-query "building#(description != nil)" [Building []]))
         (is (tc/check-query "building#(name != nil)" [Building [], Building [], Building []]))
         (is (tc/check-query "floor#(description != nil)" []))
         (is (tc/check-query "floor#(number = nil)" []))
         (is (tc/check-query "floor#(description=nil)" [Floor [], Floor [], Floor [], Floor [], Floor []]))
         (is (tc/check-query "floor#(number != nil)" [Floor [], Floor [], Floor [], Floor [], Floor []]))
         (is (tc/check-query "building#(description = nil)" [Building [], Building []])))

;; Checks regular expressions
(deftest req-expr
         ^{:doc "Tests predicates which contain regular expressions."}
         (let [rows (tc/rows-query "building#(name~\".*3\")")]
           (is (= (count rows) 1))
           (is (= (nth (nth rows 0) 0) b3)))
         (let [rows (tc/rows-query "floor#(room.number~\"00\")")]
           (is (= (count rows) 1))
           (is (= (nth (nth rows 0) 0) f1_b2)))
         (let [rows (tc/rows-query "room#(number~\"0\")")]
           (is (= (count rows) 13)))
         (let [rows (tc/rows-query "room#(number~\"0$\")")]
           (is (= (count rows) 0)))
         (let [rows (tc/rows-query "building#(room.number~\"0$\" || name=\"b3\")")]
           (is (= (count rows) 1))
           (is (= (nth (nth rows 0) 0) b3)))
         (let [rows (tc/rows-query "building#(room.number~\"0$\" || name~\"^.2$\")")]
           (is (= (count rows) 1))
           (is (= (nth (nth rows 0) 0) b2)))
         (let [rows (tc/rows-query "building#(room.number~\"00\" || name~\"^.5$\")")]
           (is (= (count rows) 1))
           (is (= (nth (nth rows 0) 0) b2)))
         (let [rows (tc/rows-query "building#(room.number~\"0$\")")]
           (is (= (count rows) 0)))
         (let [rows (tc/rows-query "building#(room.number~\"0$\" && name=\"b3\")")]
           (is (= (count rows) 0))))


(deftest req-expr-nil
         ^{:doc "Tests regular expression where string is nil."}
         (is (= (count (tc/rows-query "floor#(name~\".*3\")")) 0)))


;; Checks RCP with string
(deftest rcp-string
         ^{:doc "Tests predicates which contains RCP with strings."}
         (let [rows (tc/rows-query "building#(name=(\"2012\" || =\"2011\"))")]
           (is (= (count rows) 0)))
         (let [rows (tc/rows-query "building#(name=(\"2012\" || ~\"2011\"))")]
           (is (= (count rows) 0)))
         (let [rows (tc/rows-query "building#(name~(\"2012\" || =\"2011\"))")]
           (is (= (count rows) 0)))
         (let [rows (tc/rows-query "building#(name=(\"2012\" || ~\".*2$\"))")]
           (is (= (count rows) 1))
           (is (= (nth (nth rows 0) 0) b2)))
         (let [rows (tc/rows-query "building#(name=(\"b2\" && ~\".*5$\"))")]
           (is (= (count rows) 0)))
         (let [rows (tc/rows-query "building#(name=(~\".*5$\" && \"b2\"))")]
           (is (= (count rows) 0)))
         (let [rows (tc/rows-query "building#(name=(\"b2\" && ~\".*2$\"))")]
           (is (= (count rows) 1))
           (is (= (nth (nth rows 0) 0) b2)))
         (let [rows (tc/rows-query "building#(name=(\"b5\" || ~\"b.\"))")]
           (is (= (count rows) 3)))
         (let [rows (tc/rows-query "building#(name=(\"b5\" || ~\".2\" || ~\".3\"))")]
           (is (= (count rows) 2)))
         (let [rows (tc/rows-query "building#(name=(\"b5\" && ~\".2\" && ~\".3\"))")]
           (is (= (count rows) 0)))
         (let [rows (tc/rows-query "building#(name=(\"b2\" && ~\".2\" && ~\"b.\"))")]
           (is (= (count rows) 1))
           (is (= (nth (nth rows 0) 0) b2))))

;; RCP with != and string.
(deftest rcp-!=string
         ^{:doc "Tests predicates which contains RCP with strings and != conditions."}
         (let [rows (tc/rows-query "building#(name=(\"b1\" || !=\"b2\"))")]
           (is (= (count rows) 2))
           (let [b (nth (nth rows 0) 0)]
             (is (or (= b b1) (= b b3))))
           (let [b (nth (nth rows 1) 0)]
             (is (or (= b b1) (= b b3)))))
         (let [rows (tc/rows-query "building#(name=(!=\"b1\" && !=\"b2\"))")]
           (is (= (count rows) 1))
           (is (= (nth (nth rows 0) 0) b3)))
         (let [rows (tc/rows-query "building#(name~(\"2\" && !=\"b2\"))")]
           (is (= (count rows) 0)))
         (let [rows (tc/rows-query "building#(name~(\"^b.*$\" && !=\"b2\"))")]
           (is (= (count rows) 2))))


(deftest preds-with-dp 
         (binding [tc/*mom* (assoc tc/*mom* 
                                   Floor
                                   (assoc (get tc/*mom* Floor) 
                                          :dp :number))]
           (let [rows (tc/rows-query "building#(floor.=3)")]
             (is (= (count rows) 1))
             (is (= ((nth rows 0) 0) b1)))
           (let [rows (tc/rows-query "building#(floor.=(3 || 4))")]
             (is (= (count rows) 1))
             (is (= ((nth rows 0) 0) b1)))
           (let [rows (tc/rows-query "building#(floor.=(3 && 4))")]
             (is (= (count rows) 1)))
           (let [rows (tc/rows-query "floor#(.=3)")]
             (is (= (count rows) 1))
             (is (= ((nth rows 0) 0) f3_b1)))
           (let [rows (tc/rows-query "floor#(.=3 && .=2)")]
             (is (= (count rows) 0)))
           (let [rows (tc/rows-query "floor#(.=(3 && 2))")]
             (is (= (count rows) 0)))
           ))


(deftest preds-with-ALLm
         (let [cr? #(= (count %1) %2)]
           (let [rows (tc/rows-query "building#(∀floor.number=1)")]
             (is (cr? rows 1))
             (is (= ((nth rows 0) 0) b2)))
           (let [rows (tc/rows-query "building#(∀floor.number=3)")]
             (is (empty? rows)))
           (let [rows (tc/rows-query "floor#(∀room.number~\"^1.*\")")]
             (is (cr? rows 1))
             (is (= ((nth rows 0) 0) f1_b1)))
           (let [rows (tc/rows-query "building (floor#(∀room.number~\"^1.*\"))")]
             (is (cr? rows 3))
             (map #(or (= 0 (% 1)) (= 1 (% 1))) rows))
           (let [rows (tc/rows-query "building#(all:floor.number=1)")]
             (is (cr? rows 1))
             (is (= ((nth rows 0) 0) b2)))
           (let [rows (tc/rows-query "building#(all:floor.number=3)")]
             (is (empty? rows)))
           (let [rows (tc/rows-query "floor#(all:room.number~\"^1.*\")")]
             (is (cr? rows 1))
             (is (= ((nth rows 0) 0) f1_b1)))
           (let [rows (tc/rows-query "building (floor#(all:room.number~\"^1.*\"))")]
             (is (cr? rows 3))
             (is (map #(or (= 0 (% 1)) (= 1 (% 1))) rows)))
           ))


(deftest preds-with-self
         ^{:doc "Tests queries which contains 
                self objects in predicates: long#(& = 5)"}
         (let [t-ints [34 2 20 5 8 12]
               l-em (yzf/c-em t-ints [Long])
               f #(flatten (:rows (c/pquery %1 l-em)))]
           (is (tc/eq-colls [2] (f "long#(& = 2)")))
           (is (tc/eq-colls [2 5] (f "long#(& = (2 || 5))")))
           (is (tc/eq-colls [2 5 20] (f "long#(& = (2 || 5 || 20))")))
           (is (tc/eq-colls [20] (f "long#(& = (2 && 5 || 20))")))
           (is (tc/eq-colls [] (f "long#(& = (2 && (5 || 20)))")))
           (is (tc/eq-colls [2] (f "long#(& < 5)")))
           (is (tc/eq-colls [34] (f "long#(& > 20)")))))


(deftest preds-with-==
         ^{:doc "Tests queries which contains 
                the == binary operation in predicates: long#(& == 5)"}
         (let [p-longs [2 3] ; primitive longs
               c-longs [(Long. 2) (Long. 3)] ; complex longs
               c-ints [(Integer. 2) (Integer. 3)] ; complex ints
               f #(tc/eq-colls %1 (yzf/collq %2 %3 :rtype :rows))]
           (is (f [[2]] "long#(& == 2)" p-longs))
           (is (f [[2]] "long#(& = 2)" p-longs))
           (is (f [] "long#(& == 2)" c-longs))
           (is (f [[2]] "long#(& = 2)" c-longs))
           
           (is (f [] "integer#(& == 2)" c-ints))
           (is (f [[2]] "integer#(& = 2)" c-ints))))


(deftest negative-preds
         ^{:doc "Tests predicates which contains signs with negative symbol (!)."}
         (let [f #(flatten (tc/rows-query %1))]
           (is (tc/eq-colls [f1_b1 f1_b2] (f "floor#(number !> 1)")))
           (is (tc/eq-colls [] (f "floor#(number !>= 1)")))
           (is (tc/eq-colls [f4_b1] (f "floor#(number !< 4)")))
           (is (tc/eq-colls [f3_b1 f4_b1] (f "floor#(number !< 3)")))
           (is (tc/eq-colls [f4_b1] (f "floor#(number !<= 3)")))
           (is (tc/eq-colls [b1 b2 b3] (f "building#(name ~ \"b.*\")")))
           (is (tc/eq-colls [] (f "building#(name !~ \"b.*\")")))
           (is (tc/eq-colls [b2 b3] (f "building#(name !~ \".*1.*\")")))
           (is (tc/eq-colls [b1] (f "building#(name ~ \".*1.*\")"))))
         (let [p-longs [2 3] ; primitive longs
               L2 (Long. 2)
               L3 (Long. 3)
               c-longs [L2 L3] ; complex longs
               I2 (Integer. 2)
               I3 (Integer. 3)
               c-ints [I2 I3] ; complex ints
               f #(tc/eq-colls %1 (yzf/collq %2 %3 :rtype :rows))]
           (is (f [[3]] "long#(& !== 2)" p-longs))
           (is (f [[3]] "long#(& != 2)" p-longs))
           (is (f [[L2] [L3]] "long#(& !== 2)" c-longs))
           (is (f [[L2] [L3]] "long#(& !== 4)" c-longs))
           (is (f [[3]] "long#(& != 2)" c-longs))
           
           (is (f [[I2] [I3]] "integer#(& !== 2)" c-ints))
           (is (f [[I2] [I3]] "integer#(& !== 4)" c-ints))
           (is (f [[3]] "integer#(& != 2)" c-ints))))


(deftest negative-preds
         ^{:doc "Tests >, <, <=, >= functions for not numbers."}
         (let [f #(tc/eq-colls (flatten (tc/rows-query %1)) %2)]
           (is (f "room#(number>\"501\")" []))
           (is (f "room[number]#(number>\"4000\")" ["401" "402"]))
           (is (f "room[number]#(number!<\"4000\")" ["401" "402"]))
           (is (f "room[number]#(number>\"401\")" ["402"]))
           (is (f "room[number]#(number!<=\"401\")" ["402"]))
           (is (f "room[number]#(number>=\"401\")" ["401" "402"]))
           (is (f "room[number]#(number!<\"401\")" ["401" "402"]))

           (is (f "room[number]#(number<\"001\")" []))
           (is (f "room[number]#(number!>=\"001\")" []))
           (is (f "room[number]#(number<\"102\")" ["101" "101" "101" "1001" "1002"]))
           (is (f "room[number]#(number!>=\"102\")" ["101" "101" "101" "1001" "1002"]))
           (is (f "room[number]#(number<=\"101\")" ["101" "101" "101" "1001" "1002"]))
           (is (f "room[number]#(number!>\"101\")" ["101" "101" "101" "1001" "1002"]))))


(deftest keywords-in-rcp
         ^{:doc "Tests keywords into RCP: ."}
         (let [f #(tc/eq-colls (flatten (tc/rows-query %1)) %2)]
           (is (f "building#(description=(\"Some desc\" || =nil))" [b1 b2 b3]))
           (is (f "building#(description=(\"Some desc\" || !=nil))" [b1]))
           (is (f "building#(description=(\"Some desc\" && =nil))" []))
           (is (f "building#(description=(\"Some desc\" && !=nil))" [b1]))
           (is (f "building#(description=(!=nil || \"Some desc\"))" [b1 b2 b3]))
           (is (f "building#(description=(!=nil && =\"Some desc\"))" [b1]))
           (is (f "building#(description=(!=nil || \"Another desc\"))" [b1 b2 b3]))
           (is (f "building#(description=(!=nil && =\"Another desc\"))" []))
           (is (f "device#(forwarding=((1 || 2) && !=true))" []))
           (is (f "device#(forwarding=((1 || 2) && !=false))" []))))
