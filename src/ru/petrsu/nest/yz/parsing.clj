(ns ru.petrsu.nest.yz.parsing
  ^{:author Vyacheslav Dimitrov
    :doc "Code for the parsing of queries does due to the fnparse library."}
  (:use name.choi.joshua.fnparse))

; The parsing state data structure. The rest of input string is stored
; in :remainder, and vector of maps is stored in :result.
(defstruct q-representation :remainder :result)

; Rules of grammar are below. See BNF in the begining of file.
(def alpha
  ^{:doc "Sequence of characters."}
  (lit-alt-seq "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"))

(def whitespaces
  ^{:doc "List of whitespaces"}
  (rep+ (alt (lit \space) (lit \newline))))

(def id 
  (complex [id# (rep+ alpha)
            g-res (get-info :result)
            s-res (set-info :result (concat g-res [(reduce str id#)]))]
           id#))

(def delimiter
  (alt (lit \.) (lit \,)))

(def query
  (conc id (rep+ (conc delimiter id))))

(defn parse
  "Parses specified query on YZ language."
  [q]
  (query (struct q-representation (seq q) {})))


(parse "building.room,device")
