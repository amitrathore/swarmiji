(ns org.runa.swarmiji.utils.logger)

(use '[org.runa.swarmiji.config.system-config :as config])
(use 'org.runa.swarmiji.utils.general-utils)
(import '(java.io FileWriter BufferedWriter File))
(import '(org.apache.commons.io FileUtils))

(defn spit [f content] 
  (let [file (File. f)]
    (if (not (.exists file))
      (FileUtils/touch file))
    (with-open [#^FileWriter fw (FileWriter. f true)]
      (with-open [#^BufferedWriter bw (BufferedWriter. fw)]
	(.write bw (str content "\n"))))))

(defn log-message [& message-tokens]
  (let [message (apply str (interleave message-tokens (repeat " ")))]
    (if (log-to-console?) 
      (println message))
    (spit config/logfile message)))