(ns ru.petrsu.nest.yz.parsing
  ^{:author Vyacheslav Dimitrov
    :doc "Code for the parsing of queries does due to the fnparse library."}
  (:use name.choi.joshua.fnparse)
  (:require [clojure.string :as cs]
            [ru.petrsu.nest.yz.map-utils :as mu]))

(declare found-id)

; The parsing state data structure. 
(defstruct q-representation 
           :remainder ; The rest of input string
           :result ; vector of maps
           :mom ; The map of the object model some area
           :then-level ; then level, the nubmer of dots.
           :numq  ; The number of query.
           :nest-level ; Nest level, level of query (the number of parentthesis).
           :is-then)  ; Defines whether id is came from '.'.

; Helper macros
(defmacro sur-by-ws
  "Surrounds 'rule' by whitespaces like this
  (conc (opt whitespaces) rule (opt whitespaces))."
  [rule]
  `(conc (opt whitespaces) ~rule (opt whitespaces)))


(defmacro change-level
  "Generates code for changing 'key-level' due to function 'f'."
  [ch key-level f]
  `(complex [ret# (lit ~ch),
            then-level# (get-info ~key-level)
            _# (set-info ~key-level (~f then-level#))]
           ret#))


; Rules of grammar are below. See BNF in the begining of file.
(def alpha
  ^{:doc "Sequence of characters."}
  (lit-alt-seq "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"))


(def whitespaces
  ^{:doc "List of whitespaces"}
  (rep+ (alt (lit \space) (lit \newline) (lit \tab))))


(def id 
  (complex [id# (rep+ alpha)
            g-res (get-info :result)
            g-mom (get-info :mom)
            tl (get-info :then-level)
            nl (get-info :nest-level)
            numq (get-info :numq)
            is-level (get-info :is-then)
            _ (set-info :result (found-id g-res g-mom tl nl numq is-level (reduce str id#)))
            _ (set-info :is-then false)]
           id#))


(def delimiter
  ^{:doc "Defines delimiter of ids: there are comma (for queries) 
  and point (for property or link and so on)."}
  (alt (invisi-conc (change-level \. :then-level inc) (set-info :is-then true))
       (sur-by-ws (change-level \, :numq inc))))


(declare query)
(def nest-query
  ^{:doc "Defines nested query"}
  (conc (sur-by-ws (invisi-conc (change-level \( :nest-level inc)
                                (set-info :then-level 0))) 
        query 
        (sur-by-ws (change-level \) :nest-level dec))))

(def query
  (conc id (opt (conc delimiter query)) (opt nest-query)))


(def empty-res
  ^{:doc "Defines vector within one empty map. 
         The vector is initial result of 'parse' function."}
  [{:what nil
   :props nil
   :pred nil
   :then nil
   :nest nil}])


(defn parse
  "Parses specified query ('q') on YZ language based on
  specified ('mom') the map of the object model."
  [q, mom]
;  (:result ((query (struct q-representation (seq q) empty-res mom 0 0)) 1)))
  ((query (struct q-representation (seq q) empty-res mom 0 0 0 false)) 1))


;; Helper functions are below.

(defn find-class
  "Returns class which is correspended 'id' (search is did in 'mom'). 
  Search is did in the following positions:
    - as full name of class (package+name)
    - as name of class 
    - in 'sn' key of each map of mom.
    - as abbreviation class's name."
  [id mom]
  (some (fn [el] (let [[cl m] el, b (bean cl), l-id (cs/lower-case id)]
                   (if (or (= l-id (cs/lower-case (:name b))) 
                           (.startsWith (cs/lower-case (:simpleName b)) l-id)
                           (= l-id (cs/lower-case (:sn m))))
                     cl))) 
        mom))

(defn find-prop
  "Returns true if 'prop' is property of 'cl'"
  [cl prop mom]
  (contains? (set (:properties (get mom cl))) prop))

(defn get-proper-m
  [res tl nl numq is-then]
  (if (= nl 0) 
    (res numq)))

(defn found-id
  "This function is called when id is found in query. Returns new result."
  [res mom tl nl numq id is-then]
  (if-let [cl (find-class id, mom) m (get-proper-m (last res) tl nl numq is-then)]
    [(assoc (res 0) :what cl)]
    (throw (Exception. (str "Not found id: " id)))))
