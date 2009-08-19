(ns org.runa.swarmiji.monitor.recorder
  (:use [org.runa.swarmiji.mpi.transport])
  (:use [org.runa.swarmiji.sevak.bindings])
  (:use [org.runa.swarmiji.config.system-config])
  (:use [org.rathore.amit.utils.config])
  (:use [org.rathore.amit.utils.logger])
  (:use [org.rathore.amit.utils.clojure])
  (:use [org.runa.swarmiji.monitor.control_message :as control-message])
  (:import (java.sql Date Time)))

(defn timestamp-for-sql [time-in-millis]
  (str (.toString (Date. time-in-millis)) " " (.toString (Time. time-in-millis))))

(defn persist-message [control-message-str]
  (let [control-message (read-clojure-str control-message-str)
	_ (log-message "control-message:" control-message)
	now (timestamp-for-sql (System/currentTimeMillis))
        with-timestamps (merge {:created_at now :updated_at now} control-message)]
  (control-message/insert with-timestamps)))

(defn start []
  (binding-for-swarmiji [*rathore-utils-config* (config-for-rathore-utils "recorder")]
    (log-message "Swarmiji: Starting Control-Message-Recorder...")
    (log-message "Listening on:" (queue-diagnostics-q-name))
    (start-handler-on-queue (queue-diagnostics-q-name) persist-message)))
