(ns ru.petrsu.nest.yz.parsing
  ^{:author Vyacheslav Dimitrov
    :doc "Code for the parsing of queries does due to the fnparse library."}
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


