(ns ru.petrsu.nest.yz.test-map-utils
  ^{:author Vyacheslav Dimitrov
    :doc "Tests for MOM's utils."}
  (:use ru.petrsu.nest.yz.map-utils clojure.contrib.test-is))

(def mom {:a {:sn "sn1", :dp "dp1"}, :b {:sn "sn2" :dp, "dp2"}})

(deftest t-find-in-sn
         ^{:doc "Tests find-by-sn function"}
         (is (= (find-by-sn "sn1" mom) :a))
         (is (= (find-by-sn "sn2" mom) :b))
         (is (= (find-by-sn "sn3" mom) nil)))


(def nest-mom {ru.petrsu.nest.son.Room {:sn "r", :dp "number"}, 
               ru.petrsu.nest.son.Floor {:sn "f" :dp, "number"}})

(deftest t-find-by-sn-nest
         ^{:doc "Tests find-by-sn function for nest MOM."}
         (is (= (find-by-sn "r" nest-mom) ru.petrsu.nest.son.Room))
         (is (= (find-by-sn "f" nest-mom) ru.petrsu.nest.son.Floor))
         (is (= (find-by-sn "s" nest-mom) nil)))


(def empty-map
  {:what nil
   :then nil
   :nest nil})

(deftest t-assoc-in*
         ^{:doc "Tests assoc-in* macro."}
         (is (= (assoc-in* empty-map :nest 0 :what "someval") 
                {:what "someval" :then nil :nest nil}))
         (let [m (assoc empty-map :nest empty-map) 
               mm (assoc-in m [:nest :nest] empty-map)] 
           (is (= (assoc-in* m :nest 1 :nest "someval") 
                  {:what nil :then nil :nest {:what nil :then nil :nest "someval"}}))
           (is (= (assoc-in* m :nest 0 :what "someval") 
                  {:what "someval" :then nil :nest {:what nil :then nil :nest nil}}))
           (is (= (assoc-in* m :nest 1 :what "someval") 
                  {:what nil :then nil :nest {:what "someval" :then nil :nest nil}}))
           (is (= (assoc-in* mm :nest 2 :what "someval") 
                  {:what nil :then nil :nest {:what nil :then nil :nest {:what "someval" :then nil :nest nil}}}))))
