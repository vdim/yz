(ns ru.petrsu.nest.yz.parsing
  ^{:author Vyacheslav Dimitrov
    :doc "Code for the parsing of queries does due to the fnparse library."}
  (:use name.choi.joshua.fnparse)
  (:require [clojure.string :as cs]))

(declare find-class)

; The parsing state data structure. The rest of input string is stored
; in :remainder, vector of maps is stored in :result, and the map of
; the object model is stared in :mom.
(defstruct q-representation :remainder :result :mom)

; Rules of grammar are below. See BNF in the begining of file.
(def alpha
  ^{:doc "Sequence of characters."}
  (lit-alt-seq "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"))


(def whitespaces
  ^{:doc "List of whitespaces"}
  (rep+ (alt (lit \space) (lit \newline) (lit \tab))))


(defmacro sur-by-ws
  "Surrounds 'rule' by whitespaces like this
  (conc (opt whitespaces) rule (opt whitespaces))."
  [rule]
  `(conc (opt whitespaces) ~rule (opt whitespaces)))


(def id 
  (complex [id# (rep+ alpha)
            g-res (get-info :result)
            g-mom (get-info :mom)
            _ (set-info :result [(assoc (g-res 0) :what (find-class (reduce str id#), g-mom))])]
           id#))


(def delimiter
  ^{:doc "Define delimiter of ids: there are comma (for queries) 
  and point (for property or link and so on)."}
  (alt (lit \.) (sur-by-ws (lit \,))))


(def query
  (conc id (rep* (conc delimiter id))))


(def empty-res
  ^{:doc "Defines vector within one empty map. 
         The vector is initial result of 'parse' function."}
  [{:what nil
   :props nil
   :pred nil
   :then nil
   :next nil}])


(defn parse
  "Parses specified query ('q') on YZ language based on
  specified ('mom') the map of the object model."
  [q, mom]
  (:result ((query (struct q-representation (seq q) empty-res mom)) 1)))


(defn find-class
  [id mom]
  "Returns class which is correspended 'id' (search is did in 'mom'). 
  Search is did in the following positions:
    - as full name of class (package+name)
    - as name of class 
    - in 'sn' key of each map of mom.
    - as abbreviation class's name."
  (some (fn [el] (let [[cl m] el, b (bean cl), l-id (cs/lower-case id)]
                   (if (or (= l-id (cs/lower-case (:name b))) 
                           (.startsWith (cs/lower-case (:simpleName b)) l-id)
                           (= l-id (cs/lower-case (:sn m))))
                     cl))) 
        mom))
