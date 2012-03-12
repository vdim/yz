;;
;; Copyright 2011-2012 Vyacheslav Dimitrov <vyacheslav.dimitrov@gmail.com>
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
  (:use name.choi.joshua.fnparse)
  (:require [clojure.string :as cs])
  (:import (clojure.lang PersistentArrayMap PersistentVector Keyword)
           (ru.petrsu.nest.yz SyntaxException NotFoundPathException 
                              NotFoundElementException NotFoundFunctionException
                              NotDefinedDP)))


(defn- ^String sdrop
  "Drops first n characters from s.  Returns an empty string if n is
   greater than the length of s."
  [n ^String s]
  (if (< (count s) n)
    ""
    (.substring s n)))


;; The map of the object model some area
;; We factor out it from q-representation because of it is not changed.
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
           :cur-sort ; Current indicator of sorting.
           :pp ; Define process property.
           :all ; Defines whether ∀ modificator is set.
           )


;; Helper macros, definitions and functions.

(def query-params 
  "Defines vector with query's parameters. Example such query is: 
    floor#(number=($1 || $2))"
  (atom []))

(def empty-res
  "Defines vector within one empty map. 
  The vector is initial result of 'parse' function.
  Vector may contains the following keys:
    :props - vector with properties (empty).
    :what - class for selecting.
    :preds - list with predicates.
    :nest - nested vector with definition of linking objects: building (room)
    :then - nested map with definition of linking objects: building.room
    :where - path to parent objects."
  [{}])

(def empty-then
  "Defines then structure."
  {})

(def empty-pred
  "Defines pred structure."
  {:ids []
   :func nil
   :value nil})

(def empty-fun
  "Defines function structure"
  {:func nil ; Function (:func has clojure.lang.Var type.)
   :params []}) ; Vector with parameters.


(defmacro sur-by-ws
  "Surrounds 'rule' by whitespaces like this
  (conc (opt whitespaces) rule (opt whitespaces))."
  [rule]
  `(conc (opt whitespaces) ~rule (opt whitespaces)))


(defn- nnassoc 
  "Like assoc, but if value is nil 
  then returns map m without changes."
  ([m k value]
   (if (nil? value)
     m
     (assoc m k value)))
  ([m k value & kvs]
   (let [ret (nnassoc m k value)]
     (if kvs 
       (recur ret (first kvs) (second kvs) (nnext kvs))
       ret))))


(defn- nnassoc-in
  "Like assoc-in, but not associcates nil values."
  [m [k & ks] v]
  (if ks
    (nnassoc m k (nnassoc-in (get m k) ks v))
    (nnassoc m k v)))


(defn assoc-in-nest
  "Like assoc-in, but takes into account structure :result.
  Inserts some value 'v' in 'res' map to :nest key."
  [res nest-level l-tag v & kvs]
  (if (<= nest-level 0)
    (conj (pop res) (apply nnassoc (peek res) l-tag v kvs))
    (conj (pop res)
          (nnassoc (peek res) 
                   :nest 
                   (apply assoc-in-nest (:nest (peek res)) (dec nest-level) l-tag v kvs)))))


(defn- create-f
  "Creates function from specified string
  something like this:
    \"#=(eval \" \"#(inc %)\" \")\"."
  [s]
  (read-string (str "#=(eval " s ")")))


(defn- add-value
  "Conjs some value 'v' to :nest array which has nest-level 
  of level."
  [res nest-level v]
  (if (<= nest-level 0)
    (conj res v)
    (conj (pop res)
          (assoc (peek res) 
                 :nest 
                 (add-value (:nest (peek res)) (dec nest-level) v)))))


(defn get-in-nest
  "Like get-in, but takes into account structure of :result
  field of q-representation structure. First :nest key is
  nest-level times, then k is."
  [res nest-level k]
  (loop [res- res nl nest-level]
    (if (<= nl 0)
      (get (peek res-) k)
      (recur (:nest (peek res-)) (dec nl)))))


(defn- get-in-then
  "Gets value from res due to nl and tl"
  [res nl tl k]
  (if (= tl 0) 
    (get-in-nest res nl k) 
    (get-in (get-in-nest res nl :then) (-> tl dec (repeat :then) vec (conj k))))) 


(defn- get-in-nest-or-then
  "If last then is nil, then returns result
  of get-in-nest, otherwise returns result get-in-then."
  [res nl tl k]
  (let [last-then (get-in-nest res nl :then)]
    (if (nil? last-then)
      (let [then (get-in-nest res (dec nl) :then)]
        (if (nil? then)
          (get-in-nest res (dec nl) k)
          (loop [then- then, value (k then)]
            (if (nil? then-)
              value
              (recur (:then then-) (k then-))))))
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
                (throw (NotFoundPathException. (str "Not found path between " cl-source " and " cl-target ".")))
                paths))
            (recur (:superclass (get mom cl-))))
          paths)))))


(defn- change-preds
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
                                   (nnassoc-in last-then 
                                             (conj (vec (repeat (dec tl) :then)) :preds) 
                                             st))))))])


(defn- add-pred
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
  "Returns map where 
    - value of key :id is path from objects
      which have 'cl-source' class to objects which have 
      'cl-target' class (search based on the MOM). 
    - value of key :cl is cl-target. :cl is needed for filtering
      a set of objects by this class. It may be usefull where some 
      class has a link to another class over some superclass, but
      user use this link and use a property which belongs only to 
      the child class. 
  
      Let's consider example: we have SomeClass (sc),
      AnotherClass (ac) which extends SuperAnotherClass (sac, it may 
      be interface or abstract class). The AnotherClass has the property
      \"somep\" (SuperAnotherClass does not). SomeClass is linked with
      AnotherClass over List<SuperAnotherClass>. So user make query
      something like this sc#(ac.somep=1). YZ gets ac over list with the sac
      and then tries to get value of somep, but somep belongs only to
      ac, so error is occured in case another implementation of the sac is added to list. 
      But if we have :cl, we can filter list with sac
      and avoid error."
  [id, cl-source, cl-target]
  (let [paths (get-paths cl-target cl-source)]
    (if (empty? paths)
      ;; If path is not found then function returns self id. 
      ;; We can't throw exception, because we don't know whether 
      ;; cl-target is some interface and there is path between 
      ;; cl-source and some implementation of this interface.
      ;; This behaviour is processed into core.clj.
      [{:id [id] :cl cl-target}]
      {:id (nth paths 0) :cl cl-target})))


(defn- get-ids 
  "Returns new value of the :ids key of the pred structure."
  [ids ^String res ^Class cl]
  (loop [cl- cl, ids- ids, sp-res (cs/split res #"\.") pp nil]
    (if (empty? sp-res)
      (if (.endsWith res ".") ;; Processes queries which contain default property into predicates: building#(floor.=1)
        (if-let [dp (:dp (get mom cl-))]
          [(conj ids- {:id [(name dp)] :cl nil}) (dp (:p-properties (get mom cl-)))]
          (throw (NotDefinedDP. (str "Default property is not defined for " cl-))))
        [ids- pp])
      (let [id (first sp-res)
            ^Class cl-target (find-class id)]
        (recur cl-target
               (vec (flatten (conj ids- (get-path id cl- cl-target))))
               (rest sp-res)
               ((keyword id) (:p-properties (get mom cl-))))))))


(defn- change-pred
  "Changes :preds of q-presentation by setting key 'k' to
  the return value of 'rule'. If 'value' is supplied then
  value of the 'k' is set to 'value' (This is need for 
  true, false, nil and so on special values.). "
  ([rule, ^Keyword k]
   (change-pred rule, k, :not-value))
  ([rule, ^Keyword k, value]
   (complex [ret rule
             res (get-info :result)
             nl (get-info :nest-level)
             tl (get-info :then-level)
             preds (get-info :preds)
             cpp (get-info :pp)
             allm (get-info :all)
             res- (effects (if (seq? ret) (cs/trim (reduce str (flatten ret))) ret))

             ;; If query is floor(.=1) then res- will be "." (character), 
             ;; so we must coerce it to string (for passing to get-ids function).
             res- (effects (if (instance? Character res-) (str res-) res-))
             
             [ids pp] (effects (if (= k :ids) 
                                 (get-ids (:ids (peek preds)) res- (get-in-then res  nl tl :what))
                                 [nil nil]))
             _ (if (nil? pp) (effects ()) (set-info :pp pp))
             _ (if (= k :value) (set-info :pp nil) (effects ()))
             [value res-] (effects 
                            (cond (= value :number)
                                  [:not-value
                                   (try (Integer/parseInt res-)
                                     (catch Exception e (Double/parseDouble res-)))]
                                  (= value :string) [:not-value (subs res- 1 (dec (count res-)))]
                                  :else [value res-]))
             _ (update-info 
                 :preds 
                 #(conj (pop %) 
                        (nnassoc (peek %) 
                                 :all allm
                                 k 
                                 (cond
                                   ;; If RCP is part of predicate (like this: ei#(MACAddress="1" || "2"))
                                   ;; with processing properties from MOM, then we should just change parameter.
                                   (and (= k :value) (map? (:value (peek %)))) 
                                   (assoc (:value (peek %)) :params [res-])

                                   ;; If predicate contains processing properties from MOM, then we should 
                                   ;; replace value which is received by value with map where :func key is function 
                                   ;; from MOM and :params key is vector with value which is received.
                                   (and (= k :value) cpp (:s-to-r cpp)) 
                                   (let [stor (:s-to-r cpp)
                                         stor (if (var? stor) stor (create-f stor))]
                                     {:func stor, :params [res-]})

                                   ;; If value is :parameter then value must be atom.
                                   (= value :parameter) (do (swap! query-params conj nil)
                                                          (keyword (subs res- 1)))
                                   
                                   ;; If value is defined then we should return this value without processing (true, false, nil).
                                   (not= value :not-value) value

                                   ;; If key is :ids then we should get vector with ids due to get-ids function.
                                   (= k :ids) ids

                                   ;; Resolve function.
                                   (= k :func) 
                                   (let [tres (cs/trim res-)]
                                     (case tres
                                       ;; Because of clojure does not function "!=", we replaced it by funciton "not="
                                       "!=" #'clojure.core/not=

                                       ;; Function for regular expressions.
                                       "~" #'clojure.core/re-find 

                                       ;; Identical function.
                                       "==" #'clojure.core/identical?
                                     
                                       (resolve (symbol res-))))

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
  (let [l-id (cs/lower-case (str id))]
    (some #(get-in mom [% l-id]) [:sns :names :snames])))


(defn- get-sort
  "Returns returns vector with:
    - type of sort (tsort);
    - comparator (for specified class);
    - keyfn (for specified class).
  for specified class (cl) and its property (prop)."
  [tsort, cl, prop]
  (if tsort
    (let [prop (cond (= prop :#default-property#) (:dp (get mom cl))
                     (= prop :#self-object#) :self
                     :else prop)
          f #(let [v (get-in (get mom cl) [:sort prop %])]
               (if (string? v)
                 (create-f v)
                 v))]
      [tsort 
       (f :comp)
       (f :keyfn)])
    [nil nil nil]))


(defn- found-prop
  "This function is called when likely some property is found"
  [res id nl tl is-recur tsort]
  (let [tl- (dec tl)
        id (cond (= id "&") :#self-object#
                 (= id "&.") :#default-property#
                 (map? id) id
                 :else (keyword (str id))) 
        what (get-in-nest-or-then res nl tl- :what)]
    (if (nil? what)
      (throw (NotFoundElementException. (str "Not found element: " (name id))))
      (let [sorts (get-in-nest-or-then res (inc nl) tl- :sort)
            props (get-in-nest-or-then res (inc nl) tl- :props)
            sorts (cond

                    ; Not nothing, so sorts is nil.
                    (and (not tsort) (not sorts)) nil
                    
                    ; If sort is map (from query {a:name, d:description}room), then another sorting is ignore.
                    ;(every? list? sorts) sorts 
                    (or (and sorts (every? list? sorts)) (map? sorts)) sorts 

                    ; Because of sorting type is not nil first time (sorts is nil), then we must
                    ; create structure of value's :sort key: vector [nil nil nil] for
                    ; class which is selected plus vector [nil nil nil] for each 
                    ; property which has already added plus vector for current type of 
                    ; sorting (get-sort function).
                    (not sorts)
                    (conj (vec (repeat (count props) [nil nil nil])) [nil nil nil] (get-sort tsort, what, id))

                    ; If sorts is not nil, then we must add some vector for current type of sorting
                    ; (get-sort function).
                    :else (if (vector? (sorts 0)) 
                            (conj sorts (get-sort tsort, what, id))
                            (conj [sorts] (get-sort tsort, what, id))))
            f #(assoc-in-nest res nl %1 %2 :sort sorts)
            
            ; This function is need for improving performance because first element of :props is
            ; not vector but next is. Result of benchmark in case parameter is vector:
            ; (time (dotimes [_ 1e7] (lvec [1 2 3]))): "Elapsed time: 327.200079 msecs"
            ; (time (dotimes [_ 1e7] (vec [1 2 3]))): "Elapsed time: 7122.513999 msecs"
            lvec #(if (vector? %) % (vec %))]
        (if (> tl- 0)
          (let [v #(conj (vec (repeat (dec tl-) :then)) %)
                last-then (get-in-nest res nl :then)
                last-then (nnassoc-in last-then (v :sort) sorts)]
            (assoc-in-nest 
              res nl :then
              (update-in last-then (v :props)
                         #(lvec (conj % [id is-recur])))))
          (f :props (lvec (conj (get-in-nest res nl :props) [id is-recur]))))))))


(defn- transform-sort
  "Takes vector with sort and specified 
  class and transforms it for passing to core.clj"
  [vsort tsort cl]
  (cond 
    ; vsort is map where key is name of the property (or function) and 
    ; value is type of the sort (:asc, :desc).
    (map? vsort) 
    (reduce 
      #(let [[k v] %2
             p (cond 
                 ; Sorting is done by default property: {a:&.}building
                 (= :#default-property# k) (:dp (get mom cl)) 
                 
                 ; Sorting is done by result of a function: {a:@(count `room')}building
                 (map? k) 
                 (assoc k :params 
                        (reduce 
                          (fn [ps p]
                            (if (vector? p)
                              (let [f (p 1)]
                                (conj ps [(p 0) 
                                          (reduce (fn [r vv] (conj r (nnassoc vv :where (get-paths (:what vv) cl)))) [] f)]))
                              (conj ps p))) [] (:params k)))

                 ;Sorting is done by some property: {a:name}building
                 :else k)]
         (conj %1 (list p (get-sort v cl p)))) [] vsort)

    ; tsort won't be nil into queries something like this: a:building
    ; (tsort = :asc)
    tsort (get-sort tsort cl :self)))


(defn- found-id
  "This function is called when id is found in query. Returns new result."
  [res ^String id nl tl is-recur tsort]
  (let [[id ex] (if (.endsWith id "^") [(subs id 0 (dec (count id))) true] [id nil])
        ^Class cl (find-class id)
        last-then (get-in-nest res nl :then)
        tl- (dec tl)]
    (if (nil? cl)
      (found-prop res id nl tl is-recur tsort)
      (let [f #(assoc-in-nest res nl :then %)
            ; Vector with type of sorting, comparator and keyfn.
            vsort (get-in-nest-or-then res (inc nl) tl- :sort) 
            vsort (transform-sort vsort tsort cl)
            ; Function for association some values of some then map.
            assoc-lth #(nnassoc %1 :what cl :where (get-paths cl %2) :sort vsort :exactly ex)
            ; What for getting where.
            what (get-in-nest-or-then res nl tl- :what)]
        (if (> tl 0)
          (if (nil? last-then)
            (f (assoc-lth empty-then (get-in-nest res nl :what)))
            (let [then-v (repeat tl- :then)
                  ;; DON'T MODIFY next two lines to: lt (if (nil? lt) empty-then (get-in last-then v))
                  ;; Because of get-in can return nil, but lt must be not nil.
                  lt (get-in last-then then-v)
                  lt (if (nil? lt) empty-then lt)

                  lt (assoc-lth lt what)
                  lt (if (empty? then-v) lt (nnassoc-in last-then then-v lt))]
              (f lt)))
         (assoc-in-nest 
           res nl
           :what cl
           :where (get-paths cl, what)
           :sort vsort
           :exactly ex))))))


(defn- add-op-to-preds
  "Takes operation ('and' or 'or') and adds it to
  end of the :preds value "
  [op]
  (update-info :preds #(conj % op)))


(defn- update-param
  "Updates :params key of :function key of the q-representation structure."
  [value]
  (update-info :function #(let [f (peek %)] 
                            (conj (pop %) 
                                  (assoc f 
                                         :params 
                                         (conj (:params f) value))))))


(defn- text
  "Rule for recognizing any string.
  ch defines list with last symbols of this string."
  [chs state]
  (let [remainder (reduce str (:remainder state))
        res (for [a (:remainder state) :while (not (some #(= a %) chs))] a)]
    [(reduce str res) 
     (assoc state :remainder (sdrop (count res) remainder))]))


(def limit-nq
  "Defines number of the nested queries into parameter as query."
  100)

(declare end-pq, start-pq)
(defn- textq
  "Recognizes text of query which is parameter of a function.
  textq restricts count of nested queries, because in case where
  user input an incorrect query (with starting modificator and without
  ending modificator), then infinite loop is occured."
  [state]
  (loop [res "" remainder (:remainder state) end-c 0 count-nq 0]
    (let [ch (first remainder)
          ^Long c (cond (= ch start-pq) (inc end-c)
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
  "Sequence of characters."
  (lit-alt-seq "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890&+/-*:?"))

(def digit
  "Sequence of digits."
  (lit-alt-seq "1234567890"))

(def number
  "Defines number."
  (conc (opt (alt (lit \+) (lit \-))) (rep+ digit) (opt (conc (lit \.) (rep+ digit)))))

(def whitespaces
  "List of whitespaces"
  (rep+ (alt (lit \space) (lit \newline) (lit \tab))))

(def end-pq
  "Defines symbol for indication end query which is parameter."
  \')

(def start-pq
  "Defines symbol for indication start query which is passed."
  \`)

(def string
  "Defines string."
  (conc (lit \") (partial text [\"]) (lit \")))


(def descsort
  "Defines rule for sorting by descenting."
  (complex [_ (alt (lit \↓) (lit-conc-seq "d:"))]
           :desc))


(def ascsort
  "Defines rule for sorting by ascending."
  (complex [_ (alt (lit \↑) (lit-conc-seq "a:"))]
           :asc))


(defn- set-sort
  "Process sorting by properties which 
  are not selected: {a:number}room.
  Takes current state and list where first
  element is type of sorting and second is property.
  Returns vector where first element is new remainder, 
  and second is new state."
  [[tsort prop] state]
  (let [propid (cond (map? prop) prop ; user sorts objects by some function.
                     (= prop [\& \.]) :#default-property#
                     (= prop [\&]) :self
                     :else (keyword (reduce str prop)))
        res (:result state)
        nl (:nest-level state)
        tl (:then-level state)]
    [(:remainder state) 
     (assoc state :result 
        (if (> tl 0)
            (let [then-v (vec (repeat (dec tl) :then))
                  last-then (get-in-nest res nl :then)
                  ;; DON'T MODIFY next two lines to: lt (if (nil? lt) empty-then (get-in last-then v))
                  ;; Because of get-in can return nil, but lt must be not nil.
                  lt (get-in last-then then-v)
                  lt (if (nil? lt) empty-then lt)

                  lt (if (empty? then-v) lt (nnassoc-in last-then then-v lt))
                  lt (update-in lt (conj then-v :sort) #(nnassoc % propid tsort))]
              (assoc-in-nest res nl :then lt))
         (assoc-in-nest res nl :sort (assoc (get-in-nest res nl :sort) propid tsort))))]))


(declare function)
(def propsort
  "Defines sorting of objects by properties which are not selected.
  For example, rooms from a result of the query {a:number}room,
  will be sorted by number, although this numbers of rooms are not selected."
  (conc (lit \{) 
        (rep* (sur-by-ws 
                (complex [ret (conc (alt descsort ascsort) 
                                    (alt (lit-conc-seq "&.")  ; sorting by default property: {a:&.}room
                                         (rep+ alpha)         ; sorting by property: {a:name}room
                                         (complex [_ function ; sorting by function: {a:@(count `room')}building
                                                   f (get-info :function)
                                                   _ (update-info :function #(pop %))]
                                                  (peek f))))
                          _ (partial set-sort ret)]
                         ret)))
        (lit \})))


(def idsort
  "Defines sort and its type."
  (let [ch-sort #(invisi-conc %1 (set-info :cur-sort %2))]
    (alt (ch-sort descsort :desc) (ch-sort ascsort :asc)
         (ch-sort emptiness nil))))


(defn- set-id
  [id, f, state]
  [(:remainder state) 
   (assoc state :result 
          (f (:result state) 
             id
             (:nest-level state)
             (:then-level state)
             (:is-recur state)
             (:cur-sort state)))])


(defn- process-id
  "Processes some id due to functions 'f'"
  [f]
  (complex [id (conc (alt (lit-conc-seq "&.") (rep+ alpha)) 
                     (opt (lit \^))) ; Defines whether class must be exact.
            _ (partial set-id (reduce str (flatten id)) f)]
            id))

(def id (process-id found-id))


(def delimiter
  "Defines delimiter (now it is comma) for queries into one level.
  Examples: 
    room, building
    room (floor, building)
    room (floor, building), device"
  (complex [ret (sur-by-ws (lit \,)) 
            nl (get-info :nest-level)
            _ (update-info :result #(add-value % nl {})) ; Add new map to current nest level.
            _ (set-info :then-level 0)]
           ret))


(declare query, function)
(def nest-query
  "Defines nested query"
  (conc (sur-by-ws (complex [ret (invisi-conc (lit\() (update-info :nest-level inc))
                             nl (get-info :nest-level)
                             _ (update-info :result #(assoc-in-nest % (dec nl) :nest empty-res))
                             _ (set-info :then-level 0)]
                            ret))
        query 
        (sur-by-ws (invisi-conc (lit \)) (update-info :nest-level dec)))))



(declare block-where, props)
(def props-and-where
  "Defines block from properties or predicates.
  It may be emptiness or contains only block with
  properties or only block with predicates or both.
  Also an order of blocks is not important:
    floor#(number=1)[name]
    floor[name]#(number=1)"
  (alt (conc (opt props) (opt block-where) (opt props))))


(def bid
  "Defines sequence from ids. Example: room.floor.number
  Sorting may be defined before id. Example: {a:number}room
  Each id may contains block from properties or predicates."
  (conc (alt propsort idsort)
        id
        props-and-where
        (rep* (conc (invisi-conc (lit \.) 
                                 (update-info :then-level inc)) 
                    (alt propsort idsort)
                    id 
                    props-and-where))))

(def sign
  "Defines sing of where's expression."
  (sur-by-ws (alt (lit-conc-seq ">=")
                  (lit-conc-seq "<=")
                  (lit-conc-seq "not=")
                  (lit-conc-seq "!=")
                  (lit-conc-seq "==")
                  (lit \=) 
                  (lit \~) 
                  (lit \<)
                  (lit \>))))


(def pred-id 
  "Defines id into predicates."
  (alt (lit \.) (conc (rep+ alpha) (rep* (conc (lit \.) (rep* alpha))))))


(defn- pfunc-as-param
  "TODO: what is this function do?"
  [k]
  (pfunction
    (fn [f-m] 
      (update-info 
        :preds 
        #(conj (pop %) 
               (assoc (peek %) 
                      k
                      (peek f-m)))))
    (update-info :function #(pop %))))


(def value-as-param
  "Defines value as parameter of query: floor#(number=$1)"
  (conc (lit \$) (rep+ digit)))


(def value-as-subq
  "Defines value as subquery: floor#(name = room.number)"
  query)

;; The block "value" has the following BNF:
;;    value -> v value'
;;    value'-> or v value' | ε
;;    v -> v-f v'
;;    v'-> and v-f v' | ε
;;    v-f -> (value) | some-value
(declare value, do-q)
(def v-f (alt (conc (lit \() value (lit \))) 
              (alt (conc (opt (change-pred sign :func)) 
                         (alt (change-pred number :value :number) 
                              (change-pred value-as-param :value :parameter)))

                   ;; Rule for RCP with string: room#(number=("200" || ~".*1$"))
                   (conc (opt (change-pred 
                                (sur-by-ws 
                                  (alt (lit \=) (lit \~) (lit-conc-seq "!=") (lit-conc-seq "==")))
                                :func)) 
                         (change-pred string :value :string))
                   (change-pred (lit-conc-seq "true") :value true)
                   (change-pred (lit-conc-seq "false") :value false)
                   (change-pred (lit-conc-seq "nil") :value nil)
                   (change-pred value-as-param :value :parameter)
                   (pfunc-as-param :value)
                   
                   (complex [rm (get-info :remainder)
                             newrm (effects 
                                     (loop [rm- (next rm), ch (first rm), brs 0, newrm []]
                                       (if (and (= brs 0) (= ch \)))
                                         newrm
                                         (recur (next rm-)
                                                (first rm-)
                                                (case ch
                                                  \( (inc brs)
                                                  \) (dec brs)
                                                  brs)
                                                (conj newrm ch)))))

                             rq (effects (try 
                                           (let [rq (do-q newrm)
                                                 r (:remainder rq)]
                                             (when (empty? r) rq))
                                           (catch Exception e nil)))

                             :when rq
                             cp (change-pred (effects rq) :value (:result rq))
                             _ (set-info :remainder (drop (count newrm) rm))
                             ] cp))))
(def v-prime (alt (conc (sur-by-ws (add-pred (alt (lit-conc-seq "and") (lit-conc-seq "&&")) nil)) 
                        (invisi-conc v-f (add-op-to-preds :and))
                        v-prime) emptiness))
(def v (conc v-f v-prime))
(def value-prime (alt (conc (sur-by-ws (add-pred (alt (lit-conc-seq "or") (lit-conc-seq "||")) nil)) 
                            (invisi-conc v (add-op-to-preds :or)) 
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
            (conc (alt (conc (opt (invisi-conc (alt (lit \∀) (lit-conc-seq "all:")) (set-info :all true))) ; ALL modificator.
                             (change-pred pred-id :ids))
                       (pfunc-as-param :ids))
                  (change-pred sign :func) 
                  (invisi-conc value (set-info :all nil)))))
(def t-prime (alt (conc (sur-by-ws (add-pred (alt (lit-conc-seq "and") (lit-conc-seq "&&")))) 
                        (invisi-conc f (add-op-to-preds :and))
                        t-prime) emptiness))
(def t (conc f t-prime))
(def where-prime (alt (conc (sur-by-ws (add-pred (alt (lit-conc-seq "or") (lit-conc-seq "||"))))
                            (invisi-conc t (add-op-to-preds :or))
                            where-prime) emptiness))
(def where (conc t where-prime)) 


(def block-where
  "Defines where clause."
  (conc (add-pred (lit \#)) (invisi-conc where change-preds)))

(def props
  "Defines sequences of properties of an object."
  (conc (invisi-conc (lit \[) (update-info :then-level inc)) 
        (rep+ (alt (sur-by-ws (conc idsort 
                                    (pfunction
                                      #(partial set-id (peek %) found-prop) 
                                      (update-info :function #(pop %)))))
                   (sur-by-ws (conc (opt (invisi-conc (lit \*) (set-info :is-recur true)))
                                    (invisi-conc (conc idsort (process-id found-prop)) (set-info :is-recur false))))))
        (invisi-conc (lit \]) (update-info :then-level dec))))


(def process-fn
  "Processes function name."
  (complex [n (partial text [\space \newline \tab])
            _ (update-info :function 
                           #(if-let [f (let [sym (symbol (reduce str "" n))]
                                         (some (fn [ns-] (ns-resolve ns- sym)) (all-ns)))]
                              (conj (pop %) (assoc (peek %) :func f))
                              (throw (NotFoundFunctionException. (str "Could not found function " (reduce str "" n) ".")))))]
           n))


(declare params, param, param-query, pnumber, pstring, parse+, pfunc, pid, pself pparam)
(def function
  "Defines YZ's function."
  (conc (invisi-conc (lit \@) (update-info :function #(conj % empty-fun)))
        (lit \() process-fn params (lit \))))


(def params
  "Defines sequense of parameters of YZ's function."
  (alt (conc param params) emptiness))


(def param
  "Defines different types of function's parameters."
  (sur-by-ws (alt pstring pnumber pparam param-query pfunc pself pid)))


(defn- f-mod
  "Generates code for processing modificator 
  of function's parameter-query."
  [long-mod short-mod fm]
  (invisi-conc (alt (lit-conc-seq long-mod) 
                    (lit-conc-seq short-mod)) 
               (set-info :f-modificator fm)))


(def dep-each (f-mod "dep-each:" "de:" :dep-each))
(def dep-list (f-mod "dep-list:" "dl:" :dep-list))
(def indep-each (f-mod "indep-each:" "ie:" :indep-each))
(def indep-list (f-mod "indep-list:" "il:" :indep-list))


(def param-query
  "Defines parameter as query."
  (conc (opt (alt dep-each dep-list indep-each indep-list))
        (alt (complex [ret value-as-param
                       fm (get-info :f-modificator)
                       fm (effects (if (nil? fm) :dep-list fm))
                       _ (do (swap! query-params conj nil) 
                           (update-param [fm (keyword (reduce str "" (second ret)))]))
                       _ (set-info :f-modificator nil)]
                      ret)
             (conc
               (lit start-pq)
               (complex [ret textq
                         nl (get-info :nest-level)
                         tl (get-info :then-level)
                         res (get-info :result)
                         q (effects (:result (parse+ ret mom)))
                         q (effects (vec (map #(nnassoc % 
                                                        :where 
                                                        (get-paths (:what %) (get-in-nest res nl :what))) q)))
                         fm (get-info :f-modificator)
                         fm (effects (if (nil? fm) :dep-list fm))
                         _ (update-param [fm q])
                         _ (set-info :f-modificator nil)]
                        ret)
               (lit end-pq)))))


(def pnumber
  "Defines param as number."
  (complex [n number
            _ (update-param (let [n- (reduce str "" (flatten n))] 
                              (try (Integer/parseInt n-)
                                (catch Exception e (Double/parseDouble n-)))))]
           n))


(def pstring
  "Defines param as string."
  (complex [s string
            _ (update-param (nth s 1))]
           s))

(def pfunc
  "Defines parameter of function as another function."
  (complex [f function
            f-m (get-info :function)
            _ (update-info :function #(pop %))
            _ (update-param (peek f-m))]
           f))


(def pid
  "Defines parameter as some id."
  (complex [id (rep+ alpha)
            _ (update-param (symbol (reduce str id)))]
           id))


(def pself
  "Defines param as self object or property of one."
  (complex [s (conc (lit \&) (alt (conc (lit \.) (rep* alpha)) emptiness))
            _ (update-param (reduce str "" (flatten s)))]
           s))


(def pparam
  "Defines param as parameter."
  (complex [s value-as-param
            _ (do (swap! query-params conj nil) 
                (update-param (keyword (reduce str "" (second s)))))]
           s))

(def funcq
  "Defines rule for query as function."
  (pfunction 
    ; Deletes top of the result stack and puts structure which corresponds 
    #(update-info :result (fn [r] (conj (pop r) (peek %))))
    (update-info :function #(pop %))))


(def query
  "Defines start symbol for parsing query."
  (alt (conc funcq ; Query may be simple function: @(count `room')
             (rep* (conc delimiter query))) ; or function + some query: @(count `room'), room

       (conc bid ; or id's sequence with properties or predicates: room; room.floor; room[name]; room#(name="MB") and so on.
         (rep* ; or  sequence from the following parts.
           (alt
             nest-query ; Nested query: room (floor)
             (conc delimiter query)))))) ; Next query: room, floor


(defn do-q
  "Takes query and runs query rule with this query."
  [q]
  (let [q (if (string? q) q (reduce str q))
        q (cs/trim q)]
    ((query (struct q-representation (seq q) 
                    empty-res 0 0 [] nil [] 
                    false empty-pred nil nil nil)) 1)))


(defn parse+
  "Like parse, but returns all structure of result."
  [^String q, ^PersistentArrayMap mom]
  (do (def mom mom)
    (reset! query-params [])
    (do-q q)))


(defn parse
  "Parses specified query ('q') in YZ language. 
  Parsing is based on specified ('mom') map of an object model.
  Returns an inner representation of the query."
  [q, mom]
  (let [r (parse+ q, mom)]
    (if (nil? (:remainder r))
      (:result r)
      (throw (SyntaxException. (str "Syntax error near: " (reduce str "" (:remainder r))))))))


