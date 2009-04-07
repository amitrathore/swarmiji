(ns org.runa.swarmiji.utils.general-utils)

(import '(java.util Random))
(use '[clojure.contrib.duck-streams :only (spit)])

(defn random-number-string []
  (str (Math/abs (.nextInt (Random. ) 10000000000))))

(defn var-ize [var-vals]
  (loop [ret [] vvs (seq var-vals)]
    (if vvs
      (recur  (conj (conj ret `(var ~(first vvs))) (second vvs))
	      (next (next vvs)))
      (seq ret))))

(defn push-thread-bindings [bindings-map]
  (clojure.lang.Var/pushThreadBindings bindings-map))

(defn pop-thread-bindings []
  (clojure.lang.Var/popThreadBindings))

(defmacro with-bindings [bindings-map body]
  `(do
     (. clojure.lang.Var (pushThreadBindings ~bindings-map)
     (try
      ~body
      (finally
       (. clojure.lang.Var (popThreadBindings)))))))
