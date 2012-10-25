;;
;; Copyright 2012 Vyacheslav Dimitrov <vyacheslav.dimitrov@gmail.com>
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

(ns ru.petrsu.nest.yz.test-putils
  ^{:author "Vyacheslav Dimitrov"
    :doc "Tests util functions for parsing."}
  (:use clojure.test)
  (:require [ru.petrsu.nest.yz.parsing :as p]))


(def some-v
  ^{:doc "Defines vector with single empty map."}
  [{:what nil
   :then nil
   :nest nil}])


(deftest t-get-in-nest
         ^{:doc "Tests the 'get-in-nest' function"}
         (is (nil? (p/get-in-nest some-v 0 :what)))
         (is (= "1" (p/get-in-nest [(assoc (some-v 0) :what "1")] 0 :what)))
         (let [some-vv (p/assoc-in-nest some-v 0 :nest some-v)
               some-vvv (p/assoc-in-nest some-vv 1 :nest some-v)
               some-vvvv (p/assoc-in-nest some-vvv 2 :nest some-v)]
           (is (= "2" (p/get-in-nest (p/assoc-in-nest some-vv 1 :what "2") 1 :what)))
           (is (= "3" (p/get-in-nest (p/assoc-in-nest some-vvv 2 :what "3") 2 :what)))
           (is (= "4" (p/get-in-nest (p/assoc-in-nest some-vvvv 3 :what "4") 3 :what)))))


(deftest t-get-in-nest-or-then
         ^{:doc "Tests the 'get-in-nest-or-then' function"}
         (let [some-v [{:what 1 :then nil}]]
           (is (= 1 (p/get-in-nest-or-then some-v 0 0 :what)))
           (is (= 1 (p/get-in-nest-or-then some-v 0 1 :what)))
           (is (nil? (p/get-in-nest-or-then some-v 1 1 :what))))
         (let [some-v [{:what 1 :then {:what 2}}]]
           (is (= 1 (p/get-in-nest-or-then some-v 0 0 :what)))
           (is (= 2 (p/get-in-nest-or-then some-v 0 1 :what)))
           (is (nil? (p/get-in-nest-or-then some-v 0 2 :what))))
         (let [some-v [{:what 1 :then {:what 2 :then {:what 3}}}]]
           (is (= 2 (p/get-in-nest-or-then some-v 0 1 :what)))
           (is (= 3 (p/get-in-nest-or-then some-v 0 2 :what)))
           (is (nil? (p/get-in-nest-or-then some-v 0 3 :what))))
         (let [some-v [{:what 1 :then {:what 2 :then {:what 3}} 
                        :nest [{:what 4 :then nil}]}]]
           (is (= 4 (p/get-in-nest-or-then some-v 1 0 :what)))
           (is (= 1 (p/get-in-nest-or-then some-v 0 0 :what)))
           (is (= 4 (p/get-in-nest-or-then some-v 1 1 :what)))
           (is (= 4 (p/get-in-nest-or-then some-v 1 2 :what))))
         (let [some-v [{:what 1 :then {:what 2 :then {:what 3}} :nest [{:what 4 :then {:what 5 :then nil}}]}]]
           (is (= 4 (p/get-in-nest-or-then some-v 1 0 :what)))
           (is (= 5 (p/get-in-nest-or-then some-v 1 1 :what)))
           (is (nil? (p/get-in-nest-or-then some-v 1 2 :what)))))
