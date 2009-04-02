(ns org.runa.swarmiji.utils.general-utils)

(import '(java.util Random))
(use '[clojure.contrib.duck-streams :only (spit)])

(defn random-number-string []
  (str (Math/abs (.nextInt (Random. ) 10000000000))))
