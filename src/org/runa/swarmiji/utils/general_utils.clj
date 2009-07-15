(ns org.runa.swarmiji.utils.general-utils)

(import '(java.util Random UUID))
(use '[clojure.contrib.duck-streams :only (spit)])
(import '(java.lang.management ManagementFactory))
(require '(org.danlarkin [json :as json]))

(defn random-uuid []
  (str (UUID/randomUUID)))

(defn process-pid []
  (let [m-name (.getName (ManagementFactory/getRuntimeMXBean))]
    (first (.split m-name "@"))))

(defn simulate-jsonified [hash-object]
  (json/decode-from-str (json/encode-to-str hash-object)))