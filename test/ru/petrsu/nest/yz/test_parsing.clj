;;
;; Copyright 2011 Vyacheslav Dimitrov <vyacheslav.dimitrov@gmail.com>
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

(ns ru.petrsu.nest.yz.test-parsing
  ^{:author "Vyacheslav Dimitrov"
    :doc "Tests for parsing functions."}
  (:use ru.petrsu.nest.yz.parsing 
        ru.petrsu.nest.yz.hb-utils 
        clojure.test)
  (:import (ru.petrsu.nest.son Building Room Floor)))

(def mom- 
  ^{:doc "Defines the map of the object model (used Nest's model)"}
  (mom-from-file "nest.mom"))

(def some-v
  ^{:doc "Defines vector with single empty map."}
  [{:what nil
   :then nil
   :nest nil}])

(comment
(deftest t-find-class
         ^{:doc "Tests 'find-class' function."}
         (is (= (find-class "ru.petrsu.nest.son.Building", mom-) Building))
         (is (= (find-class "ru.petrsu.nest.son.building", mom-) Building))
         (is (= (find-class "building", mom-) Building))
         (is (= (find-class "room", mom-) Room))
         (is (= (find-class "floor", mom-) Floor)))

(deftest t-find-prop
         ^{:doc "Tests 'find-prop' function."}
         (is (find-prop ru.petrsu.nest.son.Building "name" mom-))
         (is (find-prop ru.petrsu.nest.son.Building "floors" mom-))
         (is (not (find-prop ru.petrsu.nest.son.Building "rooms" mom-))))
)

(deftest t-get-in-nest
         ^{:doc "Tests 'get-in-nest' function"}
         (is (nil? (get-in-nest some-v 0 :what)))
         (is (= "1" (get-in-nest [(assoc (some-v 0) :what "1")] 0 :what)))
         (let [some-vv (assoc-in-nest some-v 0 :nest some-v)
               some-vvv (assoc-in-nest some-vv 1 :nest some-v)
               some-vvvv (assoc-in-nest some-vvv 2 :nest some-v)]
           (is (= "2" (get-in-nest (assoc-in-nest some-vv 1 :what "2") 1 :what)))
           (is (= "3" (get-in-nest (assoc-in-nest some-vvv 2 :what "3") 2 :what)))
           (is (= "4" (get-in-nest (assoc-in-nest some-vvvv 3 :what "4") 3 :what)))))


(defn sort-to-nil
  "Takes the MOM and associates with the :sort key 
  the [nil nil nil] value. Returns new MOM."
  [mom]
  (reduce (fn [m [k v]] (assoc m k (assoc v :sort [nil nil nil]))) {} mom))


(deftest t-parse
         ^{:doc "Tests 'parse' function."}
         (let [mom- (sort-to-nil mom-)]
          (is (= (parse "building", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :props []
                   :sort nil
                   :where nil}]))


         (is (= (parse "building (room)", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :props []
                   :sort nil
                   :where nil
                   :nest [{:what ru.petrsu.nest.son.Room
                           :props []
                           :sort nil
                           :where [["floors" "rooms"]]}]}]))


         (is (= (parse "building (room (device))", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :props []
                   :where nil
                   :sort nil
                   :nest [{:what ru.petrsu.nest.son.Room
                           :props []
                           :sort nil
                           :where [["floors" "rooms"]]
                           :nest [{:what ru.petrsu.nest.son.Device
                                   :props []
                                   :sort nil
                                   :where [["occupancies" "devices"]]}]}]}]))


         (is (= (parse "building (room (device (network)))", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :props []
                   :where nil
                   :sort nil
                   :nest [{:what ru.petrsu.nest.son.Room
                           :props []
                           :where [["floors" "rooms"]] 
                           :sort nil
                           :nest [{:what ru.petrsu.nest.son.Device
                                   :props []
                                   :where [["occupancies" "devices"]]
                                   :sort nil
                                   :nest [{:what ru.petrsu.nest.son.Network
                                           :props [] 
                                           :sort nil
                                           :where [["linkInterfaces" "networkInterfaces" "network"]]}]}]}]}]))


         (is (= (parse "building (room (device (network (floor))))", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :props []
                   :where nil
                   :sort nil
                   :nest [{:what ru.petrsu.nest.son.Room
                           :props []
                           :where [["floors" "rooms"]]
                           :sort nil
                           :nest [{:what ru.petrsu.nest.son.Device
                                   :props []
                                   :where [["occupancies" "devices"]] 
                                   :sort nil
                                   :nest [{:what ru.petrsu.nest.son.Network
                                           :props [] 
                                           :sort nil
                                           :where [["linkInterfaces" "networkInterfaces" "network"]]
                                           :nest [{:what ru.petrsu.nest.son.Floor
                                                   :props [] 
                                                   :sort nil
                                                   :where [["networkInterfaces" 
                                                            "linkInterface" 
                                                            "device" 
                                                            "occupancy" 
                                                            "room" "floor"]]}]}]}]}]}]))

         
         (is (= (parse "building (room (device, floor), network)", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :props []
                   :where nil 
                   :sort nil
                   :nest [{:what ru.petrsu.nest.son.Room
                           :where [["floors" "rooms"]]
                           :props [] 
                           :sort nil
                           :nest [{:what ru.petrsu.nest.son.Device
                                   :props [] 
                                   :sort nil
                                   :where [["occupancies" "devices"]]}
                                  {:what ru.petrsu.nest.son.Floor
                                   :props [] 
                                   :sort nil
                                   :where [["floor"]]}]} 
                          {:what ru.petrsu.nest.son.Network
                           :props [] 
                           :sort nil
                           :where [["floors" "rooms" "occupancies" "devices" "linkInterfaces" "networkInterfaces" "network"]]}]}]))


         (is (= (parse "building (room, device)", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :props []
                   :where nil 
                   :sort nil
                   :nest [{:what ru.petrsu.nest.son.Room
                           :props [] 
                           :sort nil
                           :where [["floors" "rooms"]]}
                          {:what ru.petrsu.nest.son.Device
                           :props [] 
                           :sort nil
                           :where [["floors" "rooms" "occupancies" "devices"]]}]}]))

         (is (= (parse "building (room, occupancy (device (network (floor)), networkinterface))", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :props []
                   :where nil 
                   :sort nil
                   :nest [{:what ru.petrsu.nest.son.Room
                           :props [] 
                           :sort nil
                           :where [["floors", "rooms"]]}
                          {:what ru.petrsu.nest.son.Occupancy
                           :props [] 
                           :sort nil
                           :where [["floors" "rooms" "occupancies"]]
                           :nest [{:what ru.petrsu.nest.son.Device
                                   :props [] 
                                   :sort nil
                                   :where [["devices"]]
                                   :nest [{:what ru.petrsu.nest.son.Network
                                           :props [] 
                                           :sort nil
                                           :where [["linkInterfaces" "networkInterfaces" "network"]]
                                           :nest [{:what ru.petrsu.nest.son.Floor
                                                   :props [] 
                                                   :sort nil
                                                   :where [["networkInterfaces" 
                                                            "linkInterface" 
                                                            "device" 
                                                            "occupancy" 
                                                            "room" "floor"]]}]}]}
                                  {:what ru.petrsu.nest.son.NetworkInterface
                                   :props [] 
                                   :sort nil
                                   :where [["devices" "linkInterfaces" "networkInterfaces"]]}]}]}]))

         (is (= (parse "building, room", mom-)
                 [{:where nil 
                   :sort nil
                   :what ru.petrsu.nest.son.Building
                   :props []}
                  {:where nil 
                   :sort nil
                   :what ru.petrsu.nest.son.Room 
                   :props []}]))))

(deftest t-parse-props
         ^{:doc "Tests parsing queries with properties."}
         (let [mom- (sort-to-nil mom-)]
         (is (= (parse "building.name", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :sort nil
                   :props [[:name false]]
                   :where nil}]))


         (is (= (parse "building.room.floor.rooms", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :props [] 
                   :sort nil
                   :then {:what ru.petrsu.nest.son.Room 
                          :props [] 
                          :sort nil
                          :where [["floors" "rooms"]]
                          :then {:what ru.petrsu.nest.son.Floor 
                                 :where [["floor"]] 
                                 :sort nil
                                 :props [[:rooms false]]}}
                   :where nil}]))


         (is (= (parse "building.floor.room.occupancy.device.forwarding", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :props [] 
                   :sort nil
                   :then {:what ru.petrsu.nest.son.Floor 
                          :props []
                          :where [["floors"]] 
                          :sort nil
                          :then {:what ru.petrsu.nest.son.Room
                                 :props []  
                                 :sort nil
                                 :where [["rooms"]]
                                 :then {:what ru.petrsu.nest.son.Occupancy
                                        :props []  
                                        :sort nil
                                        :where [["occupancies"]]
                                        :then {:what ru.petrsu.nest.son.Device
                                               :props [[:forwarding false]]  
                                               :sort nil
                                               :where [["devices"]]}}}}
                   :where nil}]))


         (is (= (parse "simpleou[*parent]", mom-)
                 [{:what ru.petrsu.nest.son.SimpleOU 
                   :props [[:parent true]]
                   :sort nil
                   :where nil}]))


         (is (= (parse "compositeou[*parent OUs]", mom-)
                 [{:what ru.petrsu.nest.son.CompositeOU 
                   :props [[:parent true] [:OUs false]]
                   :sort nil
                   :where nil}]))


         (is (= (parse "compositeou[OUs *parent]", mom-)
                 [{:what ru.petrsu.nest.son.CompositeOU 
                   :props [[:OUs false] [:parent true]]
                   :sort nil
                   :where nil}]))


         (is (= (parse "building.room.number", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :props [] 
                   :sort nil
                   :where nil
                   :then {:what ru.petrsu.nest.son.Room 
                          :props [[:number false]] 
                          :sort nil
                          :where [["floors" "rooms"]]}}]))))


(deftest t-parse-predicates
         ^{:doc "Tests parsing queries with some restrictions."}
         (let [mom- (sort-to-nil mom-)]
          (is (= (parse "building#(name=1)", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :props []
                   :sort nil
                   :preds [{:ids [{:id ["name"] :cl nil}], :func #'clojure.core/=, :value 1}]
                   :where nil}]))
          (is (= (parse "building#(room.number=\"215\")", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :props []
                   :sort nil
                   :preds [{:ids [{:id ["floors" "rooms"] :cl Room} 
                                  {:id ["number"] :cl nil}], :func #'clojure.core/=, :value "215"}]
                   :where nil}]))
          (is (= (parse "building#(name=1 and address=2)", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :props []
                   :sort nil
                   :preds [{:ids [{:id ["name"] :cl nil}], :func #'clojure.core/=, :value 1} 
                           {:ids [{:id ["address"] :cl nil}], :func #'clojure.core/=, :value 2} :and]
                   :where nil}]))
          (is (= (parse "building#(name=1 or address=2)", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :props []
                   :sort nil
                   :preds [{:ids [{:id ["name"] :cl nil}], :func #'clojure.core/=, :value 1} 
                           {:ids [{:id ["address"] :cl nil}], :func #'clojure.core/=, :value 2} :or]
                   :where nil}]))
          (is (= (parse "building#(name=1 and address=2 and floor.number=3)", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :props []
                   :sort nil
                   :preds [{:ids [{:id ["name"], :cl nil}], :func #'clojure.core/=, :value 1} 
                           {:ids [{:id ["address"], :cl nil}], :func #'clojure.core/=, :value 2} 
                           :and {:ids [{:id ["floors"] :cl Floor}, {:id ["number"] :cl nil}], :func #'clojure.core/=, :value 3} :and]
                   :where nil}]))
          (is (= (parse "building#(name=1 and address=2 or floor.number=3)", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :props []
                   :sort nil
                   :preds [{:ids [{:id ["name"], :cl nil}], :func #'clojure.core/=, :value 1} 
                           {:ids [{:id ["address"], :cl nil}], :func #'clojure.core/=, :value 2} 
                           :and {:ids [{:id ["floors"] :cl Floor}, {:id ["number"] :cl nil}], :func #'clojure.core/=, :value 3} :or]
                   :where nil}]))
          (is (= (parse "building#(name=1 or address=2 and floor.number=3)", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :props []
                   :sort nil
                   :preds [{:ids [{:id ["name"], :cl nil}], :func #'clojure.core/=, :value 1} 
                           {:ids [{:id ["address"], :cl nil}], :func #'clojure.core/=, :value 2} 
                           {:ids [{:id ["floors"] :cl Floor}, {:id ["number"] :cl nil}], :func #'clojure.core/=, :value 3} :and :or]
                   :where nil}]))
          (is (= (parse "building#(name=1 and (address=2 or floor.number=3))", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :props []
                   :sort nil
                   :preds [{:ids [{:id ["name"], :cl nil}], :func #'clojure.core/=, :value 1} 
                           {:ids [{:id ["address"], :cl nil}], :func #'clojure.core/=, :value 2} 
                           {:ids [{:id ["floors"] :cl Floor}, {:id ["number"] :cl nil}], :func #'clojure.core/=, :value 3} :or :and]
                   :where nil}]))))


(defmacro create-is [q mom-] `(is (nil? (:remainder (parse+ ~q ~mom-)))))

(def qlist
  ^{:doc "Defines list of YZ's queries (used Nest's model)."}
  [
;; Selections   
   "building"
   "building.room"
   "building.floor.room"
   "building.floor.room.occupancy"
   "building, room"
   "building (room)"
   "building (room (device))"
   "building (room (device (networkinterface)))"
   "building (room.floor (device))"
   "building (room.floor (device), occupancy)"
   "building.floor (room.floor.building, network (device.building), occupancy)"
   "building.floor (room.floor.building (device.building), occupancy)"
   "building.name"
   "building.floors"
   "building.room.floor.rooms"
   "building (room.number)"
   "building (device.forwarding)"
   "building (room.device.forwarding)"
   "building (room.device.forwarding, floor)"
   "building (room.device.forwarding, floor, network.building.floors)"
   "building (room.device.forwarding, floor (network.building.floors))"
;; Restrictions.
   "floor#(number=1)"
   "floor#(number=1 or number=2)"
   "floor#(number=(1 or 2))"
   "floor#(number=(1 or (2 or 3)))"
   "floor#(number=(1 and (2 or 3)))"
   "floor#(number=(1 and (2 or 3 and 4)))"
   "floor#(number=1) (room)"
   "floor#(number=1) (room (device))"
   "floor#(number=1) (room (device)), building"
   "floor#(number=1).room"
   "floor#(number=(1 and (2 or 3 and 4))).room.building.name"
   "floor#(number=1 and name=2)"
   "floor#(number=1 or name=2)"
   "floor#((number=1 or number=2) and name=3)"
   "floor#((number=1 or number=2) and name=3), room"
   "floor#((number=1 or number=2) and name=3), room#(number=1)"
   "floor#((number=1 or number=2) and name=3), room#(number=1 and name=2)"
   "floor#((number=1 or number=2) and name=3) (room#(number=1))"
   "floor#((number=1 or number=2) and name=3) (room#(number=1 and name=2))" 
   "building#(name=(\"2012\" || =\"2011\"))"
   "building#(name=(\"2012\" || ~\"2011\"))"
   "building#(name~(\"2012\" || = \"2011\"))"
   "building#(name~(=\"2012\" || \"2011\"))"
   "building#(name~(=\"2012\" || ~\"2011\"))"
   "building#(name=(\"2012\" && =\"2011\"))"
   "building#(name=(\"2012\" && ~\"2011\"))"
   "building#(name~(\"2012\" && = \"2011\"))"
   "building#(name~(=\"2012\" && \"2011\"))"
   "building#(name~(=\"2012\" && ~\"2011\"))"
   "building#(name=(\"2012\" && =\"2011\" || \"2010\"))"
   "building#(name=(\"2012\" && ~\"2011\" || =\"2010\"))"
   "building#(name~(\"2012\" && = \"2011\" || ~\"2010\"))"
   "building#(name~(=\"2012\" && \"2011\" && ~\"2011\"))"
   "building#(name~(=\"2012\" && (~\"2011\" || =\"2011\")))"
   "building#(name=(\"2012\" && (=\"2011\" || \"2010\")))"
   "building#(name=(\"2012\" && (~\"2011\" || =\"2010\")))"
   "building#(name~(\"2012\" && (= \"2011\" || ~\"2010\")))"
   "building#(name~(=\"2012\" && \"2011\" && ~\"2011\"))"
   "building#(name=((\"2012\" && =\"2011\") || \"2010\"))"
   "building#(name=((\"2012\" && ~\"2011\") || =\"2010\"))"
   "building#(name~((\"2012\" && = \"2011\") || ~\"2010\"))"
   "building#(name~((=\"2012\" && \"2011\") && ~\"2011\"))"

;; not= predicate
   "floor#(number not= 1 and name not= 2)"
   "floor#(number not=1 or name not=2)"
   "floor#((number not= 1 or number=2) and name=3)"
   "floor#((number=1 or number not= 2) and name not= 3), room"
   "floor#((number=1 or number=2) and name=3), room#(number not= 1)"
   "floor#((number=1 or number=2) and name=3), room#(number not= 1 and name not= 2)"
   "floor#((number=1 or number=2) and name=3) (room#(number not= 1))"
   "floor#((number=1 or number=2) and name=3) (room#(number=1 and name not= 2))"

;; Sugar for "and" (&&), "or" (||) "and" not= (!=)
   "floor#(number=1 || number=2)"
   "floor#(number=(1 || 2))"
   "floor#(number=(1 || (2 || 3)))"
   "floor#(number=(1 && (2 || 3)))"
   "floor#(number=(1 && (2 || 3 && 4)))"
   "floor#(number=(1 && (2 || 3 && 4))).room.building.name"
   "floor#(number=1 && name=2)"
   "floor#(number=1 || name=2)"
   "floor#((number=1 || number=2) && name=3)"
   "floor#((number=1 || number=2) && name=3), room"
   "floor#((number=1 || number=2) && name=3), room#(number=1)"
   "floor#((number=1 || number=2) && name=3), room#(number=1 && name=2)"
   "floor#((number=1 || number=2) && name=3) (room#(number=1))"
   "floor#((number=1 || number=2) && name=3) (room#(number=1 && name=2))"
   "floor#(number != 1 && name != 2)"
   "floor#(number !=1 || name !=2)"
   "floor#((number != 1 || number=2) and name=3)"
   "floor#((number=1 || number != 2) and name != 3), room"
   "floor#((number=1 || number!= 2) and name!= 3), room"
   "floor#((number=1 || number=2) && name=3), room#(number != 1)"
   "floor#((number=1 || number=2) && name=3), room#(number != 1 && name != 2)"
   "floor#((number=1 || number=2) && name=3) (room#(number != 1))"
   "floor#((number=1 || number=2) && name=3) (room#(number=1 && name != 2))"

;; Properties.
   "floor[name]"
   "floor[name number]"
   "floor[name number rooms]"
   "room[name number]"
   "room[name number floor]"
   "room[name number floor].floor"
   "room[name number floor].floor[number]"
   "room[name number floor].floor[number name rooms]"
   "room.floor[number name rooms]"
   "room[name number floor].floor[number name rooms].building"
   "room[name number floor].floor[number name rooms].building[name]"
   "room[name number floor].floor[number name rooms].building[name address]"
   "room[name number floor].floor[number name rooms].building[name address floors]"
   "room.floor[number name rooms].building[name address floors]"
   "room.floor.building[name address floors]"
   "simpleou[*parent]"
   "simpleou[*parent name]"
   "compositeou[*parent OUs]"
   "building (simpleou[*parent])"
   "building[name] (simpleou[*parent])"
   "building[name] (simpleou[*parent name])"
   "building[&]"
   "building[& name]"
   "building[name &]"
   "building[name floors &]"
   "building[name & floors]"
   "building[& name floors]"
   "building (room[&])"
   "building (room[number &])"
   "building (room[& number])"
   "building[name] (room[& number])"
   "building[name] (room[number &])"
   "building[name &] (room[number &])"

;; Functions as query.
   "@(str \"One - \" 1 \", Two -\" 2)"
   "@(str `room')"
   "@(str %room')"
   "@(str $room')"
   "@(str `floor')"
   "@(str %floor')"
   "@(str $floor')"
   "@(str `floor' `room')"
   "@(str `floor' %room')"
   "@(str `floor' $room')"
   "@(str %floor' %room')"
   "@(str %floor' `room')"
   "@(str %floor' $room')"
   "@(str $floor' $room')"
   "@(str $floor' `room')"
   "@(str $floor' %room')"
   "@(str `floor' 1 `room')"
   "@(str `floor' 2 $room')"
   "@(str `floor' 3 %room')"
   "@(str `floor' 1 `room' 3)"
   "@(str `floor' 2 $room' 2)"
   "@(str `floor' 3 %room' 1)"
   "@(str `floor' `room' 3)"
   "@(str `floor' $room' 2)"
   "@(str `floor' %room' 1)"
   "@(str 1 `floor' `room' 3)"
   "@(str 2 `floor' $room' 2)"
   "@(str 3 `floor' %room' 1)"
   "@(str 1 `floor' 4 `room' 3)"
   "@(str 2 `floor' 5 $room' 2)"
   "@(str 3 `floor' 6 %room' 1)"

;; Keywords
   "device#(forwarding=true)"
   "device#(forwarding=false)"
   "device#(forwarding=nil)"
   "device#(forwarding!=true)"
   "device#(forwarding!=false)"
   "device#(forwarding!=nil)"
   "device#(forwarding =true)"
   "device#(forwarding =false)"
   "device#(forwarding =nil)"
   "device#(forwarding !=true)"
   "device#(forwarding !=false)"
   "device#(forwarding !=nil)"
   "device#(forwarding = true)"
   "device#(forwarding = false)"
   "device#(forwarding = nil)"
   "device#(forwarding != true)"
   "device#(forwarding != false)"
   "device#(forwarding != nil)"
   "device#(forwarding= true)"
   "device#(forwarding= false)"
   "device#(forwarding= nil)"
   "device#(forwarding!= true)"
   "device#(forwarding!= false)"
   "device#(forwarding!= nil)"

;; Default property
   "ni[@(ip &.inetAddress)]"
   "ni[@(ip &.)]"
   "ni[@(ip &)]"
   "ni[name @(ip &)]"
   "ni[@(ip &.) name]"
   "ni[description @(ip &.) name]"
   "ni[@(ip &.) description name]"
   "ni[&.]"
   "ni[name &.]"
   "ni[&. name]"
   "ni[description &. name]"
   "ni[&. description name]"

;; Sorting
   "↑room"
   "↓room"
   "↓room.↓number"
   "↓room.↑number"
   "↓room.number"
   "↑room (building)"
   "room (↓building)"
   "↑room (↓building)"
   "↑room (floor (↓building))"
   "↑room (device (floor (↓building)))"
   "↑room (device (↓floor (↓building)))"
   "↑room (↓device (↓floor (↓building)))"
   "↑room (device (floor, ↓building))"
   "↑building (↓room)"

   "room[↓number]"
   "room[& ↓number]"
   "room[↓& ↓number]"
   "room[name & ↓number]"
   "room[↓& name ↑number]"

   "↓room[↓number]"
   "↓room[& ↓number]"
   "↓room[↓& ↓number]"
   "↓room[name & ↓number]"
   "↓room[↓& name ↑number]"

   "↓room[number]"
   "↓room[& number]"
   "↓room[& number]"
   "↓room[name & number]"
   "↓room[& name number]"

   "room.building[↓description]"
   "room.building[& ↓description]"
   "room.building[↓& ↓description]"
   "room.building[name & ↓description]"
   "room.building[↓& name ↑description]"
   "room (building[↓description])"
   "room (building[& ↓description])"
   "room (building[↓& ↓description])"
   "room (building[name & ↓description])"
   "room (building[↓& name ↑description])"

;; Regular expressions
   "room#(number~\".*\")"
   "room#(number~\".00$\")"
   "room#(number~\".00$\" || number=\"215\")"
   "room#(number~\".00$\" || number~\"215\")"
   "room#(number=\".00$\" || number~\"215\")"
   "building (room#(number~\".*\"))"
   "building (room#(number~\".00$\"))"
   "building (room#(number~\".00$\" || number=\"215\"))"
   "building (room#(number~\".00$\" || number~\"215\"))"
   "building (room#(number=\".00$\" || number~\"215\"))"
   "building (room#(number~\".00$\" && number=\"215\"))"
   "building (room#(number~\".00$\" && number~\"215\"))"
   "building (room#(number=\".00$\" && number~\"215\"))"
   "building#(name~\".*\") (room#(number~\".00$\" && number=\"215\"))"
   "building#(name~\".*\" && description=\"1\") (room#(number~\".00$\" && number~\"215\"))"
   "building#(name=\".*\" && description~\"1\") (room#(number=\".00$\" && number~\"215\"))"
   "building (device (room (floor (network#(name~\".*\")))))"
   "building.device.room.floor.network#(name~\".*\")"
   "building, room#(number~\".*\")"
   "building, floor (room#(number~\".*\"))"
   "building#(name~\".*\"), room#(number~\".*\")"
   "floor (building, room#(number~\".*\"))"
   "room"])

(def qlist-list
  ^{:doc "Defines list with query-function with parameter (as :list) from qlist"}
  (vec (map #(str "@(str `" % "')") qlist)))

(def qlist-single
  ^{:doc "Defines list with query-function with parameter (as :single) from qlist"}
  (vec (map #(str "@(str $" % "')") qlist)))

(def qlist-indep
  ^{:doc "Defines list with query-function with parameter (as :indep) from qlist"}
  (vec (map #(str "@(str %" % "')") qlist)))

(def qlist-prop
  ^{:doc "Defines list with queries which have function as property."}
  (vec (flatten (list 
                  (map #(str "building[& @(str `" % "')]") qlist)
                  (map #(str "building[@(str `" % "') &]") qlist)
                  (map #(str "building[@(str `" % "') & @(str `" % "')]") qlist)
                  (map #(str "building[& @(str `" % "') @(str `" % "')]") qlist)
                  (map #(str "building[@(str `" % "') @(str `" % "') &]") qlist)
                  (map #(str "building[& @(str $" % "')]") qlist)
                  (map #(str "building[& @(str %" % "')]") qlist)))))

(def qlist-pred
  ^{:doc "Defines list with queries which have function as predicate."}
  (vec (flatten (list 
                  (map #(str "building#(@(str `" % "') > 3)") qlist)
                  (map #(str "building#(@(str `" % "') < 3)") qlist)
                  (map #(str "building#(@(str `" % "') = @(str $" % "'))") qlist)
                  (map #(str "building#(@(str $" % "') = @(str %" % "'))") qlist)))))


(defmacro premainder
  "Generates code for checking remainder about specified list with queries."
  [l]
  `(is (nil? (some #(not (nil? %)) (map #(:remainder (parse+ % mom-)) ~l)))))


(deftest parse-remainder
         ^{:doc "Checks remainder after parsing for queries in 'qlist' vector.
                It must be nil for all queries, because qlist contains
                only correct queries."}

         ; If all queries are success, then results is nil, 
         ; otherwise results is query which is failed and
         ; clojure.test prints something like this: 
         ;   expected: (nil? results)
         ;   actual: (not (nil? "room[name number floor].floor"))
         ;
         ; It is all I need.
         (let [results (some #(let [r (:remainder (parse+ % mom-))]
                                (if r %)) qlist)]
           (is (nil? results))))


(comment
(deftest t-parse-remainder
         ^{:doc "Generates 'is' for each query from qlist. 
                Like 'parse-remainder, but it can show remainder.'"}
         (let [f #(dotimes [n (count %)]
                   (create-is (% n) mom-))]
           (f qlist)
           (f qlist-indep)
           (f qlist-single)
           (f qlist-prop)
           (f qlist-pred)
           (f qlist-list)))
)
