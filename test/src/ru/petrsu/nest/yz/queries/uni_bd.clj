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

(ns ru.petrsu.nest.yz.queries.uni-bd
  ^{:author "Vyacheslav Dimitrov"
    :doc "University DB."}
  (:require [ru.petrsu.nest.yz.yz-factory :as yzf])
  (:import (university.model Student Course Faculty)))

;; Faculty
(def marry (doto (Faculty.) (.setName "Marry") (.setOffice "101")))
(def david (doto (Faculty.) (.setName "David") (.setOffice "101")))
(def brian (doto (Faculty.) (.setName "Brian") (.setOffice "102")))
(def bob-f (doto (Faculty.) (.setName "Bob") (.setOffice "200")))

;; Students
(def alex (doto (Student.) (.setName "Alexander") (.setID "1")))
(def nik (doto (Student.) (.setName "Nik") (.setID "2")))
(def john (doto (Student.) (.setName "John") (.setID "3")))
(def bob (doto (Student.) (.setName "Bob") (.setID "4")))

;; Courses
(def alg (doto (Course.) (.setCode "100") (.setTitle "Algebra") 
           (.setFaculty marry) (.addStudent alex) 
           (.addStudent nik) (.addStudent john)))

(def geo (doto (Course.) (.setCode "101") (.setTitle "Geometry") 
           (.setFaculty david) (.addStudent nik) (.addStudent john)))

(def rus (doto (Course.) (.setCode "200") (.setTitle "Russian") 
           (.setFaculty brian) (.addStudent john) (.addStudent bob)))

;; Define collections
(def students [alex nik john bob])
(def faculty [marry david brian bob-f])
(def courses [alg geo rus])

;; MultiCollectionManager
(def uni-em (yzf/mc-em courses [Course Student Faculty]))

