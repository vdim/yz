(ns ru.petrsu.nest.yz.test-parsing
  ^{:author Vyacheslav Dimitrov
    :doc "Tests for parsing functions."}
  (:use ru.petrsu.nest.yz.parsing 
        ru.petrsu.nest.yz.hb-utils 
        clojure.contrib.test-is)
  (:import (ru.petrsu.nest.son Building Room Floor)))

(def mom 
  ^{:doc "Defines the map of the object model (used Nest's model)"}
  (gen-mom-from-cfg "/home/adim/tsen/clj/libs/yz/test/etc/hibernate.cfg.xml"))

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
                   :pred nil
                   :then nil
                   :nest nil}]))

         (is (= (parse "building (room)", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props nil
                   :pred nil
                   :then nil
                   :nest [{:what ru.petrsu.nest.son.Room
                           :props nil
                           :pred nil
                           :then nil
                           :nest nil}]}]))


         (is (= (parse "building (room (device))", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props nil
                   :pred nil
                   :then nil
                   :nest [{:what ru.petrsu.nest.son.Room
                           :props nil
                           :pred nil
                           :then nil
                           :nest [{:what ru.petrsu.nest.son.Device
                                   :props nil
                                   :pred nil
                                   :then nil
                                   :nest nil}]}]}]))


         (is (= (parse "building (room (device (network)))", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props nil
                   :pred nil
                   :then nil
                   :nest [{:what ru.petrsu.nest.son.Room
                           :props nil
                           :pred nil
                           :then nil
                           :nest [{:what ru.petrsu.nest.son.Device
                                   :props nil
                                   :pred nil
                                   :then nil
                                   :nest [{:what ru.petrsu.nest.son.Network
                                           :props nil
                                           :pred nil
                                           :then nil
                                           :nest nil}]}]}]}]))


         (is (= (parse "building (room (device (network (floor))))", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props nil
                   :pred nil
                   :then nil
                   :nest [{:what ru.petrsu.nest.son.Room
                           :props nil
                           :pred nil
                           :then nil
                           :nest [{:what ru.petrsu.nest.son.Device
                                   :props nil
                                   :pred nil
                                   :then nil
                                   :nest [{:what ru.petrsu.nest.son.Network
                                           :props nil
                                           :pred nil
                                           :then nil
                                           :nest [{:what ru.petrsu.nest.son.Floor
                                                   :props nil
                                                   :pred nil
                                                   :then nil
                                                   :nest nil}]}]}]}]}]))

         
         (is (= (parse "building (room (device, floor), network)", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props nil
                   :pred nil
                   :then nil
                   :nest [{:what ru.petrsu.nest.son.Room
                           :props nil
                           :pred nil
                           :then nil
                           :nest [{:what ru.petrsu.nest.son.Device
                                   :props nil
                                   :pred nil
                                   :then nil
                                   :nest nil}
                                  {:what ru.petrsu.nest.son.Floor
                                   :props nil
                                   :pred nil
                                   :then nil
                                   :nest nil}]} 
                          {:what ru.petrsu.nest.son.Network
                           :props nil
                           :pred nil
                           :then nil
                           :nest nil}]}]))


         (is (= (parse "building (room, device)", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props nil
                   :pred nil
                   :then nil
                   :nest [{:what ru.petrsu.nest.son.Room
                           :props nil
                           :pred nil
                           :then nil
                           :nest nil}
                          {:what ru.petrsu.nest.son.Device
                           :props nil
                           :pred nil
                           :then nil
                           :nest nil}]}]))


         (is (= (parse "building (room, occupancy (device (network (floor)), networkinterface))", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props nil
                   :pred nil
                   :then nil
                   :nest [{:what ru.petrsu.nest.son.Room
                           :props nil
                           :pred nil
                           :then nil
                           :nest nil}
                          {:what ru.petrsu.nest.son.Occupancy
                           :props nil
                           :pred nil
                           :then nil
                           :nest [{:what ru.petrsu.nest.son.Device
                                   :props nil
                                   :pred nil
                                   :then nil
                                   :nest [{:what ru.petrsu.nest.son.Network
                                           :props nil
                                           :pred nil
                                           :then nil
                                           :nest [{:what ru.petrsu.nest.son.Floor
                                                   :props nil
                                                   :pred nil
                                                   :then nil
                                                   :nest nil}]}]}
                                  {:what ru.petrsu.nest.son.NetworkInterface
                                   :props nil
                                   :pred nil
                                   :then nil
                                   :nest nil}]}]}]))


         (is (= (parse "building, room", mom)
                 [{:what ru.petrsu.nest.son.Building 
                   :props nil
                   :pred nil
                   :then nil
                   :nest nil}
                  {:what ru.petrsu.nest.son.Room 
                   :props nil
                   :pred nil
                   :then nil
                   :nest nil}]))) 
