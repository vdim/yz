(ns ru.petrsu.nest.yz.map-utils
  ^{:author Vyacheslav Dimitrov
    :doc "Helper functions for working with map of object model (MOM).
         MOM has simple ini-format and represents by Clojure's map, 
         where key is full name of class (or another block of data, e.g. packages, funcs, scritps) 
         and value is map with values of this class, e,g. short name (\"SN\"), default property
         (\"DP\") and so on."}
  {:use clojure.set})

(defn find-in-sn
  "Finds class by id in short name (\"SN\") fields of 
  classes's definitions in MOM. If search fails is 
  returned, otherwise Class instanse is returned."
  [id, mom]
  (if (empty? mom) 
    nil 
    (let [[f-key f-map] (first mom)] 
      (if (= (:sn f-map) id) 
        f-key 
        (find-in-sn id (rest mom))))))
