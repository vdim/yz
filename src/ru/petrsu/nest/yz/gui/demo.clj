(ns ru.petrsu.nest.yz.gui.demo
  ^{:author Vyacheslav Dimitrov
    :doc "This code implements GUI for the demonstration using YZ."}
  (:require [ru.petrsu.nest.yz.parsing :as p])
  (:import (javax.swing JPanel JTree JTable JScrollPane 
                        JFrame JToolBar JButton SwingUtilities JTextField)
           (java.awt Insets GridBagConstraints)))


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

(defn- create-pane
  "Creates pane with jtextfield for query 
  and jtable for result of query."
  []
  (doto (JPanel.)
    (.setLayout (GridBagLayout.))
    (.add (JScrollPane. (JTextField.)) (create-gbc 0))
    (.add (JScrollPane. (JTable.)) (create-gbc 0.95))))

(defn demo
  "Runs gui for the demonstration using YZ."
  []
  (doto (JFrame. "Demo YZ")
    (.add (create-pane))
    (.setSize 600 400)
    (.setVisible true)))



