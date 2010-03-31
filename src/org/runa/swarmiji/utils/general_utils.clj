(ns org.runa.swarmiji.utils.general-utils)

(import '(java.util Random UUID))
(use '[clojure.contrib.duck-streams :only (spit)])
(import '(java.lang.management ManagementFactory))
(use 'org.rathore.amit.utils.clojure)

(defn random-uuid []
  (str (UUID/randomUUID)))

(defn return-queue-name []
  (random-uuid))

(defn random-queue-name []
  (random-uuid))

(defn process-pid []
  (let [m-name (.getName (ManagementFactory/getRuntimeMXBean))]
    (first (.split m-name "@"))))

(defn simulate-serialized [hash-object]
  (read-string (str hash-object)))