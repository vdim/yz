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
  (:import (ru.petrsu.nest.son Building Room Floor)
           (ru.petrsu.nest.yz SyntaxException)))

(def mom- 
  ^{:doc "Defines the map of the object model (used Nest's model)"}
  (mom-from-file "nest.mom"))

(def some-v
  ^{:doc "Defines vector with single empty map."}
  [{:what nil
   :then nil
   :nest nil}])

(defn- dis-props-sort
  "Dissociate props (sort) from specified m in case props (sort) is empty (nil)."
  [props s m]
  (let [m (if (empty? props) (dissoc m :props) m)]
    (if (nil? s) (dissoc m :sort) m)))

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
                 [{:what ru.petrsu.nest.son.Building }]))


         (is (= (parse "building (room)", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :nest [{:what ru.petrsu.nest.son.Room
                           :where [["floors" "rooms"]]}]}]))


         (is (= (parse "building (room (device))", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :nest [{:what ru.petrsu.nest.son.Room
                           :where [["floors" "rooms"]]
                           :nest [{:what ru.petrsu.nest.son.Device
                                   :where [["occupancies" "devices"]]}]}]}]))


         (is (= (parse "building (room (device (network)))", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :nest [{:what ru.petrsu.nest.son.Room
                           :where [["floors" "rooms"]] 
                           :nest [{:what ru.petrsu.nest.son.Device
                                   :where [["occupancies" "devices"]]
                                   :nest [{:what ru.petrsu.nest.son.Network
                                           :where [["linkInterfaces" "networkInterfaces" "network"]]}]}]}]}]))


         (is (= (parse "building (room (device (network (floor))))", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :nest [{:what ru.petrsu.nest.son.Room
                           :where [["floors" "rooms"]]
                           :nest [{:what ru.petrsu.nest.son.Device
                                   :where [["occupancies" "devices"]] 
                                   :nest [{:what ru.petrsu.nest.son.Network
                                           :where [["linkInterfaces" "networkInterfaces" "network"]]
                                           :nest [{:what ru.petrsu.nest.son.Floor
                                                   :where [["networkInterfaces" 
                                                            "linkInterface" 
                                                            "device" 
                                                            "occupancy" 
                                                            "room" "floor"]]}]}]}]}]}]))

         
         (is (= (parse "building (room (device, floor), network)", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :nest [{:what ru.petrsu.nest.son.Room
                           :where [["floors" "rooms"]]
                           :nest [{:what ru.petrsu.nest.son.Device
                                   :where [["occupancies" "devices"]]}
                                  {:what ru.petrsu.nest.son.Floor
                                   :where [["floor"]]}]} 
                          {:what ru.petrsu.nest.son.Network
                           :where [["floors" "rooms" "occupancies" "devices" "linkInterfaces" "networkInterfaces" "network"]]}]}]))


         (is (= (parse "building (room, device)", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :nest [{:what ru.petrsu.nest.son.Room
                           :where [["floors" "rooms"]]}
                          {:what ru.petrsu.nest.son.Device
                           :where [["floors" "rooms" "occupancies" "devices"]]}]}]))

         (is (= (parse "building (room, occupancy (device (network (floor)), networkinterface))", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :nest [{:what ru.petrsu.nest.son.Room
                           :where [["floors", "rooms"]]}
                          {:what ru.petrsu.nest.son.Occupancy
                           :where [["floors" "rooms" "occupancies"]]
                           :nest [{:what ru.petrsu.nest.son.Device
                                   :where [["devices"]]
                                   :nest [{:what ru.petrsu.nest.son.Network
                                           :where [["linkInterfaces" "networkInterfaces" "network"]]
                                           :nest [{:what ru.petrsu.nest.son.Floor
                                                   :where [["networkInterfaces" 
                                                            "linkInterface" 
                                                            "device" 
                                                            "occupancy" 
                                                            "room" "floor"]]}]}]}
                                  {:what ru.petrsu.nest.son.NetworkInterface
                                   :where [["devices" "linkInterfaces" "networkInterfaces"]]}]}]}]))

         (is (= (parse "building, room", mom-)
                 [{:what ru.petrsu.nest.son.Building}
                  {:what ru.petrsu.nest.son.Room}]))))

(deftest t-parse-props
         ^{:doc "Tests parsing queries with properties."}
         (let [mom- (sort-to-nil mom-)]
         (is (= (parse "building.name", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :props [[:name false]]}]))


         (is (= (parse "building.room.floor.rooms", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :then {:what ru.petrsu.nest.son.Room 
                          :where [["floors" "rooms"]]
                          :then {:what ru.petrsu.nest.son.Floor 
                                 :where [["floor"]] 
                                 :props [[:rooms false]]}}}]))


         (is (= (parse "building.floor.room.occupancy.device.forwarding", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :then {:what ru.petrsu.nest.son.Floor 
                          :where [["floors"]] 
                          :then {:what ru.petrsu.nest.son.Room
                                 :where [["rooms"]]
                                 :then {:what ru.petrsu.nest.son.Occupancy
                                        :where [["occupancies"]]
                                        :then {:what ru.petrsu.nest.son.Device
                                               :props [[:forwarding false]]  
                                               :where [["devices"]]}}}}}]))


         (is (= (parse "simpleou[*parent]", mom-)
                 [{:what ru.petrsu.nest.son.SimpleOU 
                   :props [[:parent true]]}]))


         (is (= (parse "compositeou[*parent OUs]", mom-)
                 [{:what ru.petrsu.nest.son.CompositeOU 
                   :props [[:parent true] [:OUs false]]}]))


         (is (= (parse "compositeou[OUs *parent]", mom-)
                 [{:what ru.petrsu.nest.son.CompositeOU 
                   :props [[:OUs false] [:parent true]]}]))


         (is (= (parse "building.room.number", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :then {:what ru.petrsu.nest.son.Room 
                          :props [[:number false]] 
                          :where [["floors" "rooms"]]}}]))))


(deftest t-parse-predicates
         ^{:doc "Tests parsing queries with some restrictions."}
         (let [mom- (sort-to-nil mom-)]
          (is (= (parse "building#(name=1)", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :preds [{:ids [{:id ["name"] :cl nil}], :func #'clojure.core/=, :value 1}]}]))
          (is (= (parse "building#(room.number=\"215\")", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :preds [{:ids [{:id ["floors" "rooms"] :cl Room} 
                                  {:id ["number"] :cl nil}], :func #'clojure.core/=, :value "215"}]}]))
          (is (= (parse "building#(name=1 and address=2)", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :preds [{:ids [{:id ["name"] :cl nil}], :func #'clojure.core/=, :value 1} 
                           {:ids [{:id ["address"] :cl nil}], :func #'clojure.core/=, :value 2} :and]}]))
          (is (= (parse "building#(name=1 or address=2)", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :preds [{:ids [{:id ["name"] :cl nil}], :func #'clojure.core/=, :value 1} 
                           {:ids [{:id ["address"] :cl nil}], :func #'clojure.core/=, :value 2} :or]}]))
          (is (= (parse "building#(name=1 and address=2 and floor.number=3)", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :preds [{:ids [{:id ["name"], :cl nil}], :func #'clojure.core/=, :value 1} 
                           {:ids [{:id ["address"], :cl nil}], :func #'clojure.core/=, :value 2} 
                           :and {:ids [{:id ["floors"] :cl Floor}, {:id ["number"] :cl nil}], :func #'clojure.core/=, :value 3} :and]}]))
          (is (= (parse "building#(name=1 and address=2 or floor.number=3)", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :preds [{:ids [{:id ["name"], :cl nil}], :func #'clojure.core/=, :value 1} 
                           {:ids [{:id ["address"], :cl nil}], :func #'clojure.core/=, :value 2} 
                           :and {:ids [{:id ["floors"] :cl Floor}, {:id ["number"] :cl nil}], :func #'clojure.core/=, :value 3} :or]}]))
          (is (= (parse "building#(name=1 or address=2 and floor.number=3)", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :preds [{:ids [{:id ["name"], :cl nil}], :func #'clojure.core/=, :value 1} 
                           {:ids [{:id ["address"], :cl nil}], :func #'clojure.core/=, :value 2} 
                           {:ids [{:id ["floors"] :cl Floor}, {:id ["number"] :cl nil}], :func #'clojure.core/=, :value 3} :and :or]}]))
          (is (= (parse "building#(name=1 and (address=2 or floor.number=3))", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :preds [{:ids [{:id ["name"], :cl nil}], :func #'clojure.core/=, :value 1} 
                           {:ids [{:id ["address"], :cl nil}], :func #'clojure.core/=, :value 2} 
                           {:ids [{:id ["floors"] :cl Floor}, {:id ["number"] :cl nil}], :func #'clojure.core/=, :value 3} :or :and]}]))))


(deftest t-parse-sorting
         ^{:doc "Tests a value of the :sort key."}
         (let [mom- (sort-to-nil mom-)
               f #(= (parse %1 mom-)
                     (let [fnest {:what Building
                                  :props %2
                                  :sort %3}
                           fnest (dis-props-sort %2 %3 fnest)]
                       [fnest]))]
           (is (f "building[name]" [[:name false]] nil))
           (is (f "↑building" [] [:asc nil nil]))
           (is (f "↓building" [] [:desc nil nil] ))
           (is (f "↓building[name]" [[:name false]] [[:desc nil nil] [nil nil nil]]))
           (is (f "building[↓name]" [[:name false]] [[nil nil nil] [:desc nil nil]]))
           (is (f "↓building[↓name]" [[:name false]] [[:desc nil nil] [:desc nil nil]]))
           (is (f "↑building[name]" [[:name false]] [[:asc nil nil] [nil nil nil]]))
           (is (f "building[↑name]" [[:name false]] [[nil nil nil] [:asc nil nil]]))
           (is (f "↑building[↑name]" [[:name false]] [[:asc nil nil] [:asc nil nil]]))
           (is (f "building[name description]" [[:name false] [:description false]] nil))
           (is (f "↓building[name description]" 
                  [[:name false] [:description false]] 
                  [[:desc nil nil] [nil nil nil] [nil nil nil]]))
           (is (f "↓building[↓name description]" 
                  [[:name false] [:description false]] 
                  [[:desc nil nil] [:desc nil nil] [nil nil nil]]))
           (is (f "↓building[↓name ↓description]" 
                  [[:name false] [:description false]] 
                  [[:desc nil nil] [:desc nil nil] [:desc nil nil]]))
           (is (f "↓building[name ↓description]" 
                  [[:name false] [:description false]] 
                  [[:desc nil nil] [nil nil nil] [:desc nil nil]]))
           (is (f "building[↓name ↓description]" 
                  [[:name false] [:description false]] 
                  [[nil nil nil] [:desc nil nil] [:desc nil nil]]))
           (is (f "building[name ↓description]" 
                  [[:name false] [:description false]] 
                  [[nil nil nil] [nil nil nil] [:desc nil nil]]))
           (let [f #(= (parse %1 mom-)
                     (let [lnest {:what ru.petrsu.nest.son.Device
                                  :props %2
                                  :sort %3
                                  :where [["occupancies" "devices"]]}
                           lnest (dis-props-sort %2 %3 lnest)]
                       [{:what ru.petrsu.nest.son.Building 
                         :nest [{:what ru.petrsu.nest.son.Room
                                 :where [["floors" "rooms"]]
                                 :nest [lnest]}]}]))]
             (is (f "building (room (↓device))" 
                    []
                    [:desc nil nil]))
             (is (f "building (room (↓device[name]))" 
                    [[:name false]]
                    [[:desc nil nil] [nil nil nil]]))
             (is (f "building (room (↓device[↓name]))" 
                    [[:name false]]
                    [[:desc nil nil] [:desc nil nil]]))
             (is (f "building (room (device[↓name]))" 
                    [[:name false]]
                    [[nil nil nil] [:desc nil nil]]))
             (is (f "building (room (device[description ↓name]))" 
                    [[:description false] [:name false]]
                    [[nil nil nil] [nil nil nil] [:desc nil nil]]))
             (is (f "building (room (device[↓description ↓name]))" 
                    [[:description false] [:name false]]
                    [[nil nil nil] [:desc nil nil] [:desc nil nil]]))
             (is (f "building (room (↓device[↓description ↓name]))" 
                    [[:description false] [:name false]]
                    [[:desc nil nil] [:desc nil nil] [:desc nil nil]]))
             (is (f "building (room (↓device[↓description name]))" 
                    [[:description false] [:name false]]
                    [[:desc nil nil] [:desc nil nil] [nil nil nil]]))
             (is (f "building (room (↓device[description name]))" 
                    [[:description false] [:name false]]
                    [[:desc nil nil] [nil nil nil] [nil nil nil]])))
           (let [f #(= (parse %1 mom-)
                       (let [lnest {:what ru.petrsu.nest.son.Device
                                    :props %2
                                    :sort %3
                                    :where [["occupancies" "devices"]]}
                             lnest (dis-props-sort %2 %3 lnest)]
                         [{:what ru.petrsu.nest.son.Room
                           :nest [lnest]}]))]
             (is (f "room (↓device)" 
                    []
                    [:desc nil nil]))
             (is (f "room (↓device[name])" 
                    [[:name false]]
                    [[:desc nil nil] [nil nil nil]]))
             (is (f "room (↓device[↓name])" 
                    [[:name false]]
                    [[:desc nil nil] [:desc nil nil]]))
             (is (f "room (device[↓name])" 
                    [[:name false]]
                    [[nil nil nil] [:desc nil nil]]))
             (is (f "room (device[description ↓name])" 
                    [[:description false] [:name false]]
                    [[nil nil nil] [nil nil nil] [:desc nil nil]]))
             (is (f "room (device[↓description ↓name])" 
                    [[:description false] [:name false]]
                    [[nil nil nil] [:desc nil nil] [:desc nil nil]]))
             (is (f "room (↓device[↓description ↓name])" 
                    [[:description false] [:name false]]
                    [[:desc nil nil] [:desc nil nil] [:desc nil nil]]))
             (is (f "room (↓device[↓description name])" 
                    [[:description false] [:name false]]
                    [[:desc nil nil] [:desc nil nil] [nil nil nil]]))
             (is (f "room (↓device[description name])" 
                    [[:description false] [:name false]]
                    [[:desc nil nil] [nil nil nil] [nil nil nil]])))
         (is (= (parse "↓building.floor.room.occupancy.device.forwarding", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :sort [:desc nil nil]
                   :then {:what ru.petrsu.nest.son.Floor 
                          :where [["floors"]] 
                          :then {:what ru.petrsu.nest.son.Room
                                 :where [["rooms"]]
                                 :then {:what ru.petrsu.nest.son.Occupancy
                                        :where [["occupancies"]]
                                        :then {:what ru.petrsu.nest.son.Device
                                               :props [[:forwarding false]]  
                                               :where [["devices"]]}}}}}]))
         (is (= (parse "↓building[name].floor.room.occupancy.device.forwarding", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :props [[:name false]] 
                   :sort [[:desc nil nil] [nil nil nil]]
                   :then {:what ru.petrsu.nest.son.Floor 
                          :where [["floors"]] 
                          :then {:what ru.petrsu.nest.son.Room
                                 :where [["rooms"]]
                                 :then {:what ru.petrsu.nest.son.Occupancy
                                        :where [["occupancies"]]
                                        :then {:what ru.petrsu.nest.son.Device
                                               :props [[:forwarding false]]  
                                               :where [["devices"]]}}}}}]))
           (let [f #(= (parse %1 mom-)
                       (let [sthen {:what ru.petrsu.nest.son.Room
                                    :where [["rooms"]]
                                    :then {:what ru.petrsu.nest.son.Occupancy
                                            :where [["occupancies"]]
                                            :then {:what ru.petrsu.nest.son.Device
                                                   :props [[:forwarding false]]  
                                                  :where [["devices"]]}}}
                             fthen {:what ru.petrsu.nest.son.Floor 
                                    :props %2
                                    :where [["floors"]] 
                                    :sort %3
                                    :then sthen}
                             fthen (dis-props-sort %2 %3 fthen)]
                         [{:what ru.petrsu.nest.son.Building 
                           :then fthen}]))]
             (is (f "building.↓floor.room.occupancy.device.forwarding" 
                    []
                    [:desc nil nil]))
             (is (f "building.floor[number].room.occupancy.device.forwarding" 
                    [[:number false]]
                    nil))
             (is (f "building.↓floor[number].room.occupancy.device.forwarding" 
                    [[:number false]]
                    [[:desc nil nil] [nil nil nil]]))
             (is (f "building.↓floor[↓number].room.occupancy.device.forwarding" 
                    [[:number false]]
                    [[:desc nil nil] [:desc nil nil]]))
             (is (f "building.floor[↓number].room.occupancy.device.forwarding" 
                    [[:number false]]
                    [[nil nil nil] [:desc nil nil]]))
             (is (f "building.floor[description number].room.occupancy.device.forwarding" 
                    [[:description false] [:number false]]
                    nil))
             (is (f "building.floor[description ↓number].room.occupancy.device.forwarding" 
                    [[:description false] [:number false]]
                    [[nil nil nil] [nil nil nil] [:desc nil nil]]))
             (is (f "building.floor[↓description ↓number].room.occupancy.device.forwarding" 
                    [[:description false] [:number false]]
                    [[nil nil nil] [:desc nil nil] [:desc nil nil]]))
             (is (f "building.↓floor[↓description ↓number].room.occupancy.device.forwarding" 
                    [[:description false] [:number false]]
                    [[:desc nil nil] [:desc nil nil] [:desc nil nil]]))
             (is (f "building.↓floor[↓description number].room.occupancy.device.forwarding" 
                    [[:description false] [:number false]]
                    [[:desc nil nil] [:desc nil nil] [nil nil nil]]))
             (is (f "building.↓floor[description number].room.occupancy.device.forwarding" 
                    [[:description false] [:number false]]
                    [[:desc nil nil] [nil nil nil] [nil nil nil]])))
         (is (= (parse "↓building[name].floor[↓description number].room.occupancy.device.forwarding", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :props [[:name false]] 
                   :sort [[:desc nil nil] [nil nil nil]]
                   :then {:what ru.petrsu.nest.son.Floor 
                          :props [[:description false] [:number false]]
                          :where [["floors"]] 
                          :sort [[nil nil nil] [:desc nil nil] [nil nil nil]]
                          :then {:what ru.petrsu.nest.son.Room
                                 :where [["rooms"]]
                                 :then {:what ru.petrsu.nest.son.Occupancy
                                        :where [["occupancies"]]
                                        :then {:what ru.petrsu.nest.son.Device
                                               :props [[:forwarding false]]  
                                               :where [["devices"]]}}}}}]))
         (is (= (parse "↓building[name].floor[description number].room.occupancy.device.forwarding", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :props [[:name false]] 
                   :sort [[:desc nil nil] [nil nil nil]]
                   :then {:what ru.petrsu.nest.son.Floor 
                          :props [[:description false] [:number false]]
                          :where [["floors"]] 
                          :then {:what ru.petrsu.nest.son.Room
                                 :where [["rooms"]]
                                 :then {:what ru.petrsu.nest.son.Occupancy
                                        :where [["occupancies"]]
                                        :then {:what ru.petrsu.nest.son.Device
                                               :props [[:forwarding false]]  
                                               :where [["devices"]]}}}}}]))
           (let [f #(= (parse %1 mom-)
                       (let [sthen {:what ru.petrsu.nest.son.Room
                                    :props %2
                                    :sort %3
                                    :where [["rooms"]]
                                    :then {:what ru.petrsu.nest.son.Occupancy
                                            :where [["occupancies"]]
                                            :then {:what ru.petrsu.nest.son.Device
                                                   :props [[:forwarding false]]  
                                                   :where [["devices"]]}}}
                             sthen (dis-props-sort %2 %3 sthen)]
                         [{:what ru.petrsu.nest.son.Building 
                           :then {:what ru.petrsu.nest.son.Floor 
                                  :where [["floors"]] 
                                  :then sthen}}]))]
             (is (f "building.floor.↓room.occupancy.device.forwarding" 
                    []
                    [:desc nil nil]))
             (is (f "building.floor.room[number].occupancy.device.forwarding" 
                    [[:number false]]
                    nil))
             (is (f "building.floor.↓room[number].occupancy.device.forwarding" 
                    [[:number false]]
                    [[:desc nil nil] [nil nil nil]]))
             (is (f "building.floor.↓room[↓number].occupancy.device.forwarding" 
                    [[:number false]]
                    [[:desc nil nil] [:desc nil nil]]))
             (is (f "building.floor.room[↓number].occupancy.device.forwarding" 
                    [[:number false]]
                    [[nil nil nil] [:desc nil nil]]))
             (is (f "building.floor.room[description number].occupancy.device.forwarding" 
                    [[:description false] [:number false]]
                    nil))
             (is (f "building.floor.room[description ↓number].occupancy.device.forwarding" 
                    [[:description false] [:number false]]
                    [[nil nil nil] [nil nil nil] [:desc nil nil]]))
             (is (f "building.floor.room[↓description ↓number].occupancy.device.forwarding" 
                    [[:description false] [:number false]]
                    [[nil nil nil] [:desc nil nil] [:desc nil nil]]))
             (is (f "building.floor.↓room[↓description ↓number].occupancy.device.forwarding" 
                    [[:description false] [:number false]]
                    [[:desc nil nil] [:desc nil nil] [:desc nil nil]]))
             (is (f "building.floor.↓room[↓description number].occupancy.device.forwarding" 
                    [[:description false] [:number false]]
                    [[:desc nil nil] [:desc nil nil] [nil nil nil]]))
             (is (f "building.floor.↓room[description number].occupancy.device.forwarding" 
                    [[:description false] [:number false]]
                    [[:desc nil nil] [nil nil nil] [nil nil nil]])))
         (is (= (parse "building.floor.room.occupancy.↓device.forwarding", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :then {:what ru.petrsu.nest.son.Floor 
                          :where [["floors"]] 
                          :then {:what ru.petrsu.nest.son.Room
                                 :where [["rooms"]]
                                 :then {:what ru.petrsu.nest.son.Occupancy
                                        :where [["occupancies"]]
                                        :then {:what ru.petrsu.nest.son.Device
                                               :props [[:forwarding false]]  
                                               :sort [[:desc nil nil] [nil nil nil]]
                                               :where [["devices"]]}}}}}]))
         (is (= (parse "building.floor.room.occupancy.device.↓forwarding", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :then {:what ru.petrsu.nest.son.Floor 
                          :where [["floors"]] 
                          :then {:what ru.petrsu.nest.son.Room
                                 :where [["rooms"]]
                                 :then {:what ru.petrsu.nest.son.Occupancy
                                        :where [["occupancies"]]
                                        :then {:what ru.petrsu.nest.son.Device
                                               :props [[:forwarding false]]  
                                               :sort [[nil nil nil] [:desc nil nil]]
                                               :where [["devices"]]}}}}}]))
         (is (= (parse "building.floor.room.occupancy.↓device.↓forwarding", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :then {:what ru.petrsu.nest.son.Floor 
                          :where [["floors"]] 
                          :then {:what ru.petrsu.nest.son.Room
                                 :where [["rooms"]]
                                 :then {:what ru.petrsu.nest.son.Occupancy
                                        :where [["occupancies"]]
                                        :then {:what ru.petrsu.nest.son.Device
                                               :props [[:forwarding false]]  
                                               :sort [[:desc nil nil] [:desc nil nil]]
                                               :where [["devices"]]}}}}}]))))


(deftest t-parse-sorting-pwns
         ^{:doc "Tests queries with sorting by properties which are not selected."}
       (let [mom- (sort-to-nil mom-)]
         (let [f #(= (parse %1 mom-)
                     (let [fnest {:what Building
                                  :props %2
                                  :sort %3}
                           fnest (dis-props-sort %2 %3 fnest)]
                       [fnest]))]
           (is (f "building" [] nil))
           (is (f "{a:name}building" [] ['(:name [:asc nil nil])]))
           (is (f "{d:name}building" [] ['(:name [:desc nil nil])]))
           (is (f "{a:name a:description}building" [] ['(:description [:asc nil nil]) '(:name [:asc nil nil])]))
           (is (f "{a:name d:description}building" [] ['(:description [:desc nil nil]) '(:name [:asc nil nil])]))
           (is (f "{a:description a:name}building" [] ['(:name [:asc nil nil]) '(:description [:asc nil nil])]))
           (is (f "{d:description a:name}building" [] ['(:name [:asc nil nil]) '(:description [:desc nil nil])]))
           (is (f "{a:description d:name}building" [] ['(:name [:desc nil nil]) '(:description [:asc nil nil])]))
           (is (f "{d:description d:name}building" [] ['(:name [:desc nil nil]) '(:description [:desc nil nil])]))
           (is (f "{a:name d:description d:address}building" [] 
                  ['(:address [:desc nil nil]) '(:description [:desc nil nil]) '(:name [:asc nil nil])]))
           (is (f "{a:name d:description a:address}building" [] 
                  ['(:address [:asc nil nil]) '(:description [:desc nil nil]) '(:name [:asc nil nil])]))
           (is (f "{a:name}building[name]" [[:name false]] ['(:name [:asc nil nil])]))
           (is (f "{a:name}building[a:name]" [[:name false]] ['(:name [:asc nil nil])]))
           (is (f "{a:name}building[a:name d:address]" 
                  [[:name false] [:address false]] ['(:name [:asc nil nil])]))
           (is (f "{a:name}building[name d:address]" 
                  [[:name false] [:address false]] ['(:name [:asc nil nil])]))
           (is (f "{a:name}building[a:name address]" 
                  [[:name false] [:address false]] ['(:name [:asc nil nil])]))
           (is (f "{a:name}building[name address]" 
                  [[:name false] [:address false]] ['(:name [:asc nil nil])]))
           (is (f "{a:name}building" [] ['(:name [:asc nil nil])])))
         (let [f #(= (parse %1 mom-)
                     (let [lnest {:what Room
                                  :props %2
                                  :sort %3
                                  :where [["floors" "rooms"]]}
                           lnest (dis-props-sort %2 %3 lnest)]
                       [{:what Building
                         :nest [lnest]}]))]
           (is (f "building (room)" [] nil))
           (is (f "building ({a:number}room)" [] ['(:number [:asc nil nil])]))
           (is (f "building ({a:number d:name}room)" [] ['(:name [:desc nil nil]) '(:number [:asc nil nil])]))
           (is (f "building ({a:number d:name}room[d:description])" [[:description false]] 
                  ['(:name [:desc nil nil]) '(:number [:asc nil nil])])))
         (let [f #(= (parse %1 mom-)
                     (let [lnest {:what Room
                                  :props %4
                                  :sort %5
                                  :where [["floors" "rooms"]]}
                           lnest (dis-props-sort %4 %5 lnest)
                           fnest {:what Building
                                  :props %2
                                  :sort %3
                                  :nest [lnest]}
                           fnest (dis-props-sort %2 %3 fnest)]
                       [fnest]))]
           (is (f "{a:name}building (room)" 
                  [] ['(:name [:asc nil nil])] 
                  [] nil))
           (is (f "{a:name}building[name] (room)" 
                  [[:name false]] ['(:name [:asc nil nil])] 
                  [] nil))
           (is (f "{a:name}building[name] (room[name])" 
                  [[:name false]] ['(:name [:asc nil nil])] 
                  [[:name false]] nil))
           (is (f "{a:name}building[a:name] (room)" 
                  [[:name false]] ['(:name [:asc nil nil])] 
                  [] nil))
           (is (f "{a:name}building[a:name] (room[d:name])" 
                  [[:name false]] ['(:name [:asc nil nil])] 
                  [[:name false]] [[nil nil nil] [:desc nil nil]]))
           (is (f "{a:name}building ({a:number}room)" 
                  [] ['(:name [:asc nil nil])] 
                  [] ['(:number [:asc nil nil])]))
           (is (f "{a:name}building[name description] ({a:number}room)" 
                  [[:name false] [:description false]] ['(:name [:asc nil nil])] 
                  [] ['(:number [:asc nil nil])]))
           (is (f "{a:name}building[name description] ({a:number}room[name number])" 
                  [[:name false] [:description false]] ['(:name [:asc nil nil])] 
                  [[:name false] [:number false]] ['(:number [:asc nil nil])]))
           (is (f "{a:address a:name}building ({a:number d:name}room)" 
                  [] ['(:name [:asc nil nil]) '(:address [:asc nil nil])]
                  [] ['(:name [:desc nil nil]) '(:number [:asc nil nil])]))
           (is (f "{a:name}building ({a:number d:name}room[d:description])" 
                  [] ['(:name [:asc nil nil])]
                  [[:description false]] ['(:name [:desc nil nil]) '(:number [:asc nil nil])])))
         (let [f #(= (parse %1 mom-)
                     (let [lnest {:what Room
                                  :props %4
                                  :sort %5
                                  :where [["floors" "rooms"]]}
                           lnest (dis-props-sort %4 %5 lnest)
                           fnest {:what Building
                                  :props %2
                                  :sort %3
                                  :then lnest}
                           fnest (dis-props-sort %2 %3 fnest)]
                       [fnest]))]
           (is (f "{a:name}building.room" 
                  [] ['(:name [:asc nil nil])] 
                  [] nil))
           (is (f "{a:name}building[name].room" 
                  [[:name false]] ['(:name [:asc nil nil])] 
                  [] nil))
           (is (f "{a:name}building.{d:number}room" 
                  [] ['(:name [:asc nil nil])] 
                  [] ['(:number [:desc nil nil])]))
           (is (f "{a:name}building[description].{d:number}room" 
                  [[:description false]] ['(:name [:asc nil nil])] 
                  [] ['(:number [:desc nil nil])]))
           (is (f "{a:name}building[a:description].{d:number}room" 
                  [[:description false]] ['(:name [:asc nil nil])] 
                  [] ['(:number [:desc nil nil])]))
           (is (f "{a:name}building[description].{d:number}room[name]" 
                  [[:description false]] ['(:name [:asc nil nil])] 
                  [[:name false]] ['(:number [:desc nil nil])]))
           (is (f "{a:name}building[description].{d:number}room[a:name]" 
                  [[:description false]] ['(:name [:asc nil nil])] 
                  [[:name false]] ['(:number [:desc nil nil])]))
           (is (f "{a:name}building[description].{d:number}room[a:name number]" 
                  [[:description false]] ['(:name [:asc nil nil])] 
                  [[:name false] [:number false]] ['(:number [:desc nil nil])]))
           (is (f "{a:name}building[description].{d:number}room[a:name d:number]" 
                  [[:description false]] ['(:name [:asc nil nil])] 
                  [[:name false] [:number false]] ['(:number [:desc nil nil])]))
           (is (f "{a:name}building[description].{d:number}room[name d:number]"
                  [[:description false]] ['(:name [:asc nil nil])] 
                  [[:name false] [:number false]] ['(:number [:desc nil nil])]))
           (is (f "{a:name}building[description].{d:number}room[name number d:description]" 
                  [[:description false]] ['(:name [:asc nil nil])] 
                  [[:name false] [:number false] [:description false]] ['(:number [:desc nil nil])]))
           (is (f "{a:name}building[description].room[a:name]" 
                  [[:description false]] ['(:name [:asc nil nil])] 
                  [[:name false]] [[nil nil nil] [:asc nil nil]]))
           (is (f "{a:name}building[description].room[a:name description]" 
                  [[:description false]] ['(:name [:asc nil nil])] 
                  [[:name false] [:description false]] [[nil nil nil] [:asc nil nil] [nil nil nil]])))
         (let [f #(= (parse %1 mom-)
                     (let [lthen {:what Room
                                  :props %4
                                  :sort %5
                                  :where [["rooms"]]}
                           lthen (dis-props-sort %4 %5 lthen)
                           fthen {:what Floor
                                  :where [["floors"]]
                                  :then lthen}
                           fnest {:what Building
                                  :props %2
                                  :sort %3
                                  :then fthen}
                           fnest (dis-props-sort %2 %3 fnest)]
                       [fnest]))]
           (is (f "{a:name}building.floor.room" 
                  [] ['(:name [:asc nil nil])] 
                  [] nil))
           (is (f "{a:name}building[name].floor.room" 
                  [[:name false]] ['(:name [:asc nil nil])] 
                  [] nil))
           (is (f "{a:name}building.floor.{d:number}room" 
                  [] ['(:name [:asc nil nil])] 
                  [] ['(:number [:desc nil nil])]))
           (is (f "{a:name}building[description].floor.{d:number}room" 
                  [[:description false]] ['(:name [:asc nil nil])] 
                  [] ['(:number [:desc nil nil])]))
           (is (f "{a:name}building[a:description].floor.{d:number}room" 
                  [[:description false]] ['(:name [:asc nil nil])] 
                  [] ['(:number [:desc nil nil])]))
           (is (f "{a:name}building[description].floor.{d:number}room[name]" 
                  [[:description false]] ['(:name [:asc nil nil])] 
                  [[:name false]] ['(:number [:desc nil nil])]))
           (is (f "{a:name}building[description].floor.{d:number}room[a:name]" 
                  [[:description false]] ['(:name [:asc nil nil])] 
                  [[:name false]] ['(:number [:desc nil nil])]))
           (is (f "{a:name}building[description].floor.{d:number}room[a:name number]" 
                  [[:description false]] ['(:name [:asc nil nil])] 
                  [[:name false] [:number false]] ['(:number [:desc nil nil])]))
           (is (f "{a:name}building[description].floor.{d:number}room[a:name d:number]" 
                  [[:description false]] ['(:name [:asc nil nil])] 
                  [[:name false] [:number false]] ['(:number [:desc nil nil])]))
           (is (f "{a:name}building[description].floor.{d:number}room[name d:number]"
                  [[:description false]] ['(:name [:asc nil nil])] 
                  [[:name false] [:number false]] ['(:number [:desc nil nil])]))
           (is (f "{a:name}building[description].floor.{d:number}room[name number d:description]" 
                  [[:description false]] ['(:name [:asc nil nil])] 
                  [[:name false] [:number false] [:description false]] ['(:number [:desc nil nil])]))
           (is (f "{a:name}building[description].floor.room[a:name]" 
                  [[:description false]] ['(:name [:asc nil nil])] 
                  [[:name false]] [[nil nil nil] [:asc nil nil]]))
           (is (f "{a:name}building[description].floor.room[a:name description]" 
                  [[:description false]] ['(:name [:asc nil nil])] 
                  [[:name false] [:description false]] [[nil nil nil] [:asc nil nil] [nil nil nil]])))
         (let [f #(= (parse %1 mom-)
                     (let [lthen {:what Room
                                  :props %6
                                  :sort %7
                                  :where [["rooms"]]}
                           lthen (dis-props-sort %6 %7 lthen)
                           fthen {:what Floor
                                  :where [["floors"]]
                                  :props %4
                                  :sort %5
                                  :then lthen}
                           fthen (dis-props-sort %4 %5 fthen)
                           fnest {:what Building
                                  :props %2
                                  :sort %3
                                  :then fthen}
                           fnest (dis-props-sort %2 %3 fnest)]
                       [fnest]))]
           (is (f "{a:name}building.floor[a:number].room" 
                  [] ['(:name [:asc nil nil])] 
                  [[:number false]] [[nil nil nil] [:asc nil nil]]
                  [] nil))
           (is (f "{a:name}building.floor[number].{d:number}room" 
                  [] ['(:name [:asc nil nil])] 
                  [[:number false]] nil
                  [] ['(:number [:desc nil nil])]))
           (is (f "{a:name}building.floor[a:number].{d:number}room" 
                  [] ['(:name [:asc nil nil])] 
                  [[:number false]] [[nil nil nil] [:asc nil nil]]
                  [] ['(:number [:desc nil nil])]))
           (is (f "{a:name}building.{a:number}floor.{d:number}room" 
                  [] ['(:name [:asc nil nil])] 
                  [] ['(:number [:asc nil nil])]
                  [] ['(:number [:desc nil nil])]))
           (is (f "{a:name}building.{a:number d:description}floor.{d:number}room" 
                  [] ['(:name [:asc nil nil])] 
                  [] ['(:description [:desc nil nil]) '(:number [:asc nil nil])]
                  [] ['(:number [:desc nil nil])]))
           (is (f "{a:name}building.{a:number d:description}floor.room" 
                  [] ['(:name [:asc nil nil])] 
                  [] ['(:description [:desc nil nil]) '(:number [:asc nil nil])]
                  [] nil))
           (is (f "{a:name}building.{a:number d:description}floor[number].room" 
                  [] ['(:name [:asc nil nil])] 
                  [[:number false]] ['(:description [:desc nil nil]) '(:number [:asc nil nil])]
                  [] nil))
           (is (f "{a:name}building.{a:number d:description}floor[description number].room" 
                  [] ['(:name [:asc nil nil])] 
                  [[:description false] [:number false]] ['(:description [:desc nil nil]) '(:number [:asc nil nil])]
                  [] nil))
           (is (f "{a:name}building.{a:number d:description}floor[description number].{d:number}room" 
                  [] ['(:name [:asc nil nil])] 
                  [[:description false] [:number false]] ['(:description [:desc nil nil]) '(:number [:asc nil nil])]
                  [] ['(:number [:desc nil nil])]))
           (is (f "{a:name}building.{a:number d:description}floor[description number].room[a:name]" 
                  [] ['(:name [:asc nil nil])] 
                  [[:description false] [:number false]] ['(:description [:desc nil nil]) '(:number [:asc nil nil])]
                  [[:name false]] [[nil nil nil] [:asc nil nil]]))
           (is (f "{a:name}building.{a:number d:description}floor[description number].{d:number}room[a:name]" 
                  [] ['(:name [:asc nil nil])] 
                  [[:description false] [:number false]] ['(:description [:desc nil nil]) '(:number [:asc nil nil])]
                  [[:name false]] ['(:number [:desc nil nil])]))
           (is (f "{a:name}building[address].{a:number d:description}floor[description number].{d:number}room[a:name]" 
                  [[:address false]] ['(:name [:asc nil nil])] 
                  [[:description false] [:number false]] ['(:description [:desc nil nil]) '(:number [:asc nil nil])]
                  [[:name false]] ['(:number [:desc nil nil])]))
           (is (f "{a:name}building[address name].{a:number d:description}floor[description number].{d:number}room[a:name]" 
                  [[:address false] [:name false]] ['(:name [:asc nil nil])] 
                  [[:description false] [:number false]] ['(:description [:desc nil nil]) '(:number [:asc nil nil])]
                  [[:name false]] ['(:number [:desc nil nil])]))
           (is (f "{a:name}building[a:address name].{a:number d:description}floor[description number].{d:number}room[a:name]" 
                  [[:address false] [:name false]] ['(:name [:asc nil nil])] 
                  [[:description false] [:number false]] ['(:description [:desc nil nil]) '(:number [:asc nil nil])]
                  [[:name false]] ['(:number [:desc nil nil])]))
           (is (f "{a:name}building[address d:name].{a:number d:description}floor[description number].{d:number}room[a:name]" 
                  [[:address false] [:name false]] ['(:name [:asc nil nil])] 
                  [[:description false] [:number false]] ['(:description [:desc nil nil]) '(:number [:asc nil nil])]
                  [[:name false]] ['(:number [:desc nil nil])]))
           (is (f "{a:name}building[a:address d:name].{a:number d:description}floor[description number].{d:number}room[a:name]" 
                  [[:address false] [:name false]] ['(:name [:asc nil nil])] 
                  [[:description false] [:number false]] ['(:description [:desc nil nil]) '(:number [:asc nil nil])]
                  [[:name false]] ['(:number [:desc nil nil])]))
           (is (f "{a:name}building.{a:number d:description}floor[description number].{d:number}room[name description]" 
                  [] ['(:name [:asc nil nil])] 
                  [[:description false] [:number false]] ['(:description [:desc nil nil]) '(:number [:asc nil nil])]
                  [[:name false] [:description false]] ['(:number [:desc nil nil])]))
           (is (f "{a:name}building.{a:number d:description}floor[description number].{d:number a:name}room[name description]" 
                  [] ['(:name [:asc nil nil])] 
                  [[:description false] [:number false]] ['(:description [:desc nil nil]) '(:number [:asc nil nil])]
                  [[:name false] [:description false]] ['(:name [:asc nil nil]) '(:number [:desc nil nil])]))
           (is (f "{a:name}building.floor[a:description number].{d:number a:name d:description}room[name description]" 
                  [] ['(:name [:asc nil nil])] 
                  [[:description false] [:number false]] [[nil nil nil] [:asc nil nil] [nil nil nil]]
                  [[:name false] [:description false]] 
                  ['(:description [:desc nil nil]) '(:name [:asc nil nil]) '(:number [:desc nil nil])]))
           )))


(deftest preds-with-dp
         ^{:doc "Tests parsing queries with predicates which use default property."}
         (let [f #(= (parse %1 mom-)
                     [{:what Building
                       :preds %2}])]
           (is (f "building#(floor.=1)" 
                  [{:ids [{:id ["floors"] :cl Floor} {:id ["number"] :cl nil}], :func #'clojure.core/=, :value 1}]))
           (is (f "building#(room.=1)" 
                  [{:ids [{:id ["floors" "rooms"] :cl Room} 
                          {:id ["number"] :cl nil}], :func #'clojure.core/=, :value 1}]))
           (is (f "building#(floor.room.=1)" 
                  [{:ids [{:id ["floors"] :cl Floor} {:id ["rooms"] :cl Room}
                          {:id ["number"] :cl nil}], :func #'clojure.core/=, :value 1}]))
           (is (f "building#(floor.=1 && room.=2)" 
                  [{:ids [{:id ["floors"] :cl Floor} {:id ["number"] :cl nil}], :func #'clojure.core/=, :value 1} 
                   {:ids [{:id ["floors" "rooms"] :cl Room} {:id ["number"] :cl nil}], :func #'clojure.core/=, :value 2}
                   :and]))
           (is (f "building#(.=1)" 
                  [{:ids [{:id ["name"] :cl nil}], :func #'clojure.core/=, :value 1}]))
           (is (f "building#(.=1 && .=2)" 
                  [{:ids [{:id ["name"] :cl nil}], :func #'clojure.core/=, :value 1}
                   {:ids [{:id ["name"] :cl nil}], :func #'clojure.core/=, :value 2}
                   :and]))
           (is (f "building#(.=(1 && 2))" 
                  [{:ids [{:id ["name"] :cl nil}], :func #'clojure.core/=, :value 1}
                   {:ids [{:id ["name"] :cl nil}], :func #'clojure.core/=, :value 2}
                   :and]))
           (is (f "building#(.=1 && .=2 && .=3)" 
                  [{:ids [{:id ["name"] :cl nil}], :func #'clojure.core/=, :value 1}
                   {:ids [{:id ["name"] :cl nil}], :func #'clojure.core/=, :value 2}
                   :and
                   {:ids [{:id ["name"] :cl nil}], :func #'clojure.core/=, :value 3}
                   :and]))
           (let [mom- (assoc mom- Floor (assoc (get mom- Floor) :dp nil))]
             (is (= (parse "building#(floor.=1)" mom-)
                     [{:what Building
                       :preds [{:ids [{:id ["floors"] :cl Floor}], :func #'clojure.core/=, :value 1}]}])))
           (let [mom- (assoc mom- Floor 
                             (assoc (get mom- Floor) 
                                    :p-properties {:number {:s-to-r #'inc}}))]
             (is (= (parse "building#(floor.=1)" mom-)
                     [{:what Building
                       :preds [{:ids [{:id ["floors"] :cl Floor} 
                                      {:id ["number"] :cl nil}], :func #'clojure.core/=, 
                                :value {:func #'clojure.core/inc :params [1]}}]
                       }])))
           ))


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

;; Default property into predicates.
   "building#(floor.=1)"
   "building#(floor.=1 || floor.number=2)"
   "building#(floor.=1 || room.number=2)"
   "building#(floor.=(1 || 2))"
   "building#(floor.=(1 && <2))"
   "building#(floor.=(1 && 2))"
   "room (building#(floor.=1))"
   "floor (room (building#(floor.=1)))"
   "device (floor (room (building#(floor.=1))))"
   "room.building#(floor.=1)"
   "floor.room.building#(floor.=1)"
   "device.floor.room.building#(floor.=1)"
   "room, building#(floor.=1)"
   "floor, room, building#(floor.=1)"
   "device, floor, room, building#(floor.=1)"
   "device#(ni.=1), floor, room, building#(floor.=1)"
   "device#(ni.=1), floor#(room.=2), room, building#(floor.=1)"
   "building#(floor.=(1 && 2)).room#(device.=3)"
   "building#(floor.=(1 && 2)).room#(device.=(3 || 4))"
   "building#(floor.=(1 && 2)) (room#(device.=3))"
   "building#(floor.=(1 && 2)) (room#(device.=(3 || 4)))"
   "building#(floor.=(1 && 2)), room#(device.=3)"
   "building#(floor.=(1 && 2)), room#(device.=(3 || 4))"

   "floor#(.=1)"
   "floor#(.=1 && .=2)"
   "floor#(.=(1 && 2))"
   "floor#(.=1 || .=2)"
   "floor#(.=(1 || 2))"
   "floor#(.=1 || .=2 || .=3)"
   "floor#(name=\"sname\" && .=1)"
   "floor#(name=\"sname\" && .=(1 && 2))"
   "floor#(name=\"sname\" || .=1)"
   "floor#(name=\"sname\" || .=(1 && 2))"
   "floor#(.=1 && name=\"sname\")"
   "floor#(.=(1 && 2) && name=\"sname\")"
   "floor#((.=1 || .=2) && name=\"sname\")"
   "floor#(.=1 || .=2 && name=\"sname\")"
   "floor#(.=1 || name=\"sname\" || .=2)"
   "floor#((.=(1 && 2) || .=3) && name=\"sname\")"
   "room, floor#(.=1)"
   "room, floor#(.=1 && .=2)"
   "room, floor#(.=(1 && 2))"
   "room, floor#(.=1 || .=2)"
   "room, floor#(.=(1 || 2))"
   "room, floor#(.=1 || .=2 || .=3)"
   "room (floor#(.=1))"
   "room (floor#(.=1 && .=2))"
   "room (floor#(.=(1 && 2)))"
   "room (floor#(.=1 || .=2))"
   "room (floor#(.=(1 || 2)))"
   "room (floor#(.=1 || .=2 || .=3))"

   
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

;; Sorting (typing syntax).
   "a:room"
   "d:room"
   "d:room.d:number"
   "d:room.a:number"
   "d:room.number"
   "a:room (building)"
   "room (d:building)"
   "a:room (d:building)"
   "a:room (floor (d:building))"
   "a:room (device (floor (d:building)))"
   "a:room (device (d:floor (d:building)))"
   "a:room (d:device (d:floor (d:building)))"
   "a:room (device (floor, d:building))"
   "a:building (d:room)"

   "room[d:number]"
   "room[& d:number]"
   "room[d:& d:number]"
   "room[name & d:number]"
   "room[d:& name a:number]"

   "d:room[d:number]"
   "d:room[& d:number]"
   "d:room[d:& d:number]"
   "d:room[name & d:number]"
   "d:room[d:& name a:number]"

   "d:room[number]"
   "d:room[& number]"
   "d:room[& number]"
   "d:room[name & number]"
   "d:room[& name number]"

   "room.building[d:description]"
   "room.building[& d:description]"
   "room.building[d:& d:description]"
   "room.building[name & d:description]"
   "room.building[d:& name a:description]"
   "room (building[d:description])"
   "room (building[& d:description])"
   "room (building[d:& d:description])"
   "room (building[name & d:description])"
   "room (building[d:& name a:description])"

;; Sorting by properties which are not selected.
   "{a:number}room"
   "{d:number}room"
   "{a:number d:description}room"
   "{a:number a:description}room"
   "{d:number a:description}room"
   "{d:number d:description}room"
   "{d:number d:description a:name}room"
   "{a:number}room.number"
   "{a:number}room.floor"
   "{a:number}room.floor.number"
   "{a:number}room.building.name"
   "{a:number}room.{a:name d:description}building.name"
   "{a:number d:name}room.{a:name d:description}building.name"
   "room.{a:name d:description}building.name"
   "{a:number d:name}room.{a:name d:description}building.floor.device"
   "room.{a:name d:description}building.floor.device"
   "room.{a:name d:description}building.{a:number d:name}floor.device"
   "room.building.floor.{a:description d:name}device"
   "room.building.{a:number d:name}floor.device"
   "room.building.{a:number d:name}floor.{a:description d:name}device"
   "room.{a:name}building.{a:number d:name}floor.{a:description d:name}device"
   "{a:number}room.{a:name}building.{a:number d:name}floor.{a:description d:name}device"
   "{a:number}room (building)"
   "{a:number}room ({a:name}building)"
   "room ({a:name}building)"
   "room ({a:name}building (device))"
   "{a:number}room ({a:name}building (device))"
   "{a:number}room ({a:name}building ({a:name}device))"
   "room ({a:name}building ({a:name}device))"
   "room (building ({a:name}device))"
   "{a:number}room (building ({a:name}device))"
   "{a:number}room, building"
   "{a:number}room, {a:name}building"
   "room, {a:name}building"
   "{a:number}room, {a:name}building, {d:name}device"
   "{a:number}room (floor), building"
   "{a:number}room ({d:number}floor), building"
   "{a:number}room ({d:number}floor), {a:number}building"
   "{a:@(count `room')}building"
   "{d:@(count `room')}building"
   "{a:name d:@(count `room')}building"
   "{d:name a:@(count `room')}building"
   "{a:@(count `room') d:name}building"
   "{a:@(count `room') a:name}building"
   "{a:@(count `room') a:name d:description}building"
   "{a:@(count `room') a:name d:description a:floor}building"
   "{a:name d:description a:@(count `room') a:floor}building"
   "{a:name d:description a:floor a:@(count `room')}building"

;; Regular expressions
   "room#(number~\".*\")"
   "room#(number~\".00$\")"
   "room#(number~\".00$\" || number=\"215\")"
   "room#(number~\".00$\" || number~\"215\")"
   "room#(number=\".00$\" || number~\"215\")"
   "room#(number~(\".00$\" || =\"215\"))"
   "room#(number~(\".00$\" || ~\"215\"))"
   "room#(number=(\".00$\" || ~\"215\"))"
   "building (room#(number~\".*\"))"
   "building (room#(number~\".00$\"))"
   "building (room#(number~\".00$\" || number=\"215\"))"
   "building (room#(number~\".00$\" || number~\"215\"))"
   "building (room#(number=\".00$\" || number~\"215\"))"
   "building (room#(number~\".00$\" && number=\"215\"))"
   "building (room#(number~\".00$\" && number~\"215\"))"
   "building (room#(number=\".00$\" && number~\"215\"))"
   "building (room#(number~(\".00$\" || =\"215\")))"
   "building (room#(number~(\".00$\" || ~\"215\")))"
   "building (room#(number=(\".00$\" || ~\"215\")))"
   "building (room#(number~(\".00$\" && =\"215\")))"
   "building (room#(number~(\".00$\" && ~\"215\")))"
   "building (room#(number=(\".00$\" && ~\"215\")))"
   "building (room#(number~(=\".00$\" || \"215\")))"
   "building (room#(number~(~\".00$\" || \"215\")))"
   "building (room#(number=(~\".00$\" || \"215\")))"
   "building (room#(number~(=\".00$\" && \"215\")))"
   "building (room#(number~(~\".00$\" && \"215\")))"
   "building (room#(number=(~\".00$\" && \"215\")))"
   "building (room#(number~(\".00$\" || !=\"215\")))"
   "building (room#(number~(\".00$\" || !=\"215\")))"
   "building (room#(number=(\".00$\" || !=\"215\")))"
   "building (room#(number~(\".00$\" && !=\"215\")))"
   "building (room#(number~(\".00$\" && !=\"215\")))"
   "building (room#(number=(\".00$\" && !=\"215\")))"
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


(def qlist-next-query
  ^{:doc "Defines list with queries with the 
         following structure: room, query from qlist."}
  (map #(str "room, " %) qlist))


(def qlist-next-query-qlist
  ^{:doc "Defines list with queries with the 
         following structure: query from qlist, query from qlist."}
  (map #(str % ", " %) qlist))


(def qlist-nest-query
  ^{:doc "Defines list with queries with the 
         following structure: son (query from qlist)."}
  (map #(str "son (" % ")") qlist))


(deftest parse-remainder
         ^{:doc "Checks remainder after parsing for queries in 'qlist' vector.
                It must be nil for all queries, because qlist contains
                only correct queries."}

         ; If all queries are success, then results is nil, 
         ; otherwise results is query which is failed and
         ; clojure.test prints something like this: 
         ;   expected: (nil? results)
         ;   actual: (not (nil? "room[name number floor].floor,"))
         ;
         ; It is all I need.
         (let [results (fn [l] 
                         (some #(let [r (try 
                                          (:remainder (parse+ % mom-))
                                          (catch Exception e (do (.printStackTrace e) %)))]
                                  (if r %)) l))]
           (is (nil? (results qlist)))
           (is (nil? (results qlist-next-query)))
           (is (nil? (results qlist-next-query-qlist)))
           (is (nil? (results qlist-nest-query)))))



(deftest neg-parse-tests
         ^{:doc "Contains tests which are thrown exceptions."}
         (is (thrown? SyntaxException (parse "building#" mom-)))
         (is (thrown? NullPointerException (parse "(building)" mom-)))
         (is (thrown? NullPointerException (parse ", building" mom-)))
         (is (thrown? SyntaxException (parse "building, " mom-))))

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
