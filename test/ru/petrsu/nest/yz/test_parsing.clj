(ns ru.petrsu.nest.yz.test-parsing
  ^{:author Vyacheslav Dimitrov
    :doc "Tests for parsing functions."}
  (:use ru.petrsu.nest.yz.parsing 
        ru.petrsu.nest.yz.hb-utils 
        clojure.contrib.test-is)
  (:import (ru.petrsu.nest.son Building Room Floor)))

(def mom 
  ^{:doc "Defines the map of the object model (used Nest's model)"}
  (gen-mom-from-cfg "test-resources/hibernate.cfg.xml"))

(def some-v
  ^{:doc "Defines vector with single empty map."}
  [{:what nil
   :then nil
   :nest nil}])

(deftest t-find-class
         ^{:doc "Tests 'find-class' function."}
         (is (= (find-class "ru.petrsu.nest.son.Building", mom) Building))
         (is (= (find-class "ru.petrsu.nest.son.building", mom) Building))
         (is (= (find-class "building", mom) Building))
         (is (= (find-class "room", mom) Room))
         (is (= (find-class "floor", mom) Floor)))

(deftest t-find-prop
         ^{:doc "Tests 'find-prop' function."}
         (is (find-prop ru.petrsu.nest.son.Building "name" mom))
         (is (find-prop ru.petrsu.nest.son.Building "floors" mom))
         (is (not (find-prop ru.petrsu.nest.son.Building "rooms" mom))))

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

(def st-fun
;  ^{:doc "Defines start of 'preds' function."}
  "#=(eval (fn [o] ")

(def fun
  "ru.petrsu.nest.yz.core/process-preds ")

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
   "building.floor.room (room.floor.building, network (device.building), occupancy)"
   "building.floor.room (room.floor.building (device.building), occupancy)"
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
   "room"])

(deftest parse-remainder
         ^{:doc "Checks remainder after parsing for queries in 'qlist' vector.
                It must be nil for all queries, because qlist contains
                only correct queries."}
         (is (nil? (some #(not (nil? %)) (map #(:remainder (parse+ % mom)) qlist)))))


(deftest t-parse
         ^{:doc "Tests 'parse' function."}
          (is (= (parse "building", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props []
                   :preds nil
                   :then nil
                   :nest nil}]))

         (is (= (parse "building (room)", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props []
                   :preds nil
                   :then nil
                   :nest [{:what ru.petrsu.nest.son.Room
                           :props []
                           :preds nil
                           :then nil
                           :nest nil}]}]))


         (is (= (parse "building (room (device))", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props []
                   :preds nil
                   :then nil
                   :nest [{:what ru.petrsu.nest.son.Room
                           :props []
                           :preds nil
                           :then nil
                           :nest [{:what ru.petrsu.nest.son.Device
                                   :props []
                                   :preds nil
                                   :then nil
                                   :nest nil}]}]}]))


         (is (= (parse "building (room (device (network)))", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props []
                   :preds nil
                   :then nil
                   :nest [{:what ru.petrsu.nest.son.Room
                           :props []
                           :preds nil
                           :then nil
                           :nest [{:what ru.petrsu.nest.son.Device
                                   :props []
                                   :preds nil
                                   :then nil
                                   :nest [{:what ru.petrsu.nest.son.Network
                                           :props []
                                           :preds nil
                                           :then nil
                                           :nest nil}]}]}]}]))


         (is (= (parse "building (room (device (network (floor))))", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props []
                   :preds nil
                   :then nil
                   :nest [{:what ru.petrsu.nest.son.Room
                           :props []
                           :preds nil
                           :then nil
                           :nest [{:what ru.petrsu.nest.son.Device
                                   :props []
                                   :preds nil
                                   :then nil
                                   :nest [{:what ru.petrsu.nest.son.Network
                                           :props []
                                           :preds nil
                                           :then nil
                                           :nest [{:what ru.petrsu.nest.son.Floor
                                                   :props []
                                                   :preds nil
                                                   :then nil
                                                   :nest nil}]}]}]}]}]))

         
         (is (= (parse "building (room (device, floor), network)", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props []
                   :preds nil
                   :then nil
                   :nest [{:what ru.petrsu.nest.son.Room
                           :props []
                           :preds nil
                           :then nil
                           :nest [{:what ru.petrsu.nest.son.Device
                                   :props []
                                   :preds nil
                                   :then nil
                                   :nest nil}
                                  {:what ru.petrsu.nest.son.Floor
                                   :props []
                                   :preds nil
                                   :then nil
                                   :nest nil}]} 
                          {:what ru.petrsu.nest.son.Network
                           :props []
                           :preds nil
                           :then nil
                           :nest nil}]}]))


         (is (= (parse "building (room, device)", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props []
                   :preds nil
                   :then nil
                   :nest [{:what ru.petrsu.nest.son.Room
                           :props []
                           :preds nil
                           :then nil
                           :nest nil}
                          {:what ru.petrsu.nest.son.Device
                           :props []
                           :preds nil
                           :then nil
                           :nest nil}]}]))


         (is (= (parse "building (room, occupancy (device (network (floor)), networkinterface))", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props []
                   :preds nil
                   :then nil
                   :nest [{:what ru.petrsu.nest.son.Room
                           :props []
                           :preds nil
                           :then nil
                           :nest nil}
                          {:what ru.petrsu.nest.son.Occupancy
                           :props []
                           :preds nil
                           :then nil
                           :nest [{:what ru.petrsu.nest.son.Device
                                   :props []
                                   :preds nil
                                   :then nil
                                   :nest [{:what ru.petrsu.nest.son.Network
                                           :props []
                                           :preds nil
                                           :then nil
                                           :nest [{:what ru.petrsu.nest.son.Floor
                                                   :props []
                                                   :preds nil
                                                   :then nil
                                                   :nest nil}]}]}
                                  {:what ru.petrsu.nest.son.NetworkInterface
                                   :props []
                                   :preds nil
                                   :then nil
                                   :nest nil}]}]}]))

         (is (= (parse "building, room", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props []
                   :preds nil
                   :then nil
                   :nest nil}
                  {:what ru.petrsu.nest.son.Room 
                   :props []
                   :preds nil
                   :then nil
                   :nest nil}]))
 
        

(deftest t-parse-props
         ^{:doc "Tests parsing queries with properties."}
         (is (= (parse "building.name", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props [["name" false]]
                   :preds nil
                   :then nil
                   :nest nil}]))


         (is (= (parse "building.room.floor.rooms", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props []
                   :preds nil
                   :then {:what ru.petrsu.nest.son.Room 
                          :props []
                          :preds nil 
                          :then {:what ru.petrsu.nest.son.Floor 
                                 :props [["rooms" false]]
                                 :preds nil 
                                 :then nil}}
                   :nest nil}]))


         (is (= (parse "building.floor.room.occupancy.device.forwarding", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props []
                   :preds nil
                   :then {:what ru.petrsu.nest.son.Floor 
                          :props []
                          :preds nil 
                          :then {:what ru.petrsu.nest.son.Room
                                 :props [] 
                                 :preds nil 
                                 :then {:what ru.petrsu.nest.son.Occupancy
                                        :props []
                                        :preds nil 
                                        :then {:what ru.petrsu.nest.son.Device
                                               :props [["forwarding" false]] 
                                               :preds nil 
                                               :then nil}}}}
                   :nest nil}]))


         (is (= (parse "simpleou[*parent]", mom)
                 [{:what ru.petrsu.nest.son.SimpleOU 
                   :props [["parent" true]]
                   :preds nil
                   :then nil 
                   :nest nil}]))


         (is (= (parse "compositeou[*parent OUs]", mom)
                 [{:what ru.petrsu.nest.son.CompositeOU 
                   :props [["parent" true] ["OUs" false]]
                   :preds nil
                   :then nil 
                   :nest nil}]))


         (is (= (parse "compositeou[OUs *parent]", mom)
                 [{:what ru.petrsu.nest.son.CompositeOU 
                   :props [["OUs" false] ["parent" true]]
                   :preds nil
                   :then nil 
                   :nest nil}]))


         (is (= (parse "building.room.number", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props []
                   :preds nil
                   :then {:what ru.petrsu.nest.son.Room 
                          :props [["number" false]]
                          :preds nil 
                          :then nil}
                   :nest nil}]))))


(deftest t-parse-predicates
         ^{:doc "Tests parsing queries with some restrictions."}
          (is (= (parse "building#(name=1)", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props []
                   :preds (str st-fun "(" fun "o, [\"name\"], =, 1)))")
                   :then nil
                   :nest nil}]))
          (is (= (parse "building#(name=1 and address=2)", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props []
                   :preds (str st-fun "(and (" fun "o, [\"name\"], =, 1) " 
                                      "(" fun "o, [\"address\"], =, 2))))")
                   :then nil
                   :nest nil}]))
          (is (= (parse "building#(name=1 or address=2)", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props []
                   :preds (str st-fun "(or (" fun "o, [\"name\"], =, 1) " 
                                      "(" fun "o, [\"address\"], =, 2))))")
                   :then nil
                   :nest nil}]))
          (is (= (parse "building#(name=1 and address=2 and floor.number=3)", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props []
                   :preds (str st-fun "(and (and (" fun "o, [\"name\"], =, 1) " 
                                      "(" fun "o, [\"address\"], =, 2)) "
                                      "(" fun "o, [\"floors\" \"number\"], =, 3))))")
                   :then nil
                   :nest nil}]))
          (is (= (parse "building#(name=1 and address=2 or floor.number=3)", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props []
                   :preds (str st-fun "(or (and (" fun "o, [\"name\"], =, 1) " 
                                      "(" fun "o, [\"address\"], =, 2)) "
                                      "(" fun "o, [\"floors\" \"number\"], =, 3))))")
                   :then nil
                   :nest nil}]))
          (is (= (parse "building#(name=1 or address=2 and floor.number=3)", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props []
                   :preds (str st-fun "(or (" fun "o, [\"name\"], =, 1) "
                                      "(and (" fun "o, [\"address\"], =, 2) " 
                                      "(" fun "o, [\"floors\" \"number\"], =, 3)))))")
                   :then nil
                   :nest nil}]))
          (is (= (parse "building#(name=1 and (address=2 or floor.number=3))", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props []
                   :preds (str st-fun "(and (" fun "o, [\"name\"], =, 1) " 
                                      "(or (" fun "o, [\"address\"], =, 2) "
                                      "(" fun "o, [\"floors\" \"number\"], =, 3)))))")
                   :then nil
                   :nest nil}])))


(defmacro create-is [q mom] `(is (nil? (:remainder (parse+ ~q ~mom)))))

(deftest t-parse-remainder
         ^{:doc "Denerates is for each query from qlist. 
                Like 'parse-remainder, but it can show remainder.'"}
         (dotimes [n (count qlist)]
           (create-is (qlist n) mom)))
