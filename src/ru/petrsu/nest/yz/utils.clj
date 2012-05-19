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


(ns ru.petrsu.nest.yz.utils
  ^{:author "Vyacheslav Dimitrov"
    :doc "Namespace for common functions."}
  (:import (ru.petrsu.nest.yz DefaultProperty)
           (java.lang.annotation Annotation)
           (java.lang.reflect Field)))


(defn get-short-name
  "Return a short name for the specified class. 
  The short is a set of upper letters from the simple name of the class."
  [cl]
  (.toLowerCase (reduce str ""
                        (for [a (.getSimpleName cl) 
                              :when (< (int a) (int \a))] a))))


(defn dp
  "Returns name of field (of specified class cl) which 
  is marked by the DefaultProperty annotation."
  [^Class cl] 
  (keyword
    (some (fn [^Field field] 
            (if (some (fn [^Annotation ann] 
                        (= DefaultProperty (.annotationType ann)))
                      (.getDeclaredAnnotations field))
              (.getName field)))
          (.getDeclaredFields cl))))


(defn descriptors
  "Returns list of PropertyDescriptors for specified class."
  [^Class clazz]
  (seq (.. java.beans.Introspector 
         (getBeanInfo clazz) 
         (getPropertyDescriptors))))
