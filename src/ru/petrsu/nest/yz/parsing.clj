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

;; 
;; Abridgements into comments:
;;    MOM - The Map of the Object Model 
;;    RCP - Reduced Complicated Predicates 
;;      (The YZ allows using the following syntax 
;;      for reducing complicated predicates: floor#(number = (1 || 2)) 
;;      instead of floor#(number=1 || number=2))
;;      

(ns ru.petrsu.nest.yz.parsing
  ^{:author "Vyacheslav Dimitrov"
    :doc "Code for the parsing of queries (due to the fnparse library)."}
  (:use name.choi.joshua.fnparse ru.petrsu.nest.yz.functions)
  (:require [clojure.string :as cs])
  (:import (clojure.lang PersistentArrayMap PersistentVector Keyword)))

(defn ^String sdrop
  "Drops first n characters from s.  Returns an empty string if n is
   greater than the length of s."
  [n ^String s]
  (if (< (count s) n)
    ""
    (.substring s n)))

;; The map of the object model some area
(declare mom)

; The parsing state data structure. 
(defstruct q-representation 
           :remainder ; The rest of input string
           :result ; vector of maps
           :then-level ; then level, the nubmer of dots.
           :nest-level ; Nest level, level of query (the number of parentthesis).
           :preds ; The vector within current predicates structure.
           :f-modificator ; Modificator of function's param.
           :function ; Describe current function.
           :is-recur ; Defines whether property is recur.
           :cur-pred ; Current predicate.
           :pp) ; Define process property.


;; Helper macros, definitions and functions.

(def empty-res
  ^{:doc "Defines vector within one empty map. 
         The vector is initial result of 'parse' function.
         Vector may contains the following keys:
            :props - vector with properties (empty).
            :what - class for selecting.
            :preds - list with predicates.
            :nest - nested vector with definition of linking objects: building (room)
            :then - nested map with definition of linking objects: building.room
            :where - path to parent objects."}
  [{:props []}])

(def empty-then
  ^{:doc "Defines then structure"}
  (empty-res 0))

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
    (if (<= nl 0)
      (get (last res-) k)
      (recur (:nest (last res-)) (dec nl)))))


(defn get-in-then
  "Gets value from res due to nl and tl"
  [res nl tl k]
  (if (= tl 0) 
    (get-in-nest res nl k) 
    (get-in (get-in-nest res nl :then) (conj (vec (repeat (dec tl) :then)) k))))


(defn get-in-nest-or-then
  "If last then is nil, then returns result
  of get-in-nest, otherwise returns result get-in-then."
  [res nl tl k]
  (let [last-then (get-in-nest res nl :then)]
    (if (nil? last-then)
      (let [then (get-in-nest res (dec nl) :then)]
        (if (nil? then)
          (get-in-nest res (dec nl) k)
          (loop [then- then, what (:what then)]
            (if (nil? then-)
              what
              (recur (:then then-) (:what then-))))))
      (get-in-then res nl tl k))))

(defn- get-paths
  "Returns list of paths beetwen cl-target and cl-source."
  [^Class cl-target, ^Class cl-source]
  (if (or (nil? cl-target) (nil? cl-source))
    nil
    (loop [cl- cl-target]
      (let [paths (get (get mom cl-source) cl-)]
        (if (empty? paths)
          (if (nil? cl-)
            (let [paths (some #(let [ps (get (get mom cl-source) %)]
                                 (if (not (empty? ps)) ps)) 
                              (ancestors cl-target))]
              (if (empty? paths)
                (throw (Exception. (str "Not found path between " cl-source " and " cl-target ".")))
                paths))
            (recur (:superclass (get mom cl-))))
          paths)))))


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


(declare find-class)
(defn- get-path
  "Returns path from cl-source to cl-target (search based on the mom)."
  [id, cl-source, cl-target]
  (let [paths (get-paths cl-target cl-source)]
    (if (empty? paths)
      ;; If path is not found then function returns self id. 
      ;; We can't throw exception, because we don't know whether 
      ;; cl-target is some interface and there is path between 
      ;; cl-source and some implementation of this interface.
      ;; This behaviour is processed into core.clj.
      [id]
      (nth paths 0))))


(defn- get-ids 
  "Returns new value of the :ids key of the pred structure."
  [ids ^String res ^Class cl]
  (let [sp-res (cs/split res #"\.")]
    (loop [cl- cl, ids- ids, sp-res- sp-res pp nil]
      (if (empty? sp-res-)
        [ids- pp]
        (let [id (first sp-res-)
              ^Class cl-target (find-class id)]
          (recur cl-target
                 (vec (flatten (conj ids- (get-path id cl- cl-target))))
                 (rest sp-res-)
                 ((keyword id) (:p-properties (get mom cl-)))))))))


(defn change-pred
  "Changes :preds of q-presentation by setting key 'k' to
  the return value of 'rule'. If 'value' is supplied then
  value of the 'k' is set to 'value' (This is need for 
  true, false, nil and so on special values.). "
  ([rule, ^Keyword k]
   (change-pred rule, k, :not-value))
  ([rule, k, value]
   (complex [ret rule
             res (get-info :result)
             nl (get-info :nest-level)
             tl (get-info :then-level)
             preds (get-info :preds)
             cpp (get-info :pp)
             res- (effects (if (seq? ret) (cs/trim (reduce str (flatten ret))) ret))
             [ids pp] (effects (if (= k :ids) 
                                 (get-ids (:ids (peek preds)) res- (get-in-then res  nl tl :what))
                                 [nil nil]))
             _ (if (nil? pp) (effects ()) (set-info :pp pp))
             _ (if (= k :value) (set-info :pp nil) (effects ()))
             _ (update-info 
                 :preds 
                 #(conj (pop %) 
                        (assoc (peek %) 
                               k 
                                 (cond

                                   ;; If RCP is part of predicate (like this: ei#(MACAddress="1" || "2"))
                                   ;; with processing properties from MOM, then we should just change parameter.
                                   (and (= k :value) (map? (:value (peek %)))) 
                                   (assoc (:value (peek %)) :params [(subs res- 1 (dec (count res-)))])

                                   ;; If predicate contains processing properties from MOM, then we should 
                                   ;; replace value which is received by value with map where :func key is function 
                                   ;; from MOM and :params key is vector with value which is received.
                                   (and (= k :value) (not (nil? cpp)) (= (first res-) \"))
                                   (let [stor (:s-to-r cpp)]
                                     {:func (some (fn [ns-] (ns-resolve ns- (symbol stor))) (all-ns)), 
                                      :params [(subs res- 1 (dec (count res-)))]})

                                   ;; If value is defined then we should return this value without processing (true, false, nil).
                                   (not= value :not-value) value

                                   ;; If key is :ids then we should get vector with ids due to get-ids function.
                                   (= k :ids) ids 

                                   ;; Because of clojure does not function "!=", we replaced it by funciton "not="
                                   (and (= k :func) (= (cs/trim res-) "!=")) "not="
                                   
                                   ;; Strings, numbers are not needed in any processing.
                                   :else res-))))
             preds (get-info :preds)
             _ (update-info :cur-pred #(if (nil? (last preds)) % (last preds)))]
            ret)))


(defn- ^Class find-class
  "Returns class which is correspended 'id' (search is did in 'mom'). 
  Search is did in the following positions:
    - as full name of class (package+name)
    - as name of class 
    - in 'sn' key of each map of mom.
    - as abbreviation class's name."
  [^String id]
  (let [l-id (cs/lower-case (str id))
        cl (get-in mom [:sns l-id])]
    (if (nil? cl)
      (let [cl (get-in mom [:names l-id])]
        (if (nil? cl)
          (get-in mom [:snames l-id])
          cl))
      cl)))


(defn- found-prop
  "This function is called when likely prop is found"
  [res id nl tl is-recur]
  (let [tl- (dec tl)
        last-then (get-in-nest res nl :then)
        id (cond (= id \&) :#self-object#
                 (= id "&.") :#default-property#
                 (map? id) id
                 :else (keyword (str id)))
        what (get-in-nest res nl :what)]
    (if (nil? what)
      (throw (Exception. (str "Not found element: " id)))
      (if (> tl- 0)
        (assoc-in-nest res nl :then (update-in last-then 
                                               (conj (vec (repeat (dec tl-) :then)) :props) 
                                               #(conj % [id is-recur])))
        (assoc-in-nest res nl :props (conj (get-in-nest res nl :props) [id is-recur]))))))


(defn- found-id
  "This function is called when id is found in query. Returns new result."
  [res ^String id nl tl is-recur]
  (let [^Class cl (find-class id)
        last-then (get-in-nest res nl :then)
        tl- (dec tl)]
    (if (nil? cl)
      (found-prop res id nl tl is-recur)
      (if (> tl 0)
        (if (nil? last-then)
          (assoc-in-nest res nl :then (assoc empty-then 
                                             :what cl 
                                             :where (get-paths cl, (get-in-nest res nl :what))))
          (assoc-in-nest 
            res nl 
            :then (assoc-in last-then 
                            (repeat tl- :then) 
                            (let [what (if (> tl- 1) 
                                         (get-in last-then (conj (vec (repeat (dec tl-) :then)) :what))
                                         (:what last-then))]
                              (assoc empty-then 
                                     :what cl
                                     :where (get-paths cl, what))))))
        (assoc-in-nest (assoc-in-nest res nl :what cl) 
                       nl :where (get-paths cl, (get-in-nest-or-then res nl tl :what)))))))


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
          ^Long c (cond (or (= ch single-pq) (= ch list-pq) (= ch indep-pq)) (inc end-c)
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
             id
             (:nest-level state)
             (:then-level state)
             (:is-recur state)))])


(defn process-id
  "Processes some id due to functions 'f'"
  [f]
  (complex [id (alt (lit-conc-seq "&.") (rep+ alpha))
            _ (partial set-id (reduce str (flatten id)) f)]
            id))

(def id (process-id found-id))

(def delimiter
  ^{:doc "Defines delimiter of ids: there are comma (for queries) 
  and dot (for property or link and so on)."}
  (alt
       (invisi-conc (lit \.) (update-info :then-level inc))
       (complex [ret (sur-by-ws (lit \,)) 
                 nl (get-info :nest-level)
                 _ (update-info :result #(add-value % nl (empty-res 0)))
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


(declare params, param, pquery, pnumber, pstring, process-fn, parse+, pfunc, pid, pself)
(def function
  ^{:doc "Defines YZ's function."}
  (conc (invisi-conc (lit \@) (update-info :function #(conj % empty-fun)))
        (lit \() (process-fn) params (lit \))))


(def params
  ^{:doc "Defines sequense of parameters of YZ's function."}
  (alt (conc param params) emptiness))


(def param
  ^{:doc "Defines different types of function's parameters."}
  (sur-by-ws (alt pstring pnumber pquery pfunc pself pid)))


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
                  nl (get-info :nest-level)
                  tl (get-info :then-level)
                  res (get-info :result)
                  q (effects (:result (parse+ ret mom)))
                  q (effects (vec (map #(assoc % 
                                               :where 
                                               (get-paths (:what %) (get-in-nest res nl :what))) q)))
                  fm (get-info :f-modificator)
                  _ (update-param [fm q])]
                 ret)
        (lit end-pq)))


(def pnumber
  ^{:doc "Defines param as number."}
  (complex [n number
            _ (update-param (let [n- (reduce str "" (flatten n))] 
                              (try (Integer/parseInt n-)
                                (catch Exception e (Double/parseDouble n-)))))]
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


(def pself
  ^{:doc "Defines param as self object or property of one."}
  (complex [s (conc (lit \&) (alt (conc (lit \.) (rep* alpha)) emptiness))
            _ (update-param (reduce str "" (flatten s)))]
           s))

(defn process-fn
  "Processes function name."
  []
  (complex [n (rep+ (alt alpha (lit \.)))
            _ (update-info :function 
                           #(if-let [f (let [sym (symbol (reduce str "" n))]
                                        (some (fn [ns-] (ns-resolve ns- sym)) (all-ns)))]
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
  [^String q, ^PersistentArrayMap mom]
  (do (def mom mom)
    ((query (struct q-representation (seq q) empty-res 0 0 [] nil [] false empty-pred nil)) 1)))


(defn parse
  "Parses specified query ('q') on the YZ language based on
  specified ('mom') the map of the object model.
  Returns a value of the :result key of the q-representation structure."
  [q, mom]
  (let [r (parse+ q, mom)]
    (if (nil? (:remainder r))
      (:result (parse+ q, mom))
      (throw (Exception. (str "Syntax error near: " (reduce str "" (:remainder r))))))))


