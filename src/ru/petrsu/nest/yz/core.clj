(ns ru.petrsu.nest.yz.core
  ^{:author Vyacheslav Dimitrov
    :doc "This code contains core functions of the Clojure's implementation of the YZ language.
         The Parsing of queries does due to the fnparse library."}
  (:use name.choi.joshua.fnparse))

; The parsing state data structure. The rest of input string is stored
; in :remainder, and list of maps (key is token, value is value of token) of tokens is
; stored in :tokens.
(defstruct generate-tokens :remainder :tokens)

; Rules of grammar are below. See BNF in the begining of file.
(def alpha
  ^{:doc "Sequence of characters."}
  (lit-alt-seq "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"))

(def id 
  (complex [id# (rep+ alpha)
            tokens (get-info :tokens)]
           id#))


(defn get-class
  "Gets name of class, finds this class and returns instance of java.lang.Class.
  If search of class fails then nil is returned."
  [id]
  (try 
    (Class/forName id) 
    (catch ClassNotFoundException _ nil)))

