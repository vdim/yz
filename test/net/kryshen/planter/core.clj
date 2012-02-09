(ns net.kryshen.planter.core
  ^{:author "Michail Kryshen"
    :doc "The planter that grows Java beans."}
  (:import
   java.util.UUID))

(definterface RefBean
  (internalState [])
  (bindings [])
  (multivaluedness [])
  (defaults []))

(definterface PropertyAccessListener
  (^void propertyAccess [^net.kryshen.planter.core.RefBean bean
                         ^clojure.lang.Keyword property]))

(defrecord BeanSpec [classname name properties mixin specs])
;; specs: map mixin-or-interface-or-Object -> (overrides*)

(defrecord BeanState [id properties loader access-listeners change-fns])

(defn bean-id [^RefBean bean]
  (:id (.internalState bean)))

(defn properties-ref [^RefBean bean]
  (:properties (.internalState bean)))

(defn- loader [^RefBean bean]
  (:loader (.internalState bean)))

(defn- load-deref [bean ref]
  (or @ref (ref-set ref ((loader bean) bean))))

(defn- fire-property-access
  "Invokes property access listeners associated with the specified bean."
  [^RefBean bean property]
  (doseq [listener @(:access-listeners (.internalState bean))]
    (.propertyAccess ^PropertyAccessListener listener bean property)))

(defn properties
  "Returns bean properties."
  [bean]
  (fire-property-access bean nil)
  (let [ref (properties-ref bean)]
    (or @ref (dosync (load-deref bean ref)))))

(defn add-change-watch [^RefBean bean f]
  (dosync
   (alter (:change-fns (.internalState bean)) conj f)))

(defn- fire-change [^RefBean bean removed added]
  (doseq [change-fn @(:change-fns (.internalState bean))]
    (change-fn bean removed added)))

(defn related [properties]
  (filter (partial instance? RefBean)
          (mapcat #(if (coll? %) (seq %) [%]) (vals properties))))

(defn- array-class
  "Returns array type corresponding to the given class or symbol."
  [^Class cls]
  (class (java.lang.reflect.Array/newInstance cls 1)))

(defmethod print-dup UUID
  [uuid ^java.io.Writer writer]
  (.write writer "#=(java.util.UUID/fromString \"")
  (.write writer (str uuid))
  (.write writer "\")"))

(defmethod print-dup (array-class Byte/TYPE)
  [bytes ^java.io.Writer writer]
  (.write writer "#=(clojure.core/byte-array ")
  (.write writer "#=(clojure.core/vector-of :byte")
  (doseq [b bytes]
    (.write writer (str \space b)))
  (.write writer "))"))

(defn multivalued? [^RefBean bean key]
  (key (.multivaluedness bean)))

(defn property-binding [^RefBean bean key]
  (key (.bindings bean)))

(defn- remove-values
  "Remove vals from coll or disjoin if coll is a set."
  [coll vals]
  (if (set? coll)
    (apply disj coll vals)
    (into (empty coll)
          (remove (set vals) coll))))

(defn set-property [bean key value]
  (dosync
   (fire-property-access bean key)
   (let [ref (properties-ref bean)
         o (get (load-deref bean ref) key)]
     (when (not= o value)
       (alter ref assoc key value)
       (fire-change bean {key o} {key value})))))

(defn get-property [bean key]
  (key (properties bean)))

(defn set-property-array [bean key array]
  (dosync
   (fire-property-access bean key)
   (let [ref (properties-ref bean)
         c (get (load-deref bean ref) key)
         cn (into (empty c) (seq array))]
     (when (not= c cn)
       (alter ref assoc key cn)
       (fire-change bean {key c} {key cn})))))

(defn get-property-array [bean key type]
  (into-array type (get-property bean key)))

(defn property-add [bean key & values]
  (dosync
   (fire-property-access bean key)
   (let [ref (properties-ref bean)
         c (get (load-deref bean ref) key)
         cn (apply conj c values)]
     (when (not= c cn)
       (alter ref assoc key cn)
       (fire-change bean nil {key (into (empty c) values)})))))

(defn property-remove [bean key & values]
  (dosync
   (fire-property-access bean key)
   (let [ref (properties-ref bean)
         c (get (load-deref bean ref) key)
         cn (remove-values c values)]
     (when (not= c cn)
       (alter ref assoc key cn)
       (fire-change bean {key (into (empty c) values)} nil)))))

(defn- property-unbind
  [bean key value]
  (if (multivalued? bean key)
    (property-remove bean key value)
    (set-property bean key nil)))

(defn- property-bind
  [bean key value]
  (if (multivalued? bean key)
    (property-add bean key value)
    (set-property bean key value)))

(defn bind-changes
  "Applies property bindings. Must be called in transaction."
  [bean removed added]
  ;; (binding [*out* *err*]
  ;;   (println bean removed added))
  (doseq [[k v] removed]
    (when-not (nil? v)
      (when-let [bind (property-binding bean k)]
        (if (multivalued? bean k)
          (doseq [vk v]
            (property-unbind vk bind bean))
          (property-unbind v bind bean)))))
  (doseq [[k v] added]
    (when-not (nil? v)
      (when-let [bind (property-binding bean k)]
        (if (multivalued? bean k)
          (doseq [vk v]
            (property-bind vk bind bean))
          (property-bind v bind bean))))))

(defn property-accessor
  "Returns accessor fn for the specified property."
  ([f key]
     (fn
       ([bean]
          (f bean key))
       ([bean value]
          (f bean key value))))
  ([f key component-type]
     (fn [bean]
       (f bean key component-type))))

(defn- capitalize-first
  "Capitalize the first character of non-empty string."
  [s]
  (str (Character/toUpperCase ^Character (first s))
       (subs s 1)))

(defn- to-single
  [property]
  (let [m (meta property)
        n (str property)
        single (or (:single property)
                   (cond
                    (.endsWith n "ies") (str (subs n 0 (- (count n) 3)) "y")
                    (.endsWith n "ses") (subs n 0 (- (count n) 2))
                    (.endsWith n "s") (subs n 0 (dec (count n)))
                    :else n))
        single (with-meta (symbol single) m)]
    single))

(defn- method-prefix [bean-name]
  (str bean-name ":"))

(defn- method-symbol [bean-name method-name]
  (symbol (str (method-prefix bean-name) method-name)))

(defn- getter-prefix [type]
  (if (= (str type) "boolean") "is" "get"))

(defn- property-methods
  "Returns descriptions of accessor methods for the property.
   Descriptions are vectors [name [param-types] return-type f]."
  [bean-name property]
  (let [kwd (keyword property)
        m (meta property)
        {:keys [many tag array-type component-type bind]} m
        type (if many array-type tag)
        capitalized (capitalize-first (str property))
        single (capitalize-first (str (to-single property)))]
    (if many
      [[(str "set" capitalized)
        [type] 'void
        `(property-accessor set-property-array ~kwd)]
       [(str "set" capitalized)
        [Iterable] 'void
        `(property-accessor set-property-array ~kwd)]
       [(str (getter-prefix type) capitalized)
        [] type
        `(property-accessor get-property-array ~kwd ~component-type)]
       [(str "add" single)
        [component-type] 'void
        `(property-accessor property-add ~kwd)]
       [(str "remove" single)
        [component-type] 'void
        `(property-accessor property-remove ~kwd)]]
      [[(str "set" capitalized)
        [type] 'void
        `(property-accessor set-property ~kwd)]
       [(str (getter-prefix type) capitalized)
        [] type
        `(property-accessor get-property ~kwd)]])))

(defn- bean-methods [namespace bean-name properties]
  (mapcat (partial property-methods bean-name) properties))

(def ^:private primitive-types
  [Float/TYPE Integer/TYPE Long/TYPE Boolean/TYPE Character/TYPE
  Double/TYPE Byte/TYPE Short/TYPE Void/TYPE])

(def ^:private primitive-syms
  (set (map #(symbol (.getName ^Class %)) primitive-types)))

(def ^:private primitive-array-syms
  (set (map #(symbol (str % "s")) primitive-syms)))

(defn- bean-spec? [obj]
  (:classname obj))

(defn- resolve-bean
  "Returns Class, BeanSpec or a symbol describing primitive"
  ([ns symbol]
     (if (or (primitive-syms symbol)
             (primitive-array-syms symbol))
       symbol
       (let [r (ns-resolve ns symbol)]
         (cond
          (class? r) r
          (var? r) (let [s (var-get r)]
                     (if (or (bean-spec? s) (::plural s))
                       s
                       (throw (Exception. (str symbol " is not a bean")))))
          (nil? r) (throw (Exception. (str "Could not resolve " symbol)))
          :else (throw (Exception. (str "Unknown type: " symbol))))))))

(defn- resolve-tag
  [namespace sym]
  (let [m (meta sym)
        type (:tag m)
        type (and type (resolve-bean namespace type))
        many (::plural type)
        type (or (::plural type) type)
        type-spec (and (bean-spec? type) type)
        type (if type-spec
               (Class/forName (:classname type-spec))
               type)]
    (if many
      (vary-meta sym assoc
                 :tag Object
                 :type-spec type-spec
                 :many true
                 :array-type (array-class type)
                 :component-type type)
      (vary-meta sym assoc
                 :tag (or type Object)
                 :type-spec type-spec))))

(defn- parse-opts+specs
  "Returns [opts specs], where opts is a map of options and specs is
  sequence of specs."
  [opts+specs]
  (loop [opts+specs opts+specs
         opts {}]
    (if (keyword? (first opts+specs))
      (recur (nnext opts+specs)
             (assoc opts (first opts+specs) (second opts+specs)))
      [opts opts+specs])))

(defn- parse-specs
  "Returns map type-or-interface-or-Object -> list of method specs."
  [ns raw-specs]
  (loop [raw raw-specs
         specs nil
         current Object]
    (if-let [[elem & raw] raw]
      (if (list? elem)
        (recur raw (update-in specs [current] conj elem) current)
        (let [resolved (resolve-bean ns elem)]
          (recur raw (assoc specs resolved nil) resolved)))
      specs)))
    
(defn- bean-classname [ns name]
  (str (namespace-munge ns) "." name))

(defn plural-type-spec [type]
  {::plural type})

(defn- spec-method-fns
  "Returns seq of pairs [name fn-form-or-symbol]."
  ([name specs]
     (spec-method-fns name specs #{} true))
  ([name specs exclude fn?]
     (apply concat
      (for [[parent methods] specs]
        (concat
         (for [[mname [& args] & body] methods
               :when (not (exclude mname))]
           (if fn?
             [mname `(fn [~@args] ~@body)]
             [mname (method-symbol name mname)]))
         (spec-method-fns (:name parent) (:specs parent)
                          (into exclude (map first methods))
                          false))))))

(defmacro defbean
  "Defines JavaBean with the specified name and property specs."
  [name [& properties] & opts+specs]
  (let [[opts specs] (parse-opts+specs opts+specs)
        specs (parse-specs *ns* specs)
        spec-methods (spec-method-fns name specs)
        mixin (:mixin opts)
        classname (or (:classname opts)
                      (bean-classname *ns* name))]
    `(do
       (let [spec# (->BeanSpec ~classname '~name '~properties
                               ~mixin '~specs)]
         (def ~(with-meta name {:bean-spec true}) spec#)
         (def ~(symbol (str name "*")) (plural-type-spec spec#)))
       ~@(for [[mname f] spec-methods]
           `(def ~(method-symbol name mname) ~f)))))

(defmacro defmixin
  [name [& properties] & opts+specs]
  `(defbean ~name ~properties ~@(concat '(:mixin true) opts+specs)))

(defn- resolve-binding
  [namespace sym]
  (let [m (meta sym)
        bind (:bind m)
        spec (:type-spec m)]
    (if (and bind spec)
      (let [bind-sym (first (filter (partial = bind) (:properties spec)))
            bind (resolve-tag namespace bind-sym)]
        (if bind
          (vary-meta sym assoc :bind bind)
          (throw (Exception. (str "Could not resolve property binding: "
                                  bind-sym)))))
      sym)))

(defn- mix-in [key-fn bean-spec]
  (if-let [extends (->> bean-spec
                        :specs
                        keys
                        (filter bean-spec?)
                        seq)]
    (apply concat
           (key-fn bean-spec)
           (map (partial mix-in key-fn) extends))
    (key-fn bean-spec)))

(defn- merge-specs [specs]
  (loop [specs specs,
         seen #{}
         result nil]
    (if-let [[spec & specs] specs]
      (if (seen (first spec))
        (recur specs seen result)
        (recur specs (conj seen (first spec)) (conj result spec)))
      result)))

(defn- bean-class
  [ns class-or-bean-spec]
  (if (class? class-or-bean-spec)
    class-or-bean-spec
    (ns-resolve ns (symbol (:classname class-or-bean-spec)))))

(defn- bean-generator
  "Returns a form that generates bean class from a BeanSpec."
  [ns bean-spec]
  (let [{:keys [classname name mixin]} bean-spec
        properties (mix-in :properties bean-spec)
        properties (map (partial resolve-tag ns) properties)
        properties (map (partial resolve-binding ns) properties)
        record-sym (symbol (str name "Record"))
        bean-methods (bean-methods ns name properties)
        gen-methods (map #(subvec % 0 3) bean-methods)
        defaults (map (comp :init meta) properties)
        implement (map (partial bean-class ns)
                       (mix-in (comp keys :specs) bean-spec))
        implement (cons RefBean (remove (partial = Object) implement))]
    (if mixin
      `(gen-interface :name ~classname
                      :extends [~@implement]
                      :methods [~@gen-methods])
      `(do
         (defrecord ~record-sym [~@(map #(with-meta % nil) properties)])
         (gen-class :name ~classname
                    :state "_state"
                    :init "init"
                    :constructors {[] []
                                   [Object] []
                                   [Object Object] []}
                    :methods [~@gen-methods]
                    :implements [~@implement]
                    :prefix ~(method-prefix name))
         ;; Property accessors
         ;; FIXME: define accessor fns in defbean.
         ~@(for [[mname _ _ f] bean-methods]
             `(def ~(method-symbol name mname) ~f))
         ;; State accessor
         (defn ~(method-symbol name "internalState")
           [~(with-meta 'bean {:tag classname})]
           (._state ~'bean))
         ;; Bean properties information
         (def ~(method-symbol name "multivaluedness")
           (constantly
            ~(list* `new record-sym
                    (map (comp :many meta) properties))))
         (def ~(method-symbol name "bindings")
           (constantly
            ~(list* `new record-sym
                    (map (comp keyword :bind meta) properties))))
         (def ~(method-symbol name "defaults")
           (constantly (new ~record-sym ~@defaults)))
         ;; Init
         (defn ~(method-symbol name "init")
           ([]
              (~(method-symbol name "init")
               (~(method-symbol name "defaults"))))
           ([properties#]
              [[] (BeanState. (str (UUID/randomUUID))
                              (ref properties#)
                              nil
                              (ref nil)
                              (ref [bind-changes]))])
           ([id# loader#]
              [[] (BeanState. id#
                              (ref nil)
                              loader#
                              (ref nil)
                              (ref [bind-changes]))]))))))

(defn- bean-specs
  "Returns a sequence of bean specs defined in the specified
  namespace."
  [ns]
  (map var-get
       (filter (comp :bean-spec meta)
               (map val (ns-interns ns)))))

(defmacro generate-beans
  "Generate JavaBean classes for the specifications defined in the
  current namespace."
  []
  (let [specs (bean-specs *ns*)]
    (concat
     `(do)
     ;; Generate dummy placeholder classes to allow circular
     ;; dependencies between beans.
     (for [spec specs]
       (if (:mixin spec)
         `(gen-interface :name ~(:classname spec))
         `(gen-class :name ~(:classname spec)
                     :state "_state")))
     ;; Load the dummy classes to make gen-class see all interfaces as
     ;; empty and not include duplicate methods.
     (for [spec specs]
       `(Class/forName ~(:classname spec)))
     ;; Generate actual implementations.
     (for [spec specs]
       (bean-generator *ns* spec)))))
