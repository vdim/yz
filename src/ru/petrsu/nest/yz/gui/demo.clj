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


(defn get-column-count
  "Returns the number of column for specified result of query."
  [data]
  (loop [data- data res 1]
    (if (empty? (data- 1))
      res
      (recur ((data- 1) 0) (inc res)))))


(defn get-rows
  "Returns set of rows. The 'data' is the result of 
  processing a query."
  ([data]
   (get-rows data ()))
  ([data & args]
   (if (empty? (data 0))
     (list (vec (flatten args)))
     (mapcat (fn [o]
            (if (empty? (o 1))
              (for [pair (partition 2 o)] (vec (flatten [args pair])))
              (mapcat #(myf (nth % 1) args (nth % 0)) (partition 2 o)))) 
          data))))


(defn table-model [data c-names]
  "Implements TableModel for querie's representation."
  (let [rows (get-rows data)
        colcnt (get-column-count data)
	rowcnt (count rows)]
    (proxy [TableModel] []
      (addTableModelListener [tableModelListener])
      (getColumnClass [columnIndex] Object)
      (getColumnCount [] colcnt)
      (getColumnName [columnIndex]
                     (nth c-names columnIndex))
      (getRowCount [] rowcnt)
      (getValueAt [rowIndex columnIndex]
                  ((nth rows rowIndex) columnIndex))
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



