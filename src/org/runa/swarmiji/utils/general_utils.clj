(ns org.runa.swarmiji.utils.general-utils)

(import '(java.util Random))
(use '[clojure.contrib.duck-streams :only (spit)])
(import '(java.lang.management ManagementFactory))

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

(defn process-pid []
  (let [m-name (.getName (ManagementFactory/getRuntimeMXBean))]
    (first (.split m-name "@"))))

(defmacro run-and-measure-timing [expr]
  `(let [start-time# (System/currentTimeMillis)
	 response# ~expr
	 end-time# (System/currentTimeMillis)]
     {:time-taken (- end-time# start-time#) :response response# :start-time start-time# :end-time end-time#}))
