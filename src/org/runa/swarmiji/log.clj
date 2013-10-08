(ns org.runa.swarmiji.log
  (:require [kits.structured-logging :as log]
            [org.runa.swarmiji.config.system-config :as config]))

(defn info [log-map]
  #_(log/info (config/syslog-config) (config/syslog-local-name) log-map)
  (println "LOGGING INFO:::" log-map))

(defn error [log-map]
  #_(log/error (config/syslog-config) (config/syslog-local-name) log-map)
  (println "LOGGING ERROR:::" log-map))

(defn exception [exception]
  #_(log/exception (config/syslog-config) (config/syslog-local-name) exception)
  (println "LOGGING EXCEPTION:::" exception))
