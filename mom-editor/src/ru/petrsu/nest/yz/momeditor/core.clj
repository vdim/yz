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

(ns ru.petrsu.nest.yz.momeditor.core
  ^{:author "Vyacheslav Dimitrov"
    :doc "MOM's editor core."}
  (:use
   (net.kryshen.indygraph partition spring-layout render inspector)
   (net.kryshen.indyvon core layers component))
  (:import
   (net.kryshen.dvec Vecs Ranges)))

; Describe class as vertex.
(defrecord Clazz 
  [bounds label clazz])

(def diameter 
  "Defines deameter of circle."
  60)


(defn- check-type
  "Defines whether type of pd (PropertyDescription) is contained in list of classes."
  [pd classes]
  (let [pt (.getPropertyType pd)]
    (if (contains? classes pt)
      [pt (.getName pd)]
      (cond (nil? pt) nil
            (contains? (ancestors pt) java.util.Collection)
            (let [t (vec (.. pd getReadMethod getGenericReturnType getActualTypeArguments))]
              (if (and (> (count t) 0) (contains? classes (t 0)))
                [(t 0), (.getName pd)] ))
            (.isArray pt)
            (let [t (.getComponentType pt)]
              (if (and (not (nil? t)) (contains? classes t))
                [t, (.getName pd)]))))))


(defn get-related
  "Returns related classes for specified Class's instance.
  ('classes' must be set for correct working 'contains?' function."
  [cl classes]
  (remove nil?
    (map #(check-type % classes)
         (seq (.. java.beans.Introspector
                (getBeanInfo cl)
                (getPropertyDescriptors))))))


(def class-view
  "Implements VertexView interface for representing class as vertex."
  (reify VertexView
    (vertex-bounds 
      [_ clazz]
      (:bounds clazz))
    (vertex-weight 
      [_ _]
      1.0)
    (vertex-anchor 
      [view context other]
      (:center context))
    (constraint-vertex 
      [_ _ location]
      location)
    (render-vertex! 
      [_ clazz]
      (.drawString *graphics* (:label clazz) 30 30)
      (.drawOval *graphics* 0 0 diameter diameter))))


(defn- class-vertex
  "Creates Clazz instance for specified class."
  [clazz]
  (let [x0 0
        y0 0
        lower (Vecs/vec x0 y0)
        ; 1 is for eliminating possible error of futher truncation.
        upper (Vecs/vec (+ x0 diameter 1) (+ y0 diameter 1))]
    (->Clazz (Ranges/range lower upper) 
             (subs (.getSimpleName clazz) 0 3)
             clazz)))


(defn- class-graph
  "Generates graph from vector of classes."
  [classes]
  (let [vs (map class-vertex classes)
        index (reduce (fn [m [k v]] (assoc m v k)) {} (map-indexed vector vs))
        vs (map #(assoc % :adjacent (keep (fn [[cl property]] (some (fn [cl-v] (if (= (:clazz cl-v) cl) cl-v)) vs))
                                         (get-related (:clazz %) (set classes)))) vs)]
    (reduce (fn [g clazz]
              (add-context g
                           clazz
                           (:adjacent clazz)
                           class-view))
            (->Layout [] index nil 0.0 0.0 0 0.0 true)
            vs)))


(defn layout-and-show-cg
  ""
  [classes]
  (show-graph
    (last (take-while (complement completed?)
                      (iterate update-layout
                               (class-graph classes))))))

(defn viz
  "Visualizes class graph"
  [classes]
  (show-frame)
  (layout-and-show-cg classes))
