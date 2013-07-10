(ns org.runa.swarmiji.sevak.bindings)

(use 'org.rathore.amit.utils.clojure)

(def swarmiji-bindings (ref {}))

(defmacro with-swarmiji-bindings [& body]
  `(do
     (push-thread-bindings @swarmiji-bindings)
     (try
       ~@body
       (finally
         (pop-thread-bindings)))))

(defmacro register-bindings [bindings]
  `(dosync (ref-set swarmiji-bindings (hash-map ~@(var-ize bindings)))))

(defmacro binding-for-swarmiji [bindings & body]
  `(do
     (register-bindings ~bindings)
     (binding [~@bindings]
       ~@body)))

