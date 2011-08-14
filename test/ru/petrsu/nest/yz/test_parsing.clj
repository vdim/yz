(ns ru.petrsu.nest.yz.test-parsing
  ^{:author Vyacheslav Dimitrov
    :doc "Tests for parsing functions."}
  (:use ru.petrsu.nest.yz.parsing clojure.contrib.test-is))

(def classes #{Floor, Room, Building})

(deftest t-parse
         ^{:doc "Tests 'parse' function."}
          (is (= (parse "building")
                 [{:what ru.petrsu.nest.son.Building 
                   :props nil
                   :pred nil
                   :then nil
                   :next nil}]))

           (is (= (parse "building (room)")
                 [{:what ru.petrsu.nest.son.Building 
                   :props nil
                   :pred nil
                   :then nil
                   :next [{:what ru.petrsu.nest.son.Room
                           :pred nil
                           :then nil
                           :next nil}]}]))

           (is (= (parse "building, room")
                 [{:what ru.petrsu.nest.son.Building 
                   :props nil
                   :pred nil
                   :then nil
                   :next nil}]))
         
            (is (= (parse "floor.building")
                 [{:what ru.petrsu.nest.son.Building 
                   :props ["building"]
                   :pred nil
                   :then nil
                   :next nil}]))

            (is (= (parse "building.room")
                 [{:what ru.petrsu.nest.son.Building 
                   :props nil
                   :pred nil
                   :then {:what ru.petrsu.nest.son.Room
                          :props nil
                          :pred nil
                          :then nil
                          :next nil}
                   :next nil}])))
