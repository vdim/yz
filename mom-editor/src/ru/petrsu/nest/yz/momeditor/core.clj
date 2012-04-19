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


(def diameter 
  "Defines deameter of circle."
  60)


(def class-view
  "Implements VertexView interface for representing class as vertex."
  (reify VertexView
    (vertex-bounds 
      [_ r]
      r)
    (vertex-weight 
      [_ _]
      1.0)
    (vertex-anchor 
      [view context other]
      (:location context))
    (constraint-vertex 
      [_ _ location]
      location)
    (render-vertex! 
      [_ _]
      (.drawOval *graphics* 0 0 diameter diameter))))


(defn- random-circles
  ([]
   (random-circles *max-range*))
  ([r]
     (let [half-r (/ r 2)
           ;; Non-uniform: more density for smaller values.
           rand #(Math/pow Math/E (rand (Math/log %)))
           x0 (rand half-r)
           y0 (rand half-r)
           lower (Vecs/vec x0 y0)
           ; 1 is error of next truncation.
           upper (Vecs/vec (+ x0 diameter 1) (+ y0 diameter 1))]
       (Ranges/range lower upper))))


(defn- class-graph
  "Generates connected random graph.
   G(n,p) plus an open path over all vertices."
  [n p]
  (let [vs (vec (repeatedly n random-circles))]
    (reduce (fn [g i]
              (add-context g
                           (vs i)
                           (cons (vs (dec i))
                                 (keep #(if (< (rand) p) (vs %))
                                       (range (dec i))))
                           class-view))
            (add-context nil (vs 0) nil class-view)
            (range 1 n))))


(defn layout-and-show-cg
  ""
  []
  (show-graph
    (update-layout (class-graph 10 1/10))))

(defn viz
  "Visualizes class graph"
  []
  (show-frame)
  (layout-and-show-cg))

