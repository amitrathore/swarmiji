(ns org.runa.swarmiji.utils.exception-utils)

(use 'org.runa.swarmiji.utils.general-utils)
(use 'org.runa.swarmiji.utils.logger)

(defn log-exception [e]
  (log-message (.getMessage e))
  (.printStackTrace e))

(defn exception-name [e]
  (.getName (.getClass e)))

(defn stacktrace [e]
  (apply str 
	 (cons (str (exception-name e) "\n")
	       (cons (str (.getMessage e) "\n")
		     (map #(str (.toString %) "\n") (.getStackTrace e))))))
