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

(ns ru.petrsu.nest.yz.momeditor.init-repl
  ^{:author "Vyacheslav Dimitrov"
    :doc "Code for starting REPL."}
  (:use ru.petrsu.nest.yz.momeditor.core
        clojure.repl clojure.pprint clojure.java.io))

(def son-classes
  [ru.petrsu.nest.son.jpa.SonElement 
   ru.petrsu.nest.son.jpa.SON 
   ru.petrsu.nest.son.jpa.Occupancy 
   ru.petrsu.nest.son.jpa.Device 
   ru.petrsu.nest.son.jpa.LinkInterface 
   ru.petrsu.nest.son.jpa.EthernetInterface 
   ru.petrsu.nest.son.jpa.VLANInterface 
   ru.petrsu.nest.son.jpa.NetworkInterface 
   ru.petrsu.nest.son.jpa.IPv4Interface 
   ru.petrsu.nest.son.jpa.Network 
   ru.petrsu.nest.son.jpa.IPNetwork 
   ru.petrsu.nest.son.jpa.AbstractOU 
   ru.petrsu.nest.son.jpa.SimpleOU 
   ru.petrsu.nest.son.jpa.CompositeOU 
   ru.petrsu.nest.son.jpa.Room 
   ru.petrsu.nest.son.jpa.Floor 
   ru.petrsu.nest.son.jpa.Building])


(def uni-model
  [university.model.Faculty
   university.model.Course
   university.model.Student])
