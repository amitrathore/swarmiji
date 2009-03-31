(ns org.runa.swarmiji.utils.exception-utils)

(defn log-exception [e]
  (println (.getMessage e))
  (.printStackTrace e))

(defn exception-name [e]
  (.getName (.getClass e)))

(defn stacktrace [e]
  (apply str 
	 (cons (str (exception-name e) "\n") 
	       (map #(str (.toString %) "\n") (.getStackTrace e)))))
