;;
;; Copyright 2011-2013 Vyacheslav Dimitrov <vyacheslav.dimitrov@gmail.com>
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
    :doc "Tests parsing of queries."}
  (:use ru.petrsu.nest.yz.core
        ru.petrsu.nest.yz.mom-utils 
        clojure.test 
        ru.petrsu.nest.yz.queries.bd)
  (:require [ru.petrsu.nest.yz.yz-factory :as yzf] 
            [ru.petrsu.nest.yz.parsing :as p] 
            [ru.petrsu.nest.yz.utils :as u])
  (:import (ru.petrsu.nest.son 
             Building Room Floor 

             ; Importing of this classes are needed for parsing 
             ; queries with this classes in case MOM is nil.
             ; So DON'T REMOVE THIS IMPORTS.
             Device Network NetworkInterface Occupancy LinkInterface CompositeOU)
           (ru.petrsu.nest.yz SyntaxException NotDefinedDPException)))

(def mom- 
  ^{:doc "Defines the map of the object model (used Nest's model)"}
  (mom-from-file "nest.mom"))

(defn dis-props-sort
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

(defn sort-to-nil
  "Takes the MOM and associates with the :sort key 
  the [nil nil nil] value. Returns new MOM."
  [mom]
  (reduce (fn [m [k v]] 
            (if (map? v)
              (assoc m k (assoc v :sort [nil nil nil]))
              (assoc m k v))) {} mom))


(defn process-result
  "Takes vector with maps and process it
  due to some functions pred and f: in case
  pred is true then f is called, else 
  value is old."
  [s pred f]
  (let [rn #(reduce (fn [m [k v]] 
                      (if (pred v) 
                        (f m k)
                        (assoc m k (if (or (= k :nest) (= k :then) (= k :params) (= k :preds)) 
                                     (process-result v pred f) 
                                     v)))) 
                    {} %1)
        s (cond (vector? s) (vec (map #(cond (vector? %1) (process-result %1 pred f)
                                             (map? %1) (rn %1)
                                             :else %1) s)) 
                (map? s) (rn s)
                :else s)]
    s))


(defn change-boolean
  "Takes vector with maps and change
  primitive type boolean to java.lang.Boolean. 
  Also calls change-boolean for :nest and :then keys."
  [s]
  (process-result s #(and (class? %) (.isPrimitive %)) 
                  #(assoc %1 %2 java.lang.Boolean)))


(defn remove-nils
  "Takes vector with maps and removes 
  nil values from this maps. Also calls 
  remove-nils for :nest and :then keys."
  [s]
  (process-result s nil? (fn [m _] m)))


(defn parse
  "Like parse from the parsing namespace, 
  but remove nils value from resulting structure."
  [q mom]
  (change-boolean (remove-nils (p/parse q mom))))


(deftest t-parse
         ^{:doc "Tests 'parse' function."}
         (let [mom- (sort-to-nil mom-)]
          (is (= (parse "building", mom-)
                 [{:what ru.petrsu.nest.son.Building }]))


         (is (= (parse "building (room)", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :nest [{:what ru.petrsu.nest.son.Room
                           :where [["floors" "rooms"]]}]}]))


         (is (= (parse "building (floor.room)", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :nest [{:what ru.petrsu.nest.son.Floor
                           :where [["floors"]]
                           :then {:what ru.petrsu.nest.son.Room
                                  :where [["rooms"]]}}]}]))


         (is (= (parse "building (floor.room.building)", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :nest [{:what ru.petrsu.nest.son.Floor
                           :where [["floors"]]
                           :then {:what ru.petrsu.nest.son.Room
                                  :where [["rooms"]]
                                  :then {:what ru.petrsu.nest.son.Building
                                         :where [["floor" "building"]]}}}]}]))


         (is (= (parse "building (floor.room (building))", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :nest [{:what ru.petrsu.nest.son.Floor
                           :where [["floors"]] 
                           :nest [{:what ru.petrsu.nest.son.Building
                                   :where [["floor" "building"]]}]
                           :then {:what ru.petrsu.nest.son.Room
                                  :where [["rooms"]]}}]}]))


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
                                   :nest [{:what ru.petrsu.nest.son.Network}]}]}]}]))


         (is (= (parse "building (room (device (ipnetwork)))", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :nest [{:what ru.petrsu.nest.son.Room
                           :where [["floors" "rooms"]] 
                           :nest [{:what ru.petrsu.nest.son.Device
                                   :where [["occupancies" "devices"]]
                                   :nest [{:what ru.petrsu.nest.son.IPNetwork
                                           :where [["linkInterfaces" "networkInterfaces" "network"]]}]}]}]}]))


         (is (= (parse "building (room (device (network (floor))))", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :nest [{:what ru.petrsu.nest.son.Room
                           :where [["floors" "rooms"]]
                           :nest [{:what ru.petrsu.nest.son.Device
                                   :where [["occupancies" "devices"]] 
                                   :nest [{:what ru.petrsu.nest.son.Network
                                           :nest [{:what ru.petrsu.nest.son.Floor}]}]}]}]}]))

         
         (is (= (parse "building (room (device (ipnetwork (floor))))", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :nest [{:what ru.petrsu.nest.son.Room
                           :where [["floors" "rooms"]]
                           :nest [{:what ru.petrsu.nest.son.Device
                                   :where [["occupancies" "devices"]] 
                                   :nest [{:what ru.petrsu.nest.son.IPNetwork
                                           :where [["linkInterfaces" "networkInterfaces" "network"]]
                                           :nest [{:what ru.petrsu.nest.son.Floor
                                                   :where [["networkInterfaces" 
                                                            "linkInterface" 
                                                            "device" 
                                                            "occupancy" 
                                                            "room" "floor"]]}]}]}]}]}]))

         
         (is (= (parse "building (room (device, floor), ipnetwork)", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :nest [{:what ru.petrsu.nest.son.Room
                           :where [["floors" "rooms"]]
                           :nest [{:what ru.petrsu.nest.son.Device
                                   :where [["occupancies" "devices"]]}
                                  u/union
                                  {:what ru.petrsu.nest.son.Floor
                                   :where [["floor"]]}]} 
                          u/union
                          {:what ru.petrsu.nest.son.IPNetwork
                           :where [["floors" "rooms" "occupancies" "devices" "linkInterfaces" "networkInterfaces" "network"]]}]}]))


         (is (= (parse "building (room (device, floor), network)", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :nest [{:what ru.petrsu.nest.son.Room
                           :where [["floors" "rooms"]]
                           :nest [{:what ru.petrsu.nest.son.Device
                                   :where [["occupancies" "devices"]]}
                                  u/union
                                  {:what ru.petrsu.nest.son.Floor
                                   :where [["floor"]]}]} 
                          u/union
                          {:what ru.petrsu.nest.son.Network}]}]))


         (is (= (parse "building (room, device)", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :nest [{:what ru.petrsu.nest.son.Room
                           :where [["floors" "rooms"]]}
                          u/union
                          {:what ru.petrsu.nest.son.Device
                           :where [["floors" "rooms" "occupancies" "devices"]]}]}]))

         (is (= (parse "building (room, occupancy (device (network (floor)), networkinterface))", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :nest [{:what ru.petrsu.nest.son.Room
                           :where [["floors", "rooms"]]}
                          u/union
                          {:what ru.petrsu.nest.son.Occupancy
                           :where [["floors" "rooms" "occupancies"]]
                           :nest [{:what ru.petrsu.nest.son.Device
                                   :where [["devices"]]
                                   :nest [{:what ru.petrsu.nest.son.Network
                                           :nest [{:what ru.petrsu.nest.son.Floor}]}]}
                                  u/union
                                  {:what ru.petrsu.nest.son.NetworkInterface}]}]}]))

         
         (is (= (parse "building (room, occupancy (device (ipnetwork (floor)), ipv4interface))", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :nest [{:what ru.petrsu.nest.son.Room
                           :where [["floors", "rooms"]]}
                          u/union
                          {:what ru.petrsu.nest.son.Occupancy
                           :where [["floors" "rooms" "occupancies"]]
                           :nest [{:what ru.petrsu.nest.son.Device
                                   :where [["devices"]]
                                   :nest [{:what ru.petrsu.nest.son.IPNetwork
                                           :where [["linkInterfaces" "networkInterfaces" "network"]]
                                           :nest [{:what ru.petrsu.nest.son.Floor
                                                   :where [["networkInterfaces" 
                                                            "linkInterface" 
                                                            "device" 
                                                            "occupancy" 
                                                            "room" "floor"]]}]}]}
                                  u/union
                                  {:what ru.petrsu.nest.son.IPv4Interface
                                   :where [["devices" "linkInterfaces" "networkInterfaces"]]}]}]}]))

         
           (is (= (parse "building (room, occupancy (device (network (floor)), networkinterface))", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :nest [{:what ru.petrsu.nest.son.Room
                           :where [["floors", "rooms"]]}
                          u/union
                          {:what ru.petrsu.nest.son.Occupancy
                           :where [["floors" "rooms" "occupancies"]]
                           :nest [{:what ru.petrsu.nest.son.Device
                                   :where [["devices"]]
                                   :nest [{:what ru.petrsu.nest.son.Network
                                           :nest [{:what ru.petrsu.nest.son.Floor}]}]}
                                  u/union
                                  {:what ru.petrsu.nest.son.NetworkInterface}]}]}]))


         (is (= (parse "building, room", mom-)
                 [{:what ru.petrsu.nest.son.Building}
                  u/union
                  {:what ru.petrsu.nest.son.Room}]))))

(deftest t-parse-props
         ^{:doc "Tests parsing queries with properties."}
         (let [mom- (sort-to-nil mom-)]
         (is (= (parse "building.name", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :then {:what java.lang.String
                          :where [["name"]]}}]))


         (is (= (parse "building.room.floor.rooms", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :then {:what ru.petrsu.nest.son.Room 
                          :where [["floors" "rooms"]]
                          :then {:what ru.petrsu.nest.son.Floor 
                                 :where [["floor"]] 
                                 :then {:what Room
                                        :where [["rooms"]]}}}}]))


         (is (= (parse "building.floor.room.occupancy.device.forwarding", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :then {:what ru.petrsu.nest.son.Floor 
                          :where [["floors"]] 
                          :then {:what ru.petrsu.nest.son.Room
                                 :where [["rooms"]]
                                 :then {:what ru.petrsu.nest.son.Occupancy
                                        :where [["occupancies"]]
                                        :then {:what ru.petrsu.nest.son.Device
                                               :then {:what java.lang.Boolean
                                                      :where [["forwarding"]]}
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
                          :then {:what java.lang.String
                                 :where [["number"]]}
                          :where [["floors" "rooms"]]}}]))))


(deftest t-parse-predicates
         ^{:doc "Tests parsing queries with some restrictions."}
         (let [mom- (sort-to-nil mom-)]
          (is (= (parse "building#(name=1)", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :preds [{:ids [{:id [["name"]] :cl nil}], :func #'clojure.core/=, :value 1}]}]))
          (is (= (parse "building#(room.number=\"215\")", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :preds [{:ids [{:id [["floors" "rooms"]] :cl Room} 
                                  {:id [["number"]] :cl nil}], :func #'clojure.core/=, :value "215"}]}]))
          (is (= (parse "building#(name=1 and address=2)", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :preds [{:ids [{:id [["name"]] :cl nil}], :func #'clojure.core/=, :value 1} 
                           {:ids [{:id [["address"]] :cl nil}], :func #'clojure.core/=, :value 2} :and]}]))
          (is (= (parse "building#(name=1 or address=2)", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :preds [{:ids [{:id [["name"]] :cl nil}], :func #'clojure.core/=, :value 1} 
                           {:ids [{:id [["address"]] :cl nil}], :func #'clojure.core/=, :value 2} :or]}]))
          (is (= (parse "building#(name=1 and address=2 and floor.number=3)", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :preds [{:ids [{:id [["name"]], :cl nil}], :func #'clojure.core/=, :value 1} 
                           {:ids [{:id [["address"]], :cl nil}], :func #'clojure.core/=, :value 2} 
                           :and {:ids [{:id [["floors"]] :cl Floor}, 
                                       {:id [["number"]] :cl nil}], :func #'clojure.core/=, :value 3} :and]}]))
          (is (= (parse "building#(name=1 and address=2 or floor.number=3)", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :preds [{:ids [{:id [["name"]], :cl nil}], :func #'clojure.core/=, :value 1} 
                           {:ids [{:id [["address"]], :cl nil}], :func #'clojure.core/=, :value 2} 
                           :and {:ids [{:id [["floors"]] :cl Floor}, 
                                       {:id [["number"]] :cl nil}], :func #'clojure.core/=, :value 3} :or]}]))
          (is (= (parse "building#(name=1 or address=2 and floor.number=3)", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :preds [{:ids [{:id [["name"]], :cl nil}], :func #'clojure.core/=, :value 1} 
                           {:ids [{:id [["address"]], :cl nil}], :func #'clojure.core/=, :value 2} 
                           {:ids [{:id [["floors"]] :cl Floor}, 
                                  {:id [["number"]] :cl nil}], :func #'clojure.core/=, :value 3} :and :or]}]))
          (is (= (parse "building#(name=1 and (address=2 or floor.number=3))", mom-)
                 [{:what ru.petrsu.nest.son.Building 
                   :preds [{:ids [{:id [["name"]], :cl nil}], :func #'clojure.core/=, :value 1} 
                           {:ids [{:id [["address"]], :cl nil}], :func #'clojure.core/=, :value 2} 
                           {:ids [{:id [["floors"]] :cl Floor}, 
                                  {:id [["number"]] :cl nil}], :func #'clojure.core/=, :value 3} :or :and]}]))))


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
                                               :then {:what java.lang.Boolean
                                                      :where [["forwarding"]]}
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
                                               :then {:what java.lang.Boolean
                                                      :where [["forwarding"]]}
                                               :where [["devices"]]}}}}}]))
           (let [f #(= (parse %1 mom-)
                       (let [sthen {:what ru.petrsu.nest.son.Room
                                    :where [["rooms"]]
                                    :then {:what ru.petrsu.nest.son.Occupancy
                                            :where [["occupancies"]]
                                            :then {:what ru.petrsu.nest.son.Device
                                                   :then {:what java.lang.Boolean
                                                          :where [["forwarding"]]}
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
                                               :then {:what java.lang.Boolean
                                                      :where [["forwarding"]]}
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
                                               :then {:what java.lang.Boolean
                                                      :where [["forwarding"]]}
                                               :where [["devices"]]}}}}}]))
           (let [f #(= (parse %1 mom-)
                       (let [sthen {:what ru.petrsu.nest.son.Room
                                    :props %2
                                    :sort %3
                                    :where [["rooms"]]
                                    :then {:what ru.petrsu.nest.son.Occupancy
                                            :where [["occupancies"]]
                                            :then {:what ru.petrsu.nest.son.Device
                                                   :then {:what java.lang.Boolean
                                                          :where [["forwarding"]]}
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
                                               :then {:what java.lang.Boolean
                                                      :where [["forwarding"]]}
                                               :sort [:desc nil nil]
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
                  [{:ids [{:id [["floors"]] :cl Floor} {:id [["number"]] :cl nil}], :func #'clojure.core/=, :value 1}]))
           (is (f "building#(room.=1)" 
                  [{:ids [{:id [["floors" "rooms"]] :cl Room} 
                          {:id [["number"]] :cl nil}], :func #'clojure.core/=, :value 1}]))
           (is (f "building#(floor.room.=1)" 
                  [{:ids [{:id [["floors"]] :cl Floor} {:id [["rooms"]] :cl Room}
                          {:id [["number"]] :cl nil}], :func #'clojure.core/=, :value 1}]))
           (is (f "building#(floor.=1 && room.=2)" 
                  [{:ids [{:id [["floors"]] :cl Floor} {:id [["number"]] :cl nil}], :func #'clojure.core/=, :value 1} 
                   {:ids [{:id [["floors" "rooms"]] :cl Room} {:id [["number"]] :cl nil}], :func #'clojure.core/=, :value 2}
                   :and]))
           (is (f "building#(.=1)" 
                  [{:ids [{:id [["name"]] :cl nil}], :func #'clojure.core/=, :value 1}]))
           (is (f "building#(.=1 && .=2)" 
                  [{:ids [{:id [["name"]] :cl nil}], :func #'clojure.core/=, :value 1}
                   {:ids [{:id [["name"]] :cl nil}], :func #'clojure.core/=, :value 2}
                   :and]))
           (is (f "building#(.=(1 && 2))" 
                  [{:ids [{:id [["name"]] :cl nil}], :func #'clojure.core/=, :value 1}
                   {:ids [{:id [["name"]] :cl nil}], :func #'clojure.core/=, :value 2}
                   :and]))
           (is (f "building#(.=1 && .=2 && .=3)" 
                  [{:ids [{:id [["name"]] :cl nil}], :func #'clojure.core/=, :value 1}
                   {:ids [{:id [["name"]] :cl nil}], :func #'clojure.core/=, :value 2}
                   :and
                   {:ids [{:id [["name"]] :cl nil}], :func #'clojure.core/=, :value 3}
                   :and]))
           (let [mom- (assoc mom- Floor (assoc (get mom- Floor) :dp nil))]
             (is (thrown? NotDefinedDPException (parse "building#(floor.=1)" mom-))))
;             (is (= (parse "building#(floor.=1)" mom-)
;                     [{:what Building
;                       :preds [{:ids [{:id ["floors"] :cl Floor}, 
;                                      {:id ["number"] :cl nil}], 
;                                :func #'clojure.core/=, :value 1}]}])))
           (let [mom- (assoc mom- Floor 
                             (assoc (get mom- Floor) 
                                    :p-properties {:number {:s-to-r #'inc}}))]
             (is (= (parse "building#(floor.=1)" mom-)
                     [{:what Building
                       :preds [{:ids [{:id [["floors"]] :cl Floor} 
                                      {:id [["number"]] :cl nil}], :func #'clojure.core/=, 
                                :value {:func #'clojure.core/inc :params [1]}}]
                       }])))
           ))


(deftest preds-with-allm
         ^{:doc "Tests parsing queries with predicates which contain ALL modificator (∀)."}
         (let [f #(every? (fn [q] (= (parse q mom-) [{:what Building :preds %2}])) %1)]
           (is (f ["building#(∀floor.=1)" "building#(all:floor.=1)"]
                  [{:all true :ids [{:id [["floors"]] :cl Floor} 
                                    {:id [["number"]] :cl nil}], :func #'clojure.core/=, :value 1}]))
           (is (f ["building#(∀room.=1)" "building#(all:room.=1)"]
                  [{:all true :ids [{:id [["floors" "rooms"]] :cl Room} 
                          {:id [["number"]] :cl nil}], :func #'clojure.core/=, :value 1}]))
           (is (f ["building#(∀floor.room.=1)" "building#(all:floor.room.=1)"]
                  [{:all true :ids [{:id [["floors"]] :cl Floor} {:id [["rooms"]] :cl Room}
                          {:id [["number"]] :cl nil}], :func #'clojure.core/=, :value 1}]))
           (is (f ["building#(∀floor.=1 && room.=2)" "building#(all:floor.=1 && room.=2)"]
                  [{:all true :ids [{:id [["floors"]] :cl Floor} {:id [["number"]] :cl nil}], :func #'clojure.core/=, :value 1} 
                   {:ids [{:id [["floors" "rooms"]] :cl Room} {:id [["number"]] :cl nil}], :func #'clojure.core/=, :value 2}
                   :and]))
           (is (f ["building#(∀floor.=1 && ∀room.=2)" "building#(all:floor.=1 && all:room.=2)" 
                   "building#(all:floor.=1 && ∀room.=2)" "building#(∀floor.=1 && all:room.=2)"]
                  [{:all true :ids [{:id [["floors"]] :cl Floor} {:id [["number"]] :cl nil}], :func #'clojure.core/=, :value 1} 
                   {:all true :ids [{:id [["floors" "rooms"]] :cl Room} 
                                    {:id [["number"]] :cl nil}], :func #'clojure.core/=, :value 2}
                   :and]))
           (is (f ["building#(floor.=1 && ∀room.=2)" "building#(floor.=1 && all:room.=2)"]
                  [{:ids [{:id [["floors"]] :cl Floor} {:id [["number"]] :cl nil}], :func #'clojure.core/=, :value 1} 
                   {:all true :ids [{:id [["floors" "rooms"]] :cl Room} {:id [["number"]] :cl nil}], :func #'clojure.core/=, :value 2}
                   :and]))
           (is (f ["building#(∀.=1)" "building#(all:.=1)"]
                  [{:all true :ids [{:id [["name"]] :cl nil}], :func #'clojure.core/=, :value 1}]))
           (is (f ["building#(∀.=1 && .=2)" "building#(all:.=1 && .=2)"]
                  [{:all true :ids [{:id [["name"]] :cl nil}], :func #'clojure.core/=, :value 1}
                   {:ids [{:id [["name"]] :cl nil}], :func #'clojure.core/=, :value 2}
                   :and]))
           (is (f ["building#(∀.=1 && ∀.=2)" "building#(all:.=1 && all:.=2)" 
                   "building#(all:.=1 && ∀.=2)" "building#(∀.=1 && all:.=2)"]
                  [{:all true :ids [{:id [["name"]] :cl nil}], :func #'clojure.core/=, :value 1}
                   {:all true :ids [{:id [["name"]] :cl nil}], :func #'clojure.core/=, :value 2}
                   :and]))
           (is (f ["building#(.=1 && ∀.=2)" "building#(.=1 && all:.=2)"]
                  [{:ids [{:id [["name"]] :cl nil}], :func #'clojure.core/=, :value 1}
                   {:all true :ids [{:id [["name"]] :cl nil}], :func #'clojure.core/=, :value 2}
                   :and]))
           (is (f ["building#(∀.=(1 && 2))" "building#(all:.=(1 && 2))"]
                  [{:all true :ids [{:id [["name"]] :cl nil}], :func #'clojure.core/=, :value 1}
                   {:all true :ids [{:id [["name"]] :cl nil}], :func #'clojure.core/=, :value 2}
                   :and]))
           (is (f ["building#(.=1 && .=2 && ∀.=3)" "building#(.=1 && .=2 && all:.=3)"]
                  [{:ids [{:id [["name"]] :cl nil}], :func #'clojure.core/=, :value 1}
                   {:ids [{:id [["name"]] :cl nil}], :func #'clojure.core/=, :value 2}
                   :and
                   {:all true :ids [{:id [["name"]] :cl nil}], :func #'clojure.core/=, :value 3}
                   :and]))
           (is (f ["building#(.=1 && ∀.=2 && ∀.=3)" "building#(.=1 && all:.=2 && all:.=3)" 
                   "building#(.=1 && all:.=2 && ∀.=3)" "building#(.=1 && ∀.=2 && all:.=3)"]
                  [{:ids [{:id [["name"]] :cl nil}], :func #'clojure.core/=, :value 1}
                   {:all true :ids [{:id [["name"]] :cl nil}], :func #'clojure.core/=, :value 2}
                   :and
                   {:all true :ids [{:id [["name"]] :cl nil}], :func #'clojure.core/=, :value 3}
                   :and]))
           (is (f ["building#(∀.=1 && ∀.=2 && ∀.=3)" "building#(all:.=1 && all:.=2 && all:.=3)"]
                  [{:all true :ids [{:id [["name"]] :cl nil}], :func #'clojure.core/=, :value 1}
                   {:all true :ids [{:id [["name"]] :cl nil}], :func #'clojure.core/=, :value 2}
                   :and
                   {:all true :ids [{:id [["name"]] :cl nil}], :func #'clojure.core/=, :value 3}
                   :and]))
           ))


(deftest limiting
         ^{:doc "Tests parsing queries with limiting."}
         (let [f #(= (parse %1 mom-)
                     [{:what Building
                       :limit %2}])]
           (is (f "1:building" [nil 1 false]))
           (is (f "0-1:building" [0 1 false]))
           (is (f "-1:building" [nil 1 true]))
           (is (f "2-3:building" [2 3 false]))
           (is (f "-2-3:building" [2 3 true])))
         (let [f #(= (parse %1 mom-)
                     [{:what Building
                       :nest [{:what Floor :where [["floors"]] :limit %2}]}])]
           (is (f "building (1:floor)" [nil 1 false]))
           (is (f "building (-1:floor)" [nil 1 true]))
           (is (f "building (0-1:floor)" [0 1 false]))
           (is (f "building (2-3:floor)" [2 3 false]))
           (is (f "building (-2-3:floor)" [2 3 true])))
         (let [f #(= (parse %1 mom-)
                     [{:what Building
                       :limit %2
                       :nest [{:what Floor :where [["floors"]] :limit %3}]}])]
           (is (f "1:building (1:floor)" [nil 1 false] [nil 1 false]))
           (is (f "-1:building (1:floor)" [nil 1 true] [nil 1 false]))
           (is (f "1-2:building (1:floor)" [1 2 false] [nil 1 false]))
           (is (f "1:building (-1:floor)" [nil 1 false] [nil 1 true]))
           (is (f "1-2:building (2-3:floor)" [1 2 false] [2 3 false]))
           (is (f "-1-2:building (-2-3:floor)" [1 2 true] [2 3 true]))))


(deftest limit-sorting-unique
  "Defines queries with limiting, sorting and unique options."
  (let [v (for [s [["a:" [:asc nil nil]] ["d:" [:desc nil nil]] 
                   ["↑" [:asc nil nil]] ["↓" [:desc nil nil]] ["" nil]] ; modificators of sorting
                u [["u:" true] ["¹" true] ["" nil]] ; modificators of removing duplicates
                l [["1:" [nil 1 false]] ["1-2:" [1 2 false]] ["-1-2:" [1 2 true]] 
                   ["-1:" [nil 1 true]] ["" nil]]] ; examples of limiting
            [s u l]) 
        mom- (sort-to-nil mom-)
        f #(= (parse %1 mom-) [%2])
        q-res (map (fn [[[s_str s_struct] 
                         [u_str u_struct] 
                         [l_str l_struct]]] 
                     (let [m {:what Floor}
                           m (reduce (fn [m [v k]] (if v (assoc m k v) m)) 
                                     m 
                                     [[s_struct :sort]
                                      [u_struct :unique] 
                                      [l_struct :limit]])]
                       (f (str s_str u_str l_str "floor") m))) v)]
    (is (every? true? q-res))))


(deftest funcs-mods
         ^{:doc "Tests function's modificators."}
         (let [f #(= (parse %1 mom-) [{:func #'clojure.core/count :params [[%2 [{:what Building}]]]}])]
           (is (f "@(count `building')" :dep-list))
           (is (f "@(count dep-list:`building')" :dep-list))
           (is (f "@(count dl:`building')" :dep-list))
           (is (f "@(count dep-each:`building')" :dep-each))
           (is (f "@(count de:`building')" :dep-each))
           (is (f "@(count indep-list:`building')" :indep-list))
           (is (f "@(count il:`building')" :indep-list))
           (is (f "@(count indep-each:`building')" :indep-each))
           (is (f "@(count ie:`building')" :indep-each)))
           
         (let [f #(= (parse %1 mom-) [{:func #'clojure.core/count :params [[%2 [{:what Building}]]]} 
                                      u/union
                                      {:func #'clojure.core/count :params [[%3 [{:what Room}]]]}])]
           (is (f "@(count `building'), @(count `room')" :dep-list :dep-list))
           (is (f "@(count `building'), @(count dl:`room')" :dep-list :dep-list))
           (is (f "@(count dl:`building'), @(count `room')" :dep-list :dep-list))
           (is (f "@(count dl:`building'), @(count dl:`room')" :dep-list :dep-list))
           (is (f "@(count il:`building'), @(count dl:`room')" :indep-list :dep-list))
           (is (f "@(count il:`building'), @(count de:`room')" :indep-list :dep-each))))


(deftest all-medium
         ^{:doc "Tests parsing queries with getting 
                all medium: building->room."}
         (let [f #(= (parse %1 mom-)
                     [(merge
                        {:what Building
                         :nest [{:what Floor
                                 :where [["floors"]]
                                 :medium true
                                 :nest [(merge {:what Room :where [["rooms"]]} %2)]}]}
                        %3)])]
           (is (f "building->room" nil nil))
           (is (f "building->u:room" {:unique true} nil))
           (is (f "building->{a:number}room" {:sort ['(:number [:asc nil nil])]} nil))
           (is (f "building->{a:number d:name}room" 
                  {:sort ['(:name [:desc nil nil]) '(:number [:asc nil nil])]} nil))
           (is (f "building->{a:number d:name d:&}room" 
                  {:sort ['(:self [:desc nil nil]) 
                          '(:name [:desc nil nil]) 
                          '(:number [:asc nil nil])]} nil))
           (is (f "building->1-5:room" {:limit [1 5 false]} nil))
           (is (f "building->room[number]" {:props [[:number false]]} nil))
           (is (f "building->1-5:room[number]" {:limit [1 5 false] :props [[:number false]]} nil))
           (is (f "building->{d:name}1-5:room[number]" 
                  {:limit [1 5 false] :props [[:number false]] 
                   :sort ['(:name [:desc nil nil])]} nil))
           (is (f "1-5:building->room" nil {:limit [1 5 false]}))
           (is (f "{a:name}building->room" nil {:sort ['(:name [:asc nil nil])]}))
           (is (f "u:building->room" nil {:unique true}))
           (is (f "{a:name}u:building->room" nil {:unique true :sort ['(:name [:asc nil nil])]}))
           (is (f "{a:name}u:1-5:building->room" nil {:limit [1 5 false] :unique true :sort ['(:name [:asc nil nil])]}))
           (is (f "{a:name}u:1-5:building->room[number]" 
                  {:props [[:number false]]} 
                  {:limit [1 5 false] :unique true :sort ['(:name [:asc nil nil])]}))
           (is (f "{a:name}u:1-5:building->4-7:room[number]" 
                  {:props [[:number false]] :limit [4 7 false]} 
                  {:limit [1 5 false] :unique true :sort ['(:name [:asc nil nil])]}))
           (is (f "{a:name}u:1-5:building->{d:description}4-7:room[number]" 
                  {:props [[:number false]] :limit [4 7 false] :sort ['(:description [:desc nil nil])]}
                  {:limit [1 5 false] :unique true :sort ['(:name [:asc nil nil])]})))

         (let [f #(= (parse %1 mom-)
                     [(merge
                        {:what Floor
                         :nest [{:what Floor
                                 :where [["floors"]]
                                 :medium true
                                 :nest [(merge {:what Room :where [["rooms"]]} %2)]}]
                         :then (merge {:what Building :where [["building"]]} %4)}
                        %3)])]
           (is (f "floor.building->room" nil nil nil))
           (is (f "floor.building->u:room" {:unique true} nil nil))
           (is (f "floor.building->{a:number}room" {:sort ['(:number [:asc nil nil])]} nil nil))
           (is (f "floor.building->{a:number d:name}room" 
                  {:sort ['(:name [:desc nil nil]) '(:number [:asc nil nil])]} nil nil))
           (is (f "floor.building->{a:number d:name d:&}room" 
                  {:sort ['(:self [:desc nil nil]) 
                          '(:name [:desc nil nil]) 
                          '(:number [:asc nil nil])]} nil nil))
           (is (f "floor.building->1-5:room" {:limit [1 5 false]} nil nil))
           (is (f "floor.building->room[number]" {:props [[:number false]]} nil nil))
           (is (f "floor.building->1-5:room[number]" {:limit [1 5 false] :props [[:number false]]} nil nil))
           (is (f "floor.building->{d:name}1-5:room[number]" 
                  {:limit [1 5 false] :props [[:number false]] 
                   :sort ['(:name [:desc nil nil])]} nil nil))
           (is (f "u:floor.building->u:room" 
                  {:unique true} 
                  {:unique true} 
                  nil))
           (is (f "{a:number}2-6:floor[description].building->{d:name}1-5:room[number]" 
                  {:limit [1 5 false] :props [[:number false]] 
                   :sort ['(:name [:desc nil nil])]} 
                  {:limit [2 6 false] :props [[:description false]] 
                   :sort ['(:number [:asc nil nil])]} 
                  nil))
           (is (f "{a:number}2-6:floor[description].{a:description}3-7:building->{d:name}1-5:room[number]" 
                  {:limit [1 5 false] :props [[:number false]] 
                   :sort ['(:name [:desc nil nil])]} 
                  {:limit [2 6 false] :props [[:description false]] 
                   :sort ['(:number [:asc nil nil])]} 
                  {:limit [3 7 false] :sort ['(:description [:asc nil nil])]})))
         (let [f #(= (parse %1 mom-)
                     [(merge
                        {:what Floor
                         :nest [{:what Floor
                                 :where [["floors"]]
                                 :medium true
                                 :nest [(merge {:what Room :where [["rooms"]]} %2)]}]
                         :then (merge {:what Building :where [["building"]]} %4)}
                        %3)])]
           (is (f "floor.building->room (floor)" 
                  {:nest [{:what Floor :where [["floor"]]}]} 
                  nil nil))
           (is (f "floor.building->room (u:floor)" 
                  {:nest [{:what Floor :where [["floor"]] :unique true}]} 
                  nil nil))
           (is (f "floor.building->room ({a:number}floor)" 
                  {:nest [{:what Floor :where [["floor"]] :sort ['(:number [:asc nil nil])]}]}
                  nil nil))
           (is (f "floor.building->room (floor[a:number])" 
                  {:nest [{:what Floor :where [["floor"]] 
                           :sort [[nil nil nil] [:asc nil nil]] :props [[:number false]]}]}
                  nil nil))
           (is (f "floor.building->u:room (floor[a:number])" 
                  {:nest [{:what Floor :where [["floor"]] 
                           :sort [[nil nil nil] [:asc nil nil]] :props [[:number false]]}]
                   :unique true}
                  nil nil))
           (is (f "floor.building->{a:number}room ({d:name}floor[number])" 
                  {:nest [{:what Floor :where [["floor"]] 
                           :sort ['(:name [:desc nil nil])] :props [[:number false]]}]
                   :sort ['(:number [:asc nil nil])]}
                  nil nil))
           (is (f "floor.building->{a:number}u:room ({d:name}floor[number])" 
                  {:nest [{:what Floor :where [["floor"]] 
                           :sort ['(:name [:desc nil nil])] :props [[:number false]]}]
                   :unique true
                   :sort ['(:number [:asc nil nil])]}
                  nil nil))
         ))


(defmacro create-is [q mom-] `(is (nil? (:remainder (p/parse+ ~q ~mom-)))))

(def qlist
  ^{:doc "Defines list of YZ's queries (used Nest's model).
         DON'T MODIFY IT, because first benchmark for qlist is done.
         Add new queries to qlist-new."}
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
   "@(str dl:`room')"
   "@(str il:`room')"
   "@(str `floor')"
   "@(str de:`floor')"
   "@(str ie:`floor')"
   "@(str `floor' `room')"
   "@(str `floor' de:`room')"
   "@(str `floor' il:`room')"
   "@(str de:`floor' de:`room')"
   "@(str de:`floor' `room')"
   "@(str de:`floor' ie:`room')"
   "@(str ie:`floor' de:`room')"
   "@(str ie:`floor' `room')"
   "@(str ie:`floor' ie:`room')"
   "@(str `floor' 1 `room')"
   "@(str `floor' 2 de:`room')"
   "@(str `floor' 3 ie:`room')"
   "@(str `floor' 1 `room' 3)"
   "@(str `floor' 2 de:`room' 2)"
   "@(str `floor' 3 ie:`room' 1)"
   "@(str `floor' `room' 3)"
   "@(str `floor' de:`room' 2)"
   "@(str `floor' ie:`room' 1)"
   "@(str 1 `floor' `room' 3)"
   "@(str 2 `floor' de:`room' 2)"
   "@(str 3 `floor' ie:`room' 1)"
   "@(str 1 `floor' 4 `room' 3)"
   "@(str 2 `floor' 5 de:`room' 2)"
   "@(str 3 `floor' 6 ie:`room' 1)"

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
   "ni[@(str &)]"
   "ni[name @(str &)]"
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
   "device#(floor.=1), floor, room, building#(floor.=1)"
   "device#(floor.=1), floor#(room.=2), room, building#(floor.=1)"
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
   "{a:number}room ({d:number}floor), {a:name}building"
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


(def qlist-new
  "List with new queries."
  [;; Identical.
   "floor#(number==1)" 
   "floor#(.==1)" 
   "floor#(number==1 && number=2)" 
   "floor#(number==1 && number==2)" 
   "floor#(number=1 && number==2)" 
   "floor#(.==1 && .==2)" 
   "floor#(.==1 && .=2)" 
   "floor#(.=1 && .==2)" 
   "floor#(.==(1 && 2))" 
   "floor#(.==(1 && ==2))" 
   "floor#(.==(1 && =2))" 
   "floor#(.=(1 && ==2))" 
   "floor#(.=(==1 && 2))" 
   "floor#(.=(==1 && ==2))" 
   "building#(name==1)"
   
   ;; Exact class
   "ni^"
   "ni^.device"
   "ni^.device^"
   "ni^ (device)"
   "ni^ (device^)"
   "ni^, device"
   "ni^, device^"
   "ni^.device.room.floor.building"
   "ni^.device^.room.floor.building"
   "ni^.device.room^.floor.building"
   "ni^.device^.room^.floor.building"
   "ni^.device.room.floor^.building"
   "ni^.device.room^.floor^.building"
   "ni^.device^.room^.floor^.building"
   "ni^.device^.room.floor^.building"
   "ni^.device.room.floor.building^"
   "ni^.device.room.floor^.building^"
   "ni^.device.room^.floor^.building^"
   "ni^.device^.room^.floor^.building^"
   "ni^.device^.room.floor^.building^"
   "ni^.device^.room^.floor^.building^"
   "ni^.device^.room^.floor.building^"
   "ni.device^.room^.floor.building^"

   ;; "ALL" modificator
   "floor#(∀number=1)"
   "room#(∀building.name=\"1\")"
   "room#(∀building.floor.room.name=\"1\")"
   "floor#(∀room.number=1)"
   "floor#(number=2 && ∀room.number=1)"
   "floor#(∀room.number=1 && number=2)"
   "floor#(∀room.number=1 && ∀device.name=\"nd\")"
   "floor#(∀room.number=1 && ∀device.name=\"nd\")"
   "floor#(∀room.number=1 || ∀device.name=\"nd\")"
   "floor#(∀room.number=1 || ∀device.name=\"nd\" && ∀li.name=\"ld\")"
   "floor#(all:number=1)"
   "room#(all:building.name=\"1\")"
   "room#(all:building.floor.room.name=\"1\")"
   "floor#(all:room.number=1)"
   "floor#(number=2 && all:room.number=1)"
   "floor#(all:room.number=1 && number=2)"
   "floor#(all:room.number=1 && all:device.name=\"nd\")"
   "floor#(all:room.number=1 && all:device.name=\"nd\")"
   "floor#(all:room.number=1 || all:device.name=\"nd\")"
   "floor#(all:room.number=1 || all:device.name=\"nd\" && all:li.name=\"ld\")"

   ;; Name of a function may contains any character.
   "building[@(clojure.core/count `room')]"
   "building[@(clojure.core/nil? `room')]"
   "building[@(nil? `room')]"

   ;; New function's modificators.
   "@(count `room')"
   "@(count dl:`room')"
   "@(count de:`room')"
   "@(count il:`room')"
   "@(count ie:`room')"
   "building[@(count `room')]"
   "building[@(count dl:`room')]"
   "building[@(count de:`room')]"
   "building[@(count il:`room')]"
   "building[@(count ie:`room')]"

   ;; Subqueries
   "floor#(name = room.name)"
   "floor#(name = room[name])"
   "floor#(name = room.description)"
   "floor#(room = room#(name=\"MB\"))"
   "floor#(name=room.name)"
   "floor#(name = room.name || description=\"SM\")"
   "floor#(name = room.name && description=\"SM\")"
   "floor#(name=room.name || description=\"SM\")"
   "floor#(name=room.name && description=\"SM\")"
   "floor#(name = room.name||description=\"SM\")"
   "floor#(name = room.name&&description=\"SM\")"
   "floor#(name = room.name or description=\"SM\")"
   "floor#(name = room.name and description=\"SM\")"
   "floor#(description=\"SM\" || name = room.name)"
   "floor#(description=\"SM\" && name = room.name)"
   "floor#(name = room.name || description=\"SM\" || name = building.name)"
   "floor#(name = room.name && description=\"SM\" && name = building.name)"
   "floor#(name = room.name || name = building.name)"
   "floor#(name = room.name && name = building.name)"

   ;; Whitespaces
   "room "
   "    room "
   "    room  "
   "room#(name = floor.name )"
   "room#(name = floor.name    )"
   "room#(name = floor.name    )  "
   "room#(name =    floor.name    )  "

   ;; Self objects in predicates
   "floor#(& = 1)"
   "floor#(& = (1 || 2))"
   "floor#(&.name = 1 && & = 1)"
   "floor#(& = 1 && & = 1)"
   "floor#(& = 2 || & = 1)"
   "floor#(&.name = 1 || & = 1)"
   "floor#(&.name = 1 || (& = 1 || & =2))"

   ;; Removed duplicates
   "u:floor"
   "u:floor.room"
   "u:floor.u:room"
   "floor.u:room"
   "u:floor.room.device.building.simpleou"
   "floor.u:room.device.building.simpleou"
   "floor.room.u:device.building.simpleou"
   "floor.room.device.u:building.simpleou"
   "floor.room.device.building.u:simpleou"
   "floor.room.device.u:building.u:simpleou"
   "floor.room.u:device.u:building.u:simpleou"
   "floor.room.u:device.building.u:simpleou"
   "floor.u:room.u:device.building.u:simpleou"
   "floor.u:room.u:device.u:building.u:simpleou"
   "u:floor.u:room.u:device.u:building.u:simpleou"

   "u:room (floor)"
   "room (u:floor)"
   "u:room (u:floor)"
   "u:room (u:floor.device)"
   "room (u:floor.device)"
   "u:room (u:floor.u:device)"
   "u:room (floor.u:device)"
   "room (floor.u:device)"
   "u:room (u:floor.device.building.simpleou)"
   "u:room (u:floor.u:device.building.simpleou)"
   "u:room (u:floor.u:device.u:building.simpleou)"
   "u:room (u:floor.u:device.u:building.u:simpleou)"
   "u:room (u:floor.device.u:building.simpleou)"
   "u:room (u:floor.device.building.u:simpleou)"

   "room (floor (u:device))"
   "room (u:floor (u:device))"
   "u:room (floor (u:device))"
   "u:room (u:floor (u:device))"
   "room (floor (device (u:building)))"

   
   "¹floor"
   "¹floor.room"
   "¹floor.¹room"
   "floor.¹room"
   "¹floor.room.device.building.simpleou"
   "floor.¹room.device.building.simpleou"
   "floor.room.¹device.building.simpleou"
   "floor.room.device.¹building.simpleou"
   "floor.room.device.building.¹simpleou"
   "floor.room.device.¹building.¹simpleou"
   "floor.room.¹device.¹building.¹simpleou"
   "floor.room.¹device.building.¹simpleou"
   "floor.¹room.¹device.building.¹simpleou"
   "floor.¹room.¹device.¹building.¹simpleou"
   "¹floor.¹room.¹device.¹building.¹simpleou"

   "¹room (floor)"
   "room (¹floor)"
   "¹room (¹floor)"
   "¹room (¹floor.device)"
   "room (¹floor.device)"
   "¹room (¹floor.¹device)"
   "¹room (floor.¹device)"
   "room (floor.¹device)"
   "¹room (¹floor.device.building.simpleou)"
   "¹room (¹floor.¹device.building.simpleou)"
   "¹room (¹floor.¹device.¹building.simpleou)"
   "¹room (¹floor.¹device.¹building.¹simpleou)"
   "¹room (¹floor.device.¹building.simpleou)"
   "¹room (¹floor.device.building.¹simpleou)"

   "room (floor (¹device))"
   "room (¹floor (¹device))"
   "¹room (floor (¹device))"
   "¹room (¹floor (¹device))"
   "room (floor (device (¹building)))"

   ;; Sorting and removing duplicates.
   "u:d:room"
   "u:a:room"
   "d:u:room"
   "a:u:room"
   "¹↓room"
   "¹↑room"
   "↑¹room"
   "↓¹room"
   "¹{a:number}room"
   "¹{d:number}room"
   "{a:number}¹room"
   "{d:number}¹room"
   "¹{a:number d:name}room"
   "{d:number a:name}¹room"

   ;; Subqueries and modificators.
   "floor#(name = Ŷ∀room.name)"
   "floor#(name = Ŷ∀room[name])"
   "floor#(name = Ŷ∀room.description)"
   "floor#(room = Ŷ∀room#(name=\"MB\"))"
   "floor#(name = ∀Ŷroom.name)"
   "floor#(name = ∀Ŷroom[name])"
   "floor#(name = ∀Ŷroom.description)"
   "floor#(room = ∀Ŷroom#(name=\"MB\"))"
   "floor#(name = ∀room.name)"
   "floor#(name = ∀room[name])"
   "floor#(name = ∀room.description)"
   "floor#(room = ∀room#(name=\"MB\"))"
   "floor#(name = Ŷroom.name)"
   "floor#(name = Ŷroom[name])"
   "floor#(name = Ŷroom.description)"
   "floor#(room = Ŷroom#(name=\"MB\"))"

   ;; Limits
   "1:floor"
   "-1:floor"
   "1-5:floor"
   "-1-5:floor"
   "floor (1:room)"
   "floor (-1:room)"
   "floor (1-5:room)"
   "floor (-1-5:room)"
   "2:floor (1:room)"
   "1-2:floor (-1:room)"
   "-3:floor (1-5:room)"
   "-4-6:floor (1-5:room)"
   "2:floor (room)"
   "1-2:floor (room)"
   "-3:floor (room)"
   "-4-6:floor (room)"
   "2:floor.1:room"
   "1-2:floor.-1:room"
   "-3:floor.1-5:room"
   "-4-6:floor.1-5:room"
   "2:floor.room"
   "1-2:floor.room"
   "-3:floor.room"
   "-4-6:floor.room"
   "floor.1:room"
   "floor.-1:room"
   "floor.1-5:room"
   "floor.-1-5:room"

   ;; Negative predicates
   "floor#(number != 0)"
   "floor#(number !== 0)"
   "floor#(number !> 0)"
   "floor#(number !< 0)"
   "floor#(number !>= 0)"
   "floor#(number !<= 0)"
   "floor#(number !<= (0 || 1))"
   "floor#(number != (0 && 1))"
   "building#(name !~ \"MB\")"

   ;; Queries with parameters
   "floor#(number=$1)"
   "floor#(number=($1 || $2))"
   "floor#(number=($1 && $2))"
   "floor#(number=($1 && $2 && $3))"
   "floor#(number=($3 && $1 && $2))"
   "floor#(number=$1 && description=$2)"
   "floor#(number=$1 && description=$2 && name=$3)"
   "floor#(number=$1 && description=$2 && name=($3 || $4))"
   "$1"
   "$1 (ni)"
   "$1.ni"
   "$1.$2"
   "$2.$1"
   "$1.$2.ni"
   "$1 ($2.ni)"
   "$1 ($2 (ni))"
   "$1 ($2, ni)"
   "$1.ni.$2"
   "$1 (ni.$2)"
   "$1 (ni ($2))"
   "$1 (ni, $2)"
   "$1.$2.$3"
   "$2.$1.$3"
   "$1 ($2.ni.$3)"
   "$1 ($2 (ni ($3)))"
   "$1 ($2, ni, $3)"
   "$1.ni.$2.$3"
   "building.$1"
   "building.$1.room"

   ;; Query with function without parameters
   "@(str)"
   "building[@(str)]"
   "building[name @(str)]"

   ;; Tests for fixing exception "java.lang.Character cannot be cast to java.lang.String"
   "@(class r)"
   "@(class room)"

   ;; Recursive link
   "*li"
   "li.*li"
   "li.*link"
   "li.*link.cou"
   "li.cou.*parent"
   "li.cou.*parent.cou"
   
   ;; Queries with getting all-medium
   "building->room"
   "building->room (ni)"
   "floor.building->room (ni)"
   "building->u:room"
   "u:building->u:room"
   "u:building->{a:number d:&.}u:room[name]"
   ])


(def list-limit-sorting-unique
  "Defines queries with limiting, sorting and unique options."
  (let [v (for [s ["a:" "d:" "↑" "↓" ""] ; modificators of sorting
                u ["u:" "¹" ""] ; modificators of removing duplicates
                l ["1:" "1-2:" "-1-2:" "-1:" ""]] ; examples of limiting
            (reduce str "" [s u l]))
        floors (map #(str % "floor") v)
        buildings (map #(str % "building") v)
        union (map #(str % ", " %) floors)
        joining (map #(str %1 " (" %2 ")") floors (reverse buildings))
        linking (map #(str %1 "." %2) floors (reverse buildings))]
    (concat floors union joining linking)))


(def clist
  "List with queries from qlist, qlist-new and list-limit-sorting-unique."
  (concat qlist qlist-new))


(def qlist-list
  ^{:doc "Defines list with query-function with parameter (as :list) from qlist"}
  (vec (map #(str "@(str `" % "')") qlist)))

(def qlist-single
  ^{:doc "Defines list with query-function with parameter (as :single) from qlist"}
  (vec (map #(str "@(str de:`" % "')") qlist)))

(def qlist-indep
  ^{:doc "Defines list with query-function with parameter (as :indep) from qlist"}
  (vec (map #(str "@(str ie:`" % "')") qlist)))

(def qlist-prop
  ^{:doc "Defines list with queries which have function as property."}
  (vec (flatten (list 
                  (map #(str "building[& @(str `" % "')]") qlist)
                  (map #(str "building[@(str `" % "') &]") qlist)
                  (map #(str "building[@(str `" % "') & @(str `" % "')]") qlist)
                  (map #(str "building[& @(str `" % "') @(str `" % "')]") qlist)
                  (map #(str "building[@(str `" % "') @(str `" % "') &]") qlist)
                  (map #(str "building[& @(str de:`" % "')]") qlist)
                  (map #(str "building[& @(str ie:`" % "')]") qlist)))))

(def qlist-pred
  ^{:doc "Defines list with queries which have function as predicate."}
  (vec (flatten (list 
                  (map #(str "building#(@(str `" % "') > 3)") qlist)
                  (map #(str "building#(@(str `" % "') < 3)") qlist)
                  (map #(str "building#(@(str `" % "') = @(str de:`" % "'))") qlist)
                  (map #(str "building#(@(str de:`" % "') = @(str ie:`" % "'))") qlist)))))


(def clist-next-query
  ^{:doc "Defines list with queries with the 
         following structure: room, query from clist."}
  (map #(str "room, " %) clist))


(def clist-next-query-clist
  ^{:doc "Defines list with queries with the 
         following structure: query from clist, query from clist."}
  (map #(str % ", " %) clist))


(def clist-nest-query
  ^{:doc "Defines list with queries with the 
         following structure: son (query from clist)."}
  (map #(str "son (" % ")") clist))


(def clist-subqueries
  ^{:doc "Defines list with queries with the 
         following structure: occupancy#(name = query from clist).
         Also modificators (Ŷ and ∀) are used."}
  (concat (map #(str "occupancy#(name = " % ")") clist) 
          (map #(str "occupancy#(name = Ŷ" % ")") clist)
          (map #(str "occupancy#(name = ∀Ŷ" % ")") clist)))


(deftest ^:laborious parse-remainder
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
         (let [results (fn [l mom] 
                         (some #(let [r (try 
                                          (:remainder (p/parse+ % mom))
                                          (catch Exception e (do (.printStackTrace e) %)))]
                                  (if r %)) l))]
           (is (nil? (results clist mom-)))
           (is (nil? (results clist nil)))
           (is (nil? (results clist-next-query mom-)))
           (is (nil? (results clist-next-query-clist mom-)))
           (is (nil? (results clist-nest-query mom-)))
           (is (nil? (results clist-subqueries mom-)))
           (is (nil? (results list-limit-sorting-unique mom-)))))


(deftype SType [property])


(defmacro thr?
  "Evaluates form and returns true 
  in case form is throwned exception 
  exp. False is otherwise."
  [exp form]
  `(try
     (try
       ~form
       false
       (catch ~exp e# true))
     (catch Exception e# false)))


(deftest neg-parse-tests
         ^{:doc "Contains tests which are thrown exceptions."}
         (let [f #(parse % mom-)]
           (is (thrown? NullPointerException (f "(building)")))
           (is (thrown? NullPointerException (f ", building")))
           (is (every? #(thr? SyntaxException (f %)) ["building (room, floor) ni"
                                                      "building (room, floor) (ni)"
                                                      "building (room, floor (ni) (li))"
                                                      "building ni"
                                                      "building#(name = room#(number=1)"
                                                      "building#(floor.∀room.number=1)"
                                                      "building, "
                                                      "building#"]))
           (let [em (yzf/c-em [(->SType "P1")] [SType])]
             (is (thrown? ClassCastException (parse "a:stype" (.getMom em)))))))

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
(deftest ^:laborious pquery-qlist
         ^{:doc "Calling pquery for each query from qlist."}
         (let [mom- (assoc mom- Room
                             (assoc (get mom- Room) 
                                    :sort {:self {:keyfn "#(.getNumber %)"}}))
               results (fn [l] 
                         (some #(let [e (:error (pquery % mom- mem))]
                                  (if e [e %]))
                               l))]
           (is (nil? (results clist)))
           (is (nil? (results clist-subqueries)))
           (is (nil? (results list-limit-sorting-unique)))))

