(ns org.runa.swarmiji.utils.general-utils)

(import '(java.util Random UUID))
(use '[clojure.contrib.duck-streams :only (spit)])
(import '(java.lang.management ManagementFactory))
(require '(org.danlarkin [json :as json]))

(defn random-uuid []
  (str (UUID/randomUUID)))

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

(defn simulate-jsonified [hash-object]
  (json/decode-from-str (json/encode-to-str hash-object)))