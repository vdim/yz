(ns ru.petrsu.nest.yz.parsing
  ^{:author Vyacheslav Dimitrov
    :doc "Code for the parsing of queries (due to the fnparse library)."}
  (:use name.choi.joshua.fnparse)
  (:require [clojure.string :as cs]
            [clojure.contrib.string :as ccs]))


; The parsing state data structure. 
(defstruct q-representation 
           :remainder ; The rest of input string
           :result ; vector of maps
           :mom ; The map of the object model some area
           :then-level ; then level, the nubmer of dots.
           :nest-level ; Nest level, level of query (the number of parentthesis).
           :preds ; The vector within current predicates structure.
           :is-then)  ; Defines whether id comes from '.'.

;; Helper macros, definitions and functions.

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
    (get-in-nest res nl :what) 
    (get-in (get-in-nest res nl :then) (conj (vec (repeat (dec tl) :then)) k))))


(def empty-res
  ^{:doc "Defines vector within one empty map. 
         The vector is initial result of 'parse' function."}
  [{:what nil
   :props nil
   :preds nil
   :then nil
   :nest nil}])

(def empty-then
  ^{:doc "Defines then structure"}
  {:what nil
   :props nil
   :preds nil
   :then nil})

(def empty-pred
  ^{:doc "Defines pred structure"}
  {:ids []
   :func nil
   :value nil})


(defmacro change-preds-
  "Generates code for changing ':preds'."
  [rule st]
  `(complex 
     [ret# ~rule
      res# (get-info :result)
      nl# (get-info :nest-level)
      tl# (get-info :then-level)
      _# (set-info 
           :result 
           (if (= tl# 0) 
             (assoc-in-nest res# nl# :preds (str (get-in-nest res# nl# :preds ) ~st))
             (let [last-then# (get-in-nest res# nl# :then)]
               (assoc-in-nest res# nl# :then 
                              (assoc-in last-then# 
                                        (conj (vec (repeat (dec tl#) :then)) :preds) 
                                        (str (get-in last-then# 
                                                     (conj (vec (repeat (dec tl#) :then)) :preds)) 
                                             ~st))))))]
     ret#))


(declare tr-pred)
(defn change-preds
  "Generates code for changing ':preds'."
  [state]
  ["", (let [res (:result state)
             nl (:nest-level state)
             tl (:then-level state)
             st (str "#=(eval (fn [o] " 
                     (let [fp (first (:preds state))] 
                       (if (map? fp) (tr-pred fp) fp)) "))")]
         (assoc (assoc state :preds []) :result 
                (if (= tl 0) 
                  (assoc-in-nest res nl :preds (str (get-in-nest res nl :preds ) st))
                  (let [last-then (get-in-nest res nl :then)]
                    (assoc-in-nest res nl :then 
                                   (assoc-in last-then 
                                             (conj (vec (repeat (dec tl) :then)) :preds) 
                                             (str (get-in last-then (conj (vec (repeat (dec tl) :then)) :preds)) 
                                                  st)))))))])


(defn add-pred
  "Conjs empty-pred to current vector :preds in
  q-representation."
  [rule]
  (invisi-conc rule (update-info :preds #(conj % empty-pred))))


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
  (let [sp-res (ccs/split #"\." res)]
    (loop [cl- cl, ids- ids, sp-res- sp-res]
      (if (empty? sp-res-)
        ids-
        (recur (find-class (first sp-res-) mom) 
               (vec (flatten (conj ids- (get-path (first sp-res-) cl- mom))))
               (rest sp-res-))))))


(defn change-pred
  "Changes :preds of q-presentation by setting key 'k' to
  the return value of 'rule'."
  [rule, k]
  (complex [ret rule
            mom (get-info :mom)
            res (get-info :result)
            nl (get-info :nest-level)
            tl (get-info :then-level)
            _ (update-info 
                :preds 
                #(conj (pop %) 
                       (assoc (peek %) 
                              k 
                              (let [res- (if (seq? ret) (reduce str (flatten ret)) ret)]
                                (if (= k :ids)
                                  (get-ids (:ids (peek %)) res- mom (get-in-then res nl tl :what))
                                  res-)))))]
           ret))


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


(defmacro assoc-in* 
  "Like clojure.core/assoc-in but vector of keys has specified structure: 
  key* repeats n times and then key-in is."
  [m key* n key-in v] 
  `(assoc-in ~m (vec (flatten [(repeat ~n ~key*) ~key-in])) ~v))


(defn found-id
  "This function is called when id is found in query. Returns new result."
  [res mom id nl tl is-then]
  (let [cl (find-class id, mom)
        last-then (get-in-nest res nl :then)
        tl- (- tl 2)
        what (if last-then 
               (:what (if (<= tl- 0) 
                        last-then
                        (get-in last-then (repeat tl- :then)))) 
               (get-in-nest res nl :what))]
    (if (nil? cl)
      (if is-then
        (if-let [prop (find-prop what id mom)]
          (if (>= tl- 0)
            (assoc-in-nest res nl :then (assoc-in last-then (conj (vec (repeat tl- :then)) :props) id))
            (assoc-in-nest res nl :props id))
          (throw (Exception. (str "Not found id: " id))))
        (throw (Exception. (str "Not found id: " id))))
      (if is-then
        (if (nil? last-then)
          (assoc-in-nest res nl :then (assoc empty-then :what cl))
          (assoc-in-nest res nl :then (assoc-in last-then (repeat (dec tl) :then) (assoc empty-then :what cl))))
        (assoc-in-nest res nl :what cl)))))

(defn tr-pred
  "Transforms 'pred' map into string"
  [pred]
  (str "(ru.petrsu.nest.yz.core/process-preds o, " (:ids pred) 
       ", " (:func pred) ", " (reduce str (:value pred)) ")"))

(defn do-predicate
  "Returns string of predicate."
  [op, pred1, pred2]
  (let [pred1- (if (map? pred1) (tr-pred pred1) pred1)
        pred2- (if (map? pred2) (tr-pred pred2) pred2)]
    (str "(" op " " pred1- " " pred2- ")")))








;; Rules of grammar are below. See BNF in the begining of file.

(def alpha
  ^{:doc "Sequence of characters."}
  (lit-alt-seq "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"))

(def digit
  ^{:doc "Sequence of digits."}
  (lit-alt-seq "1234567890."))

(def number
  ^{:doc "Defines number."}
  (conc (opt (alt (lit \+) (lit \-))) (rep+ digit)))

(def whitespaces
  ^{:doc "List of whitespaces"}
  (rep+ (alt (lit \space) (lit \newline) (lit \tab))))


(defn string-
  "Rule for recognizing any string."
  [state]
  (let [remainder (reduce str (:remainder state))
        res (for [a (:remainder state) :while (not (= a \"))] a)]
    [(reduce str res) 
     (assoc state :remainder (ccs/drop (count res) remainder))]))


(def string
  ^{:doc "Defines string"}
  (conc (lit \") string- (lit \")))


(defn set-id
  [id, state]
  [(:remainder state) 
   (assoc state :result 
          (found-id (:result state) 
                    (:mom state) 
                    (reduce str id) 
                    (:nest-level state)
                    (:then-level state)
                    (:is-then state)))])


(def id (complex [id- (rep+ alpha) 
                  _ (invisi-conc (partial set-id id-) (set-info :is-then false))]
                 id-))


(def delimiter
  ^{:doc "Defines delimiter of ids: there are comma (for queries) 
  and dot (for property or link and so on)."}
  (alt
       (invisi-conc (invisi-conc (lit \.) (update-info :then-level inc))  (set-info :is-then true))
       (complex [ret (sur-by-ws (lit \,)) 
                 res (get-info :result)
                 nl (get-info :nest-level)
                 _ (set-info :result (add-value res nl (empty-res 0)))
                 _ (set-info :then-level 0)]
                ret)))


(declare query)
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
  (alt (lit \=) 
       (lit \<)
       (lit \>)
       (conc (lit \!) (lit \=))
       (conc (lit \>) (lit \=))
       (conc (lit \<) (lit \=))))


(def pred-id (conc (rep+ alpha) (rep* (conc (lit \.) (rep+ alpha)))))

;; The block "value" has the following BNF:
;;    value -> v value'
;;    value'-> or v value' | ε
;;    v -> v-f v'
;;    v'-> and v-f v' | ε
;;    v-f -> (value) | some-value
(declare value)
(def v-f (alt (conc (lit \() value (lit \))) 
              (alt (conc (opt sign) 
                         (change-pred number :value)) 
                   (change-pred string :value)
                   (change-pred (lit-conc-seq "true") :value)
                   (change-pred (lit-conc-seq "false") :value))))
(def v-prime (alt (conc (sur-by-ws (lit-conc-seq "and")) v-f v-prime) emptiness))
(def v (conc v-f v-prime))
(def value-prime (alt (conc (sur-by-ws (lit-conc-seq "or")) v value-prime) emptiness))
(def value (conc v value-prime)) 


;; The block "where" has the following BNF:
;;    where -> T where'
;;    where'-> or T where' | ε
;;    T -> F T'
;;    T'-> and F T' | ε
;;    F -> (where) | id sign value

(declare where)
(def f (alt (conc (lit \() where (lit \)))
            (conc (change-pred pred-id :ids) 
                  (change-pred sign :func) 
                  value)))
(def t-prime (alt (conc (sur-by-ws (add-pred (lit-conc-seq "and"))) 
                        (invisi-conc 
                          f 
                          (update-info :preds 
                                       #(conj (pop (pop %))
                                              (do-predicate "and" (peek (pop %)) (peek %)))))                       
                        t-prime) emptiness))
(def t (conc f t-prime))
(def where-prime (alt (conc (sur-by-ws (add-pred (lit-conc-seq "or"))) 
                            (invisi-conc 
                              t 
                              (update-info :preds 
                                           #(conj (pop (pop %))
                                                  (do-predicate "or" (peek (pop %)) (peek %)))))
                            where-prime) emptiness))
(def where (conc t where-prime)) 


(def block-where
  ^{:doc ""}
  (conc (add-pred (lit \#)) (invisi-conc where change-preds)))

(def query
  (rep+ (alt bid nest-query (conc delimiter bid) block-where)))


(defn parse+
  "Like parse, but returns all structure of result."
  [q, mom]
  ((query (struct q-representation (seq q) empty-res mom 0 0 [] false)) 1))


(defn parse
  "Parses specified query ('q') on YZ language based on
  specified ('mom') the map of the object model.
  Returns value of key's :result of structure of result (q-representation structure)."
  [q, mom]
  (:result (parse+ q, mom)))


