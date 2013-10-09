(ns org.runa.swarmiji.log
  (:require [kits.structured-logging :as log]
            [org.runa.swarmiji.config.system-config :as config]))

(defn info [log-map]
  (log/info (config/syslog-config) (config/syslog-local-name) log-map))

(defn error [log-map]
  (log/error (config/syslog-config) (config/syslog-local-name) log-map))

(defn exception [exception]
  (log/exception (config/syslog-config) (config/syslog-local-name) exception))
