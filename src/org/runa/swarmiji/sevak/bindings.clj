(ns org.runa.swarmiji.sevak.bindings
  (:require [org.rathore.amit.utils.clojure :refer :all]))


(declare swarmiji-bindings)

(def swarmiji-bindings {})

(defmacro with-swarmiji-bindings [& body]
  `(do
     (push-thread-bindings swarmiji-bindings)
     (try
       ~@body
       (finally
         (pop-thread-bindings)))))

(defmacro register-bindings [bindings]
  `(alter-var-root #'swarmiji-bindings (constantly (hash-map ~@(var-ize bindings)))))

(defmacro binding-for-swarmiji [bindings & body]
  `(do
     (register-bindings ~bindings)
     (binding [~@bindings]
       ~@body)))

