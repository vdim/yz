(ns net.kryshen.planter.store
  ^{:author "Michail Kryshen"
    :doc "Store beans in files."} 
  (:use
   net.kryshen.planter.core)
  (:require
   (clojure.java [io :as io]))
  (:import
   net.kryshen.planter.core.RefBean
   (java.io File PushbackReader)))

(defrecord Store
    [data-dir
     log-writer
     ;; managed beans, ref to map: id -> object
     beans
     ;; ref to set of changed beans
     changed
     agent])

(defn store [data-dir]
  (->Store data-dir
           (let [file (io/file data-dir "log")]
             (io/make-parents file)
             (io/writer file))
           (ref nil)
           (ref #{})
           (agent nil)))

(def ^:private ^:dynamic *store*)

(def ^:private ^:dynamic *saving*
  "The bean being saved."
  nil)

(defn- log [store & objs]
  (binding [*out* (:log-writer store)]
    (apply println objs)))

(defn- bean-file
  ([store bean]
     (bean-file store (class bean) (bean-id bean)))
  ([store ^Class cls id]
     (io/file (io/file (:data-dir store) (.getName cls)) (str id))))

(defn- load-properties
  "Reads and returns bean properties from the data store."
  [store bean]
  (binding [*store* store]
    (let [file (bean-file store bean)]
      (log store "Loading" (str file))
      (with-open [in (io/reader file)]
        (read (PushbackReader. in))))))

(defn- bean-change
  "Handle bean change. Must be called in transaction."
  [store bean _old new]
  (let [beans (:beans store)
        changed (:changed store)
        change-watch (partial bean-change store)
        handle-change (fn handle-change [props]
                        (let [rels (related props)
                              rels (remove #(contains? @beans (bean-id %))
                                           rels)
                              id-rels (map #(vector (bean-id %) %) rels)]
                          (alter changed into rels)
                          (alter beans into id-rels)
                          ;; Invoke change handler for all newly
                          ;; discovered beans.
                          (doseq [rel rels]
                            (add-change-watch rel change-watch)
                            (handle-change (properties rel)))))]
        (alter changed conj bean)
        (handle-change new)))

(defn register-bean [store bean]
  (dosync
   (alter (:beans store) assoc (bean-id bean) bean)
   (add-change-watch bean (partial bean-change store))
   (bean-change store bean nil (properties bean))))

(defn bean-instance
  "Returns bean instance specified by class and id."
  ([cls id]
     (bean-instance *store* cls id))
  ([store ^Class cls id]
     (let [beans (:beans store)]
       (or
        (.cast cls (get @beans id))
        (dosync
         (or
          (.cast cls (get @beans id))
          (let [b (.newInstance
                   (.getConstructor cls (into-array [Object Object]))
                   (object-array [id (partial load-properties store)]))]
            (add-change-watch b (partial bean-change store))
            (alter beans assoc id b)
            b)))))))

(defn- store-contents
  "Returns a sequence of sequences (class & ids) describing the
  contents of the data store."
  [store]
  (->> store :data-dir io/file .listFiles
       (filter (fn [^File f] (.isDirectory f)))
       (map (fn [^File f]
              (cons (Class/forName (.getName f))
                    (lazy-seq (.list f)))))))

(defn instances-of
  "Returns sequence of instances of the specified class."
  [store ^Class cls]
  ;; Instantiate all stored beans of classes assignable to cls.
  (doseq [[c & ids] (store-contents store)]
    (if (.isAssignableFrom cls c)
      (doseq [id ids]
        (bean-instance store c id))))
  (filter (partial instance? cls) (vals @(:beans store))))

(defn- save-properties!
  [store cls id properties]
  (io! "Writing to the data store while transaction is running.")
  (let [file (bean-file store cls id)]
    (log store "Writing" (str file))
    (io/make-parents file)
    (with-open [fw (io/writer file)]
      (binding [*print-dup* true
                *verbose-defrecords* true
                *out* fw]
        (pr properties)))))

(defn- collect-changes
  "Returns [class id properties] for each changed bean, clears the set
   of changed beans. Must be called in transaction."
  [store]
  (let [changed (:changed store)]
    ;; Repeat until there are no more new changes.
    (loop [data nil]
      (if-let [beans (seq @changed)]
        (do
          (ref-set changed #{})
          (recur
           (into data
                 (map #(binding [*saving* %]
                         [(class %) (bean-id %) (properties %)])
                      beans))))
        data))))

(defn- save-changes!
  ([store]
     (save-changes! store (dosync (collect-changes store))))
  ([store changes]
     (log store "save-changes!")
     (doseq [data changes]
       (apply save-properties! store data))))

(defn save-all
  "Writes all changes to the store. Returns immediately."
  ([store]
     (log store "save-all")
     (send-off (:agent store) (fn [_] (save-changes! store))))
  ([store changes]
     (log store "save-all")
     (send-off (:agent store) (fn [_] (save-changes! store changes)))))

(defn save-all-and-wait [store]
  (log store "save-all-and-wait")
  (io! "save-all-and-wait called in transaction.")
  (save-all store)
  (loop []
    (let [store-agent (:agent store)
          w (await-for 1000 store-agent)]
      (log store "save-all-and-wait: await-for" w)
      (when-let [e (agent-error store-agent)]
        (log store "save-all-and-wait: agent-error" e)
        (restart-agent store-agent nil)
        (throw e))
      (if-not w
        (recur)))))

(defmethod print-dup RefBean
  [bean ^java.io.Writer writer]
  (.write writer "#=(")
  (.write writer (str `bean-instance))
  (.write writer " ")
  (print-dup (class bean) writer)
  (.write writer " ")
  (print-dup (bean-id bean) writer)
  (.write writer ")"))
