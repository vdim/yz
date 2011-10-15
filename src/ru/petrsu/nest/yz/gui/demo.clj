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

(ns ru.petrsu.nest.yz.gui.demo
  ^{:author "Vyacheslav Dimitrov"
    :doc "This code implements GUI for the demonstration using YZ."}
  (:use ru.petrsu.nest.yz.functions)
  (:require [ru.petrsu.nest.yz.core :as c]
            [ru.petrsu.nest.yz.hb-utils :as hb])
  (:import (javax.swing JPanel JTable JScrollPane 
                        JFrame JTextField JOptionPane)
           (java.awt Insets GridBagConstraints GridBagLayout)
           (javax.swing.table TableModel AbstractTableModel)
           (java.awt.event KeyEvent KeyAdapter)))


(def ^{:dynamic true} *q-history* (ref []))
(def ^{:dynamic true} *current-q* (ref 0))

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


(defn table-model [rows c-names]
  "Implements TableModel for querie's representation."
  (let [colcnt (reduce max (map count rows))
	rowcnt (count rows)]
    (proxy [TableModel] []
      (addTableModelListener [tableModelListener])
      (getColumnClass [columnIndex] Object)
      (getColumnCount [] colcnt)
      (getColumnName [columnIndex]
                     (nth c-names columnIndex))
      (getRowCount [] rowcnt)
      (getValueAt [rowIndex columnIndex]
                  (let [row (nth rows rowIndex)]
                    (if (< (dec (count row)) columnIndex)
                      nil
                      (row columnIndex))))
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

(def mom (hb/mom-from-file "nest.mom"))
(def em (.createEntityManager (javax.persistence.Persistence/createEntityManagerFactory "nest")))


(defn- create-qtext
  "Create jtextfield for the input query."
  [rtable]
  (let [qtext (JTextField.)]
    (add-key-released-listener 
      qtext 
      (fn [e] 
        (if (= (.getKeyCode e) KeyEvent/VK_UP)
          (let [cur-q (dec @*current-q*)]
            (if (>= cur-q 0)
              (dosync 
                (.setText qtext (@*q-history* cur-q))
                (ref-set *current-q* cur-q)))))

        (if (= (.getKeyCode e) KeyEvent/VK_DOWN)
          (let [cur-q (inc @*current-q*)]
            (if (< cur-q (count @*q-history*))
              (dosync 
                (.setText qtext (@*q-history* cur-q))
                (ref-set *current-q* cur-q)))))

        (if (= (.getKeyCode e) KeyEvent/VK_ENTER)
          (let [text (.getText qtext) 
                qr (c/pquery text mom em)]
            (if (nil? (:error qr))
              (.setModel rtable (table-model (:rows qr)
                                             (:columns qr)))
              (JOptionPane/showMessageDialog rtable (:error qr)))
            (dosync 
              (ref-set *q-history* (conj @*q-history* text))
              (ref-set *current-q* (dec (count @*q-history*))))))))
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



