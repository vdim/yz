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

(ns ru.petrsu.nest.yz.test-hb-utils
  ^{:author "Vyacheslav Dimitrov"
    :doc "Tests for functions which generate MOM."}
  (:use ru.petrsu.nest.yz.hb-utils clojure.test)
  (:import (ru.petrsu.nest.son Floor Room Building)))

(def classes #{Floor, Room, Building})

(deftest t-get-paths
         ^{:doc "Tests get-paths function."}
          (is (= (first (get-paths Room Building classes))
                 {:path [ru.petrsu.nest.son.Room ru.petrsu.nest.son.Floor ru.petrsu.nest.son.Building], 
                  :ppath ["floor" "building"]}))
          (is (= (first (get-paths Floor Building classes))
                 {:path [ru.petrsu.nest.son.Floor ru.petrsu.nest.son.Building], 
                  :ppath ["building"]}))
          (is (= (first (get-paths Building Floor classes))
                 {:path [ru.petrsu.nest.son.Building ru.petrsu.nest.son.Floor], 
                  :ppath ["floors"]})))

(deftest t-get-short-name
         ^{:doc "Tests 'get-short-name' function."}
         (is (= "f" (get-short-name Floor)))
         (is (= "b" (get-short-name Building)))
         (is (= "ni" (get-short-name ru.petrsu.nest.son.NetworkInterface)))
         (is (= "ip4i" (get-short-name ru.petrsu.nest.son.IPv4Interface))))

