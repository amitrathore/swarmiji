(ns org.runa.swarmiji.sevak.sevak-core)

(def sevaks (ref {}))

(defmacro defsevak [service-name args expr]
  `(let [sevak-name# (keyword (str '~service-name))]
     (dosync (ref-set sevaks (assoc @sevaks sevak-name# (fn ~args ~expr))))))

(defn boot []
  (println "Sevaks are offering the following" (count @sevaks) "services:" (keys @sevaks)))