(ns ru.petrsu.nest.yz.gui.demo
  ^{:author Vyacheslav Dimitrov
    :doc "This code implements GUI for the demonstration using YZ."}
  (:require [ru.petrsu.nest.yz.parsing :as p])
  (:import (javax.swing JPanel JTree JTable JScrollPane 
                        JFrame JToolBar JButton SwingUtilities JTextField)
           (java.awt Insets GridBagConstraints)
           (javax.swing.table TableModel AbstractTableModel)
           (java.awt.event KeyEvent KeyAdapter)))


(defn- create-gbc
  "Creates instanse of the GridBagConstraints."
  ([]
   (create-gbc 0))
  ([weighty]
   (GridBagConstraints. 
     GridBagConstraints/RELATIVE ; gridx
     GridBagConstraints/RELATIVE ; gridy
     GridBagConstraints/REMAINDER ; gridwidth
     1 ; gridheight
     1 ; weightx
     weighty ; weighty
     GridBagConstraints/CENTER ; anchor
     GridBagConstraints/BOTH ; fill
     (Insets. 5 5 5 5) ; insets
     0 ; ipadx
     0))) ; ipady


(defn get-row-count
  "Returns the number of row for specified result of query."
  [data]
  (if (empty? data)
    1
    (if (empty? (data 1))
      (/ (count data) 2)
      (reduce #(if (vector? %2)
                 (+ %1 (get-row-count (%2 0)))
                 %1) 0 data))))


(defn get-column-count
  "Returns the number of column for specified result of query."
  [data]
  (loop [data- data res 1]
    (if (empty? (data- 1))
      res
      (recur ((data- 1) 0) (inc res)))))


(defn get-value
  [data, row, column]
  ())

(defn get-rows-
  [data]
  (if (empty? data)
    []
    (if (empty? (data 1))
      (for [a (partition 2 data)] (first a))
      (reduce #(if (vector? %2)
                 (conj %1 (get-rows (%2 0)))
                 %1) [] data))))

;(defn get-rows
;  [data]
;  (loop [data- data res [] rst (rest (rest data))]
;    (if (or (empty? rst) (nil? rst))
;      (reduce #(conj %1 (conj res (first %2))) [] (partition 2 data-)
;      (if (empty? (data- 1))
;        (recur rst res  (rest (rest rst)))
;        (recur ((data- 1) 0) (conj res (data- 0))  rst))))))
;  (reduce #(conj %1 (first %2)) [] (partition 2 (data 0))))

(defn myf
  ([data]
   (myf data ()))
  ([data & args]
   (if (empty? (data 0))
     (list (vec (flatten args)))
     (mapcat (fn [o]
            (if (empty? (o 1))
              (for [b (partition 2 o)] (vec (flatten [args b])))
              (mapcat #(myf (nth % 1) args (nth % 0)) (partition 2 o)))) 
          data))))


(defn myff
  [data]
  (let [d (myf data)]
    (loop [d- d res [] dd- nil]
      (if (vector? (nth d- 0))
        (reduce #(conj %2 %1) [] dd-)
        (recur (reduce #(concat %1 %2) d-) res d-)))))

(defn myff-
  ([data]
   (if (vector? data)
     (do (println data) 1)
     (map #(myff- %) data))))


(defn table-model [data c-names]
  "Implements TableModel for querie's representation."
  (let [colcnt (get-column-count data)
	rowcnt (get-row-count data)]
    (proxy [TableModel] []
      (addTableModelListener [tableModelListener])
      (getColumnClass [columnIndex] Object)
      (getColumnCount [] colcnt)
      (getColumnName [columnIndex]
                     (nth c-names columnIndex))
      (getRowCount [] rowcnt)
      (getValueAt [rowIndex columnIndex]
                  (get-value data rowIndex columnIndex))
      (isCellEditable [rowIndex columnIndex] false)
      (removeTableModelListener [tableModelListener]))))


(defn add-key-released-listener
    "Adds a KeyListener to component that only responds to KeyReleased events.
    When a key is typed, f is invoked with the KeyEvent as its first argument
    followed by args. Returns the listener."
    [component f & args]
    (let [listener (proxy [KeyAdapter] []
                     (keyReleased [event] (apply f event args)))]
      (.addKeyListener component listener)
      listener))

(defn- create-qtext
  "Create jtextfield for the input query."
  [rtext]
  (let [qtext (JTextField.)]
    (add-key-released-listener 
      qtext 
      (fn [e] 
        (if (= (.getKeyCode e) KeyEvent/VK_ENTER) 
          (println "HEY"))))
    qtext))

(defn- create-pane
  "Creates pane with jtextfield for query 
  and jtable for result of query."
  []
  (let [rtable (JTable.)]
    (doto (JPanel.)
      (.setLayout (GridBagLayout.))
      (.add (JScrollPane. (create-qtext rtable)) (create-gbc 0))
      (.add (JScrollPane. rtable) (create-gbc 0.95)))))

(defn demo
  "Runs gui for the demonstration using YZ."
  []
  (doto (JFrame. "Demo YZ")
    (.add (create-pane))
    (.setSize 600 400)
    (.setVisible true)))



