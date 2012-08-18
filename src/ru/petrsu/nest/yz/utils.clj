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
  (:import (ru.petrsu.nest.yz DefaultProperty NotFoundPathException)
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


(defn yz-compare
  "YZ's version of the compare function.
  Arguments: 
    f - comparing sign (>, <, >=, <=). Must be string.
    arg1, arg2 - arguments for comparing."
  [f arg1 arg2]
  (let [r (compare arg1 arg2)]
    (cond
      ; equal to
      (zero? r) (or (= f ">=") (= f "<="))
      ; greather than
      (pos? r) (or (= f ">=") (= f ">"))
      ; less than 
      :else (or (= f "<=") (= f "<")))))


(defn intersection
  "Returns intersection of two vectors comparing by two elements.
  Arguments must the following recursive structure: 
    [el1 [el11 [el111 [] el112 [] ...] el12 [el121 [] ...]] el2 [el21 [] ...]]"
  [v1 v2]
  (let [v1 (partition 2 v1)
        v2 (partition 2 v2)
        r (for [v11 v1 v22 v2 :when (and (= (first v11) (first v22)) 
                                         (let [s1 (second v11)
                                               s2 (second v22)]
                                           (or (and (empty? s1) (empty? s2))
                                               (let [i (intersection s1 s2)]
                                               (and (= i s1) (= i s2))))))]
            v11)]
    (vec (reduce #(conj %1 (first %2) (second %2)) [] r))))


(defn union
  "Returns union of two vectors. Note: the function 
  clojure.set/union does not fit because of order of
  conjunction depends from count of elements in this
  function."
  [v1 v2]
  (reduce conj v1 v2))


(defn get-paths
  "Returns list of paths beetwen cl-target and cl-source.
  The search is based on a MOM (Map Of Model)."
  [^Class cl-target, ^Class cl-source mom]
  (if (or (nil? cl-target) (nil? cl-source)
          (= :not-specified cl-target) (= :not-specified cl-source)
          (not (class? cl-target)) (not (class? cl-source)))
    nil
    (loop [cl- cl-target]
      (let [paths (get (get mom cl-source) cl-)]
        (if (empty? paths)
          (if (nil? cl-)
            (let [paths (some #(let [ps (get (get mom cl-source) %)]
                                 (if (not (empty? ps)) ps)) 
                              (ancestors cl-target)) 
                  children (get-in mom [:children cl-source])
                  ; In case there are paths between children and cl-target
                  ; we return map where child -> paths between this child and
                  ; cl-target.
                  paths (if (empty? paths)
                          (reduce #(let [; path from child to cl-target
                                         p (get (get mom %2) cl-target)]
                                     (if p
                                       (assoc %1 %2 p)
                                       %1))
                                  {}
                                  children)
                          paths)]
              (if (empty? paths)
                (if mom
                  (throw (NotFoundPathException. (str "Not found path between " cl-source " and " cl-target "."))))
                paths))
            (recur (:superclass (get mom cl-))))
          paths)))))
