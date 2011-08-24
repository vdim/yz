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


(def qlist
  ^{:doc "Defines list of YZ's queries (used Nest's model)."}
  ["building"
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
                   :props nil
                   :preds nil
                   :then nil
                   :nest nil}]))

         (is (= (parse "building (room)", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props nil
                   :preds nil
                   :then nil
                   :nest [{:what ru.petrsu.nest.son.Room
                           :props nil
                           :preds nil
                           :then nil
                           :nest nil}]}]))


         (is (= (parse "building (room (device))", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props nil
                   :preds nil
                   :then nil
                   :nest [{:what ru.petrsu.nest.son.Room
                           :props nil
                           :preds nil
                           :then nil
                           :nest [{:what ru.petrsu.nest.son.Device
                                   :props nil
                                   :preds nil
                                   :then nil
                                   :nest nil}]}]}]))


         (is (= (parse "building (room (device (network)))", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props nil
                   :preds nil
                   :then nil
                   :nest [{:what ru.petrsu.nest.son.Room
                           :props nil
                           :preds nil
                           :then nil
                           :nest [{:what ru.petrsu.nest.son.Device
                                   :props nil
                                   :preds nil
                                   :then nil
                                   :nest [{:what ru.petrsu.nest.son.Network
                                           :props nil
                                           :preds nil
                                           :then nil
                                           :nest nil}]}]}]}]))


         (is (= (parse "building (room (device (network (floor))))", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props nil
                   :preds nil
                   :then nil
                   :nest [{:what ru.petrsu.nest.son.Room
                           :props nil
                           :preds nil
                           :then nil
                           :nest [{:what ru.petrsu.nest.son.Device
                                   :props nil
                                   :preds nil
                                   :then nil
                                   :nest [{:what ru.petrsu.nest.son.Network
                                           :props nil
                                           :preds nil
                                           :then nil
                                           :nest [{:what ru.petrsu.nest.son.Floor
                                                   :props nil
                                                   :preds nil
                                                   :then nil
                                                   :nest nil}]}]}]}]}]))

         
         (is (= (parse "building (room (device, floor), network)", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props nil
                   :preds nil
                   :then nil
                   :nest [{:what ru.petrsu.nest.son.Room
                           :props nil
                           :preds nil
                           :then nil
                           :nest [{:what ru.petrsu.nest.son.Device
                                   :props nil
                                   :preds nil
                                   :then nil
                                   :nest nil}
                                  {:what ru.petrsu.nest.son.Floor
                                   :props nil
                                   :preds nil
                                   :then nil
                                   :nest nil}]} 
                          {:what ru.petrsu.nest.son.Network
                           :props nil
                           :preds nil
                           :then nil
                           :nest nil}]}]))


         (is (= (parse "building (room, device)", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props nil
                   :preds nil
                   :then nil
                   :nest [{:what ru.petrsu.nest.son.Room
                           :props nil
                           :preds nil
                           :then nil
                           :nest nil}
                          {:what ru.petrsu.nest.son.Device
                           :props nil
                           :preds nil
                           :then nil
                           :nest nil}]}]))


         (is (= (parse "building (room, occupancy (device (network (floor)), networkinterface))", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props nil
                   :preds nil
                   :then nil
                   :nest [{:what ru.petrsu.nest.son.Room
                           :props nil
                           :preds nil
                           :then nil
                           :nest nil}
                          {:what ru.petrsu.nest.son.Occupancy
                           :props nil
                           :preds nil
                           :then nil
                           :nest [{:what ru.petrsu.nest.son.Device
                                   :props nil
                                   :preds nil
                                   :then nil
                                   :nest [{:what ru.petrsu.nest.son.Network
                                           :props nil
                                           :preds nil
                                           :then nil
                                           :nest [{:what ru.petrsu.nest.son.Floor
                                                   :props nil
                                                   :preds nil
                                                   :then nil
                                                   :nest nil}]}]}
                                  {:what ru.petrsu.nest.son.NetworkInterface
                                   :props nil
                                   :preds nil
                                   :then nil
                                   :nest nil}]}]}]))


         (is (= (parse "building, room", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props nil
                   :preds nil
                   :then nil
                   :nest nil}
                  {:what ru.petrsu.nest.son.Room 
                   :props nil
                   :preds nil
                   :then nil
                   :nest nil}])))
         

(deftest t-parse-props
         ^{:doc "Tests parsing queries with properties."}
         (is (= (parse "building.name", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props "name"
                   :preds nil
                   :then nil
                   :nest nil}]))


         (is (= (parse "building.room.floor.rooms", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props nil
                   :preds nil
                   :then {:what ru.petrsu.nest.son.Room 
                          :props nil 
                          :preds nil 
                          :then {:what ru.petrsu.nest.son.Floor 
                                 :props "rooms" 
                                 :preds nil 
                                 :then nil}}
                   :nest nil}]))


         (is (= (parse "building.room.number", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props nil
                   :preds nil
                   :then {:what ru.petrsu.nest.son.Room 
                          :props "number" 
                          :preds nil 
                          :then nil}
                   :nest nil}])))


(defmacro create-is [q mom] `(is (nil? (:remainder (parse+ ~q ~mom)))))

(deftest t-parse-remainder
         ^{:doc "Denerates is for each query from qlist. 
                Like 'parse-remainder, but it can show remainder.'"}
         (dotimes [n (count qlist)]
           (create-is (qlist n) mom)))
