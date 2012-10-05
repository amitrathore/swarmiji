(ns org.runa.swarmiji.sevak.bindings)

(use 'org.rathore.amit.utils.clojure)

(def swarmiji-bindings (ref {}))

(defmacro with-swarmiji-bindings [& exprs]
  `(do
     (try
       ~@exprs
       )))

(defmacro register-bindings [bindings]
  `(dosync (ref-set swarmiji-bindings (hash-map ~@(var-ize bindings)))))

(defmacro binding-for-swarmiji [bindings & expr]
  `(do
     (register-bindings ~bindings)
     (binding [~@bindings]
       ~@expr)))

