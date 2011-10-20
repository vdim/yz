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

(ns ru.petrsu.nest.yz.parsing
  ^{:author "Vyacheslav Dimitrov"
    :doc "Code for the parsing of queries (due to the fnparse library)."}
  (:use name.choi.joshua.fnparse)
  (:use ru.petrsu.nest.yz.functions)
  (:require [clojure.string :as cs])
  (:import (java.util.regex Pattern)))

(defn ^String sdrop
  "Drops first n characters from s.  Returns an empty string if n is
   greater than the length of s."
  [n ^String s]
  (if (< (count s) n)
    ""
    (.substring s n)))


; The parsing state data structure. 
(defstruct q-representation 
           :remainder ; The rest of input string
           :result ; vector of maps
           :mom ; The map of the object model some area
           :then-level ; then level, the nubmer of dots.
           :nest-level ; Nest level, level of query (the number of parentthesis).
           :preds ; The vector within current predicates structure.
           :f-modificator ; Modificator of function's param.
           :function ; Describe current function.
           :is-recur ; Defines whether property is recur.
           :cur-pred) ; Current predicate.

;; Helper macros, definitions and functions.

(def empty-res
  ^{:doc "Defines vector within one empty map. 
         The vector is initial result of 'parse' function."}
  [{:what nil
   :props []
   :preds nil
   :then nil
   :nest nil}])

(def empty-then
  ^{:doc "Defines then structure"}
  {:what nil
   :props []
   :preds nil
   :then nil})

(def empty-pred
  ^{:doc "Defines pred structure"}
  {:ids []
   :func nil
   :value nil})

(def empty-fun
  ^{:doc "Defines function structure"}
  {:func nil ; Function (:func has clojure.lang.Var type.)
   :params []}) ; Vector with parameters.


(defmacro sur-by-ws
  "Surrounds 'rule' by whitespaces like this
  (conc (opt whitespaces) rule (opt whitespaces))."
  [rule]
  `(conc (opt whitespaces) ~rule (opt whitespaces)))

(defn assoc-in-nest
  "Like assoc-in, but takes into account structure :result.
  Inserts some value 'v' in 'res' map to :nest key."
  [res nest-level l-tag v]
  (if (<= nest-level 0)
    (conj (vec (butlast res)) (assoc (last res) l-tag v))
    (conj (vec (butlast res)) 
           (assoc (last res) 
                  :nest 
                  (assoc-in-nest (:nest (last res)) (dec nest-level) l-tag v)))))


(defn add-value
  "Conjs some value 'v' to :nest array which has nest-level 
  of level."
  [res nest-level v]
  (if (= nest-level 0)
    (conj res v)
    (conj (vec (butlast res)) 
           (assoc (last res) 
                  :nest 
                  (add-value (:nest (last res)) (dec nest-level) v)))))


(defn get-in-nest
  "Like get-in, but takes into account structure of :result
  field of q-representation structure. First :nest key is
  nest-level times, then k is."
  [res nest-level k]
  (loop [res- res nl nest-level]
    (if (= nl 0)
      (get (last res-) k)
      (recur (:nest (last res-)) (dec nl)))))


(defn get-in-then
  "Gets value from res due to nl and tl"
  [res nl tl k]
  (if (= tl 0) 
    (get-in-nest res nl k) 
    (get-in (get-in-nest res nl :then) (conj (vec (repeat (dec tl) :then)) k))))


(declare tr-pred)
(defn change-preds
  "Changed ':preds' and 'result' of the state."
  [state]
  [nil, (let [res (:result state)
              nl (:nest-level state)
              tl (:then-level state)
              st (:preds state)]
         (assoc (assoc state :preds []) :result 
                (if (= tl 0) 
                  (assoc-in-nest res nl :preds st)
                  (let [last-then (get-in-nest res nl :then)]
                    (assoc-in-nest res nl :then 
                                   (assoc-in last-then 
                                             (conj (vec (repeat (dec tl) :then)) :preds) 
                                             st))))))])


(defn add-pred
  "Conjs empty-pred value to current vector :preds in q-representation.
  If 'f' is supplied then value is call of 'f' with :preds as parameter."
  ([rule]
   (add-pred rule empty-pred))
  ([rule f]
   (complex [cur_pred (get-info :cur-pred)
             ret (invisi-conc rule (update-info :preds #(conj % (if (nil? f) cur_pred f))))]
            ret)))


(declare find-class, find-prop)
(defn- get-path
  "Returns path from cl-source to class of id
  (search based on the mom.)"
  [id, cl-source, mom]
  (if-let [cl-target (find-class id mom)]
    (let [paths (get (get mom cl-source) cl-target)]
      (if (empty? paths)
        (throw (Exception. (str "Not found id: " id)))
        (nth paths 0)))
    (if (find-prop cl-source id, mom)
      [id]
      (throw (Exception. (str "Not found id: " id " which must be beloged to " cl-source))))))


(defn- get-ids 
  "Returns new value of the :ids key of the pred structure."
  [ids res mom cl]
  (let [sp-res (cs/split res #"\.")]
    (loop [cl- cl, ids- ids, sp-res- sp-res]
      (if (empty? sp-res-)
        ids-
        (recur (find-class (first sp-res-) mom) 
               (vec (flatten (conj ids- (get-path (first sp-res-) cl- mom))))
               (rest sp-res-))))))


(defn change-pred
  "Changes :preds of q-presentation by setting key 'k' to
  the return value of 'rule'. If 'value' is supplied then
  value of the 'k' is set to 'value' (This is need for 
  true, false, nil and so on special values.). "
  ([rule, k]
   (change-pred rule, k, :not-value))
  ([rule, k, value]
   (complex [ret rule
             mom (get-info :mom)
             res (get-info :result)
             nl (get-info :nest-level)
             tl (get-info :then-level)
             preds (get-info :preds)
             _ (update-info :cur-pred #(if (nil? (last preds)) % (last preds)))
             _ (update-info 
                 :preds 
                 #(conj (pop %) 
                        (assoc (peek %) 
                               k 
                               (let [res- (if (seq? ret) (cs/trim (reduce str (flatten ret))) ret)]
                                 (cond (not= value :not-value) value
                                       (= k :ids) (get-ids (:ids (peek %)) res- mom (get-in-then res nl tl :what))
                                       (and (= k :func) (= (cs/trim res-) "!=")) "not="
                                       :else res-)))))]
              ret)))


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
                           (= l-id (cs/lower-case (:simpleName b)))
;                           (.startsWith (cs/lower-case (:simpleName b)) l-id)
                           (= l-id (cs/lower-case (:sn m))))
                     cl))) 
        mom))


(defn find-prop
  "Returns true if 'prop' is property of 'cl'"
  [cl prop mom]
  (contains? (set (:properties (get mom cl))) prop))


(defn found-prop
  "This function is called when likely prop is found"
  [res mom id nl tl is-recur]
  (let [tl- (dec tl)
        last-then (get-in-nest res nl :then)]
    (if (or (= id \&) (map? id) (find-prop (get-in-then res nl tl- :what) id mom))
      (if (> tl- 0)
        (assoc-in-nest res nl :then (update-in last-then 
                                               (conj (vec (repeat (dec tl-) :then)) :props) 
                                               #(conj % [id is-recur])))
        (assoc-in-nest res nl :props (conj (get-in-nest res nl :props) [id is-recur])))
      (throw (Exception. (str "Not found id: " id))))))


(defn found-id
  "This function is called when id is found in query. Returns new result."
  [res mom id nl tl is-recur]
  (let [cl (find-class id, mom)
        last-then (get-in-nest res nl :then)
        tl- (dec tl)]
    (if (nil? cl)
      (found-prop res mom id nl tl is-recur)
      (if (> tl 0)
        (if (nil? last-then)
          (assoc-in-nest res nl :then (assoc empty-then :what cl))
          (assoc-in-nest res nl :then (assoc-in last-then (repeat tl- :then) (assoc empty-then :what cl))))
        (assoc-in-nest res nl :what cl)))))


(defn update-preds
  [op]
  (update-info :preds #(conj % op)))


(defn update-param
  "Updates :params key of :function key of the q-representation structure."
  [value]
  (update-info :function #(let [f (peek %)] 
                            (conj (pop %) 
                                  (assoc f 
                                         :params 
                                         (conj (:params f) value))))))


(defn text
  "Rule for recognizing any string."
  [ch state]
  (let [remainder (reduce str (:remainder state))
        res (for [a (:remainder state) :while (not (= a ch))] a)]
    [(reduce str res) 
     (assoc state :remainder (sdrop (count res) remainder))]))


(def limit-nq
  ^{:doc "Defines number of the nested queries into parameter as query."}
  (identity 100))

(declare single-pq, list-pq, indep-pq, end-pq)
(defn textq
  "Recognizes text of query which is parameter of function.
  textq restricts count of nested queries, because in case where
  user input an incorrect query (with starting modificator and without
  ending modificator), then infinite loop is occured."
  [state]
  (loop [res "" remainder (:remainder state) end-c 0 count-nq 0]
    (let [ch (first remainder)
          c (cond (or (= ch single-pq) (= ch list-pq) (= ch indep-pq)) (inc end-c)
                  (= ch end-pq) (dec end-c)
                  :else end-c)]
      (cond (> count-nq limit-nq) (throw (Exception. "Limit of nested queries is exceeded."))
            (and (= ch end-pq) (= c -1)) [res (assoc state :remainder 
                                                     (sdrop (count res) (reduce str (:remainder state))))]
            :else (recur (str res ch) (rest remainder) c (inc count-nq))))))


(defmacro pfunction
  "Helper function for reduced code for processing function."
  [pf up]
  `(complex [ret# function
             f# (get-info :function)
             _# (~pf f#) 
             _# ~up]
            ret#))









;; Rules of grammar are below. See BNF in the begining of file.

(def alpha
  ^{:doc "Sequence of characters."}
  (lit-alt-seq "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ123456780&+/-*:"))

(def digit
  ^{:doc "Sequence of digits."}
  (lit-alt-seq "1234567890."))

(def number
  ^{:doc "Defines number."}
  (conc (opt (alt (lit \+) (lit \-))) (rep+ digit)))

(def whitespaces
  ^{:doc "List of whitespaces"}
  (rep+ (alt (lit \space) (lit \newline) (lit \tab))))

(def end-pq
  ^{:doc "Defines symbol for indication 
         end query which is parameter."}
  (identity \'))

(def list-pq
  ^{:doc "Defines symbol for indication 
         query-parameter which is passed as list."}
  (identity \`))

(def single-pq
  ^{:doc "Defines symbol for indication 
         query-parameter for which function is called
         for each tuple."}
  (identity \$))

(def indep-pq
  ^{:doc "Defines symbol for indication 
         query-parameter which is independence
         from the rest of query."}
  (identity \%))


(def string
  ^{:doc "Defines string"}
  (conc (lit \") (partial text \") (lit \")))


(defn set-id
  [id, f, state]
  [(:remainder state) 
   (assoc state :result 
          (f (:result state) 
             (:mom state)
             id
             (:nest-level state)
             (:then-level state)
             (:is-recur state)))])

(defmacro process-id
  "Processes some id due to functions 'f'"
  [f]
  `(complex [id# (rep+ alpha)
             _# (partial set-id (reduce str id#) ~f)]
            id#))


(def id (process-id found-id))

(def delimiter
  ^{:doc "Defines delimiter of ids: there are comma (for queries) 
  and dot (for property or link and so on)."}
  (alt
       (invisi-conc (lit \.) (update-info :then-level inc))
       (complex [ret (sur-by-ws (lit \,)) 
                 res (get-info :result)
                 nl (get-info :nest-level)
                 _ (set-info :result (add-value res nl (empty-res 0)))
                 _ (set-info :then-level 0)]
                ret)))


(declare query, function)
(def nest-query
  ^{:doc "Defines nested query"}
  (conc (sur-by-ws (complex [ret (invisi-conc (lit\() (update-info :nest-level inc))
                             nl (get-info :nest-level)
                             _ (update-info :result #(assoc-in-nest % (dec nl) :nest empty-res))
                             _ (set-info :then-level 0)]
                            ret))
        query 
        (sur-by-ws (invisi-conc (lit \)) (update-info :nest-level dec)))))

(def bid
  (conc id (rep* (conc delimiter id))))

(def sign
  ^{:doc "Defines sing of where's expression."}
  (sur-by-ws (alt (lit-conc-seq ">=")
                  (lit-conc-seq "<=")
                  (lit-conc-seq "not=")
                  (lit-conc-seq "!=")
                  (lit \=) 
                  (lit \<)
                  (lit \>))))


(def pred-id (conc (rep+ alpha) (rep* (conc (lit \.) (rep+ alpha)))))

;; The block "value" has the following BNF:
;;    value -> v value'
;;    value'-> or v value' | ε
;;    v -> v-f v'
;;    v'-> and v-f v' | ε
;;    v-f -> (value) | some-value
(declare value)
(def v-f (alt (conc (lit \() value (lit \))) 
              (alt (conc (opt (change-pred sign :func)) (change-pred number :value)) 
                   (change-pred string :value)
                   (change-pred (lit-conc-seq "true") :value true)
                   (change-pred (lit-conc-seq "false") :value false)
                   (change-pred (lit-conc-seq "nil") :value nil)
                   (pfunction
                     (fn [f-m] (update-info 
                                 :preds 
                                 #(conj (pop %) 
                                        (assoc (peek %) 
                                               :value
                                               (peek f-m)))))
                          (update-info :function #(pop %))))))
(def v-prime (alt (conc (sur-by-ws (add-pred (alt (lit-conc-seq "and") (lit-conc-seq "&&")) nil)) 
                        (invisi-conc v-f (update-preds :and))
                        v-prime) emptiness))
(def v (conc v-f v-prime))
(def value-prime (alt (conc (sur-by-ws (add-pred (alt (lit-conc-seq "or") (lit-conc-seq "||")) nil)) 
                            (invisi-conc v (update-preds :or)) 
                            value-prime) emptiness))
(def value (conc v value-prime)) 


;; The block "where" has the following BNF:
;;    where -> T where'
;;    where'-> or T where' | ε
;;    T -> F T'
;;    T'-> and F T' | ε
;;    F -> (where) | id sign value

(declare where)
(def f (alt (conc (lit \() where (lit \)))
            (conc (alt (change-pred pred-id :ids)
                       (pfunction 
                         (fn [f-m] (update-info 
                                     :preds 
                                     #(conj (pop %) 
                                            (assoc (peek %) 
                                                   :ids
                                                   (peek f-m)))))
                         (update-info :function #(pop %))))
                  (change-pred sign :func) 
                  value)))
(def t-prime (alt (conc (sur-by-ws (add-pred (alt (lit-conc-seq "and") (lit-conc-seq "&&")))) 
                        (invisi-conc f (update-preds :and))
                        t-prime) emptiness))
(def t (conc f t-prime))
(def where-prime (alt (conc (sur-by-ws (add-pred (alt (lit-conc-seq "or") (lit-conc-seq "||"))))
                            (invisi-conc t (update-preds :or))
                            where-prime) emptiness))
(def where (conc t where-prime)) 


(def block-where
  ^{:doc "Defines where clause."}
  (conc (add-pred (lit \#)) (invisi-conc where change-preds)))


(def props
  ^{:doc "Defines sequences of properties of an object."}
  (conc (invisi-conc (lit \[) (update-info :then-level inc)) 
        (rep+ (alt (sur-by-ws (pfunction
                                #(partial set-id (peek %) found-prop) 
                                (update-info :function #(pop %))))
                   (sur-by-ws (conc (opt (invisi-conc (lit \*) (set-info :is-recur true)))
                                    (invisi-conc (process-id found-prop) (set-info :is-recur false))))))
        (invisi-conc (lit \]) (update-info :then-level dec))))


(declare params, param, pquery, pnumber, pstring, process-fn, parse+, pfunc, pid)
(def function
  ^{:doc "Defines YZ's function."}
  (conc (invisi-conc (lit \@) (update-info :function #(conj % empty-fun)))
        (lit \() (process-fn) params (lit \))))


(def params
  ^{:doc "Defines sequense of parameters of YZ's function."}
  (alt (conc param params) emptiness))


(def param
  ^{:doc "Defines different types of function's parameters."}
  (sur-by-ws (alt pstring pnumber pquery pfunc pid)))


(defmacro f-mod
  "Generates code for processing modificator 
  of function's parameter-query."
  [ch fm]
  `(invisi-conc (lit ~ch) (set-info :f-modificator ~fm)))


(def pquery
  ^{:doc "Defines parameter as query."}
  (conc (alt (f-mod single-pq :single)
             (f-mod list-pq :list)
             (f-mod indep-pq :indep))
        (complex [ret textq 
                  mom (get-info :mom)
                  q (effects (:result (parse+ ret mom)))
                  fm (get-info :f-modificator)
                  _ (update-param [fm q])]
                 ret)
        (lit end-pq)))


(def pnumber
  ^{:doc "Defines param as number."}
  (complex [n number
            _ (update-param (Double/parseDouble (reduce str "" (flatten n))))]
           n))


(def pstring
  ^{:doc "Defines param as string."}
  (complex [s string
            _ (update-param (nth s 1))]
           s))

(def pfunc
  ^{:doc "Defines parameter of function as another function"}
  (complex [f function
            f-m (get-info :function)
            _ (update-info :function #(pop %))
            _ (update-param (peek f-m))]
           f))


(def pid
  ^{:doc "Defines parameter as some id."}
  (complex [id (rep+ alpha)
            _ (update-param (symbol (reduce str id)))]
           id))


(defn process-fn
  "Processes function name."
  []
  (complex [n (rep+ (alt alpha (lit \.)))
            _ (update-info :function 
                           #(if-let [f (resolve (symbol (reduce str "" n)))]
                              (conj (pop %) (assoc (peek %) :func f))
                              (throw (Exception. (str "Could not found function " (reduce str "" n) ".")))))]
           n))

(def funcq
  ^{:doc "Defines rule for query as function."}
  (pfunction #(set-info :result (peek %)) 
             (update-info :function #(pop %))))


(def query
  (alt funcq (rep+ (alt bid nest-query (conc delimiter bid) block-where props))))


(defn parse+
  "Like parse, but returns all structure of result."
  [q, mom]
  ((query (struct q-representation (seq q) empty-res mom 0 0 [] nil [] false empty-pred)) 1))


(defn parse
  "Parses specified query ('q') on the YZ language based on
  specified ('mom') the map of the object model.
  Returns a value of the :result key of the q-representation structure."
  [q, mom]
  (let [r (parse+ q, mom)]
    (if (nil? (:reminder r))
      (:result (parse+ q, mom))
      (throw (Exception. (str "Syntax error near: " (reduce str "" (:remainder r))))))))


