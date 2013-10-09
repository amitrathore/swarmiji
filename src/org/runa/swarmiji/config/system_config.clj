(ns org.runa.swarmiji.config.system-config)

(def ^{:dynamic true} *swarmiji-env* (or (.get (System/getenv) "SWARMIJI_ENV") "test"))
(def swarmiji-home (or (.get (System/getenv) "SWARMIJI_HOME") (str (System/getProperty "user.home") "/workspace/swarmiji")))

(def ^:dynamic *swarmiji-conf* {})

(defn set-config [config-map]
  (def ^:dynamic *swarmiji-conf* config-map))

;; (defn current-swarmiji-ns-for [env-var-name]
;;   (or (System/getenv env-var-name)
;;       (throw (Exception. (str env-var-name " is not set")))))

;; (defn queue-name-prefixed-for [stem env-var-name]
;;   (str stem (current-swarmiji-ns-for env-var-name) "_" *swarmiji-env* "_"))

(defn read-config []
  *swarmiji-conf*)

(defn environment-specific-config-from [configs]
  (if *swarmiji-env*
    (configs *swarmiji-env*)
    (throw (Exception. "SWARMIJI_ENV is not set"))))

(defn operation-config []
  (:operation-configs (read-config)))

(defn swarmiji-mysql-config []
  (:swarmiji-mysql-configs (read-config)))

(defn swarmiji-user []
  (:swarmiji-username (operation-config)))

(defn queue-host []
  (:host (operation-config)))

(defn queue-port []
  (:port (operation-config)))

(defn queue-username []
  (:q-username (operation-config)))

(defn queue-password []
  (:q-password (operation-config)))

(defn queue-sevak-q-name [realtime?]
  (if realtime?
    (str (:sevak-request-queue-prefix (operation-config)) "realtime_" (swarmiji-user))
    (str (:sevak-request-queue-prefix (operation-config)) "non_realtime_" (swarmiji-user))))

(defn queue-diagnostics-q-name []
  (str (:sevak-diagnostics-queue-prefix (operation-config)) (swarmiji-user)))

(defn sevak-fanout-exchange-name []
  (str (:sevak-fanout-exchange-prefix (operation-config)) (swarmiji-user)))

(defn swarmiji-distributed-mode? []
  (:distributed-mode (operation-config)))

(defn swarmiji-diagnostics-mode? []
  (:diagnostics-mode (operation-config)))

(defn log-to-console? []
  ((operation-config) :log-to-console))

(defn rabbitmq-prefetch-count []
  ((operation-config) :rabbit-prefetch-count))

(defn rabbitmq-max-pool-size []
  (or (:rabbit-max-pool-size (operation-config))
      10))

(defn rabbitmq-max-idle-size []
  (or (:rabbit-max-idle-size (operation-config))
      10))

(defn medusa-server-thread-count []
  (:medusa-server-thread-count (operation-config)))

(defn medusa-client-thread-count []
  (:medusa-client-thread-count (operation-config)))

(defn should-reload-namespaces? []
  (:reload-namespaces (operation-config)))

(defn config-for-rathore-utils [process-type-id]
  {:log-to-console (log-to-console?)
   :logs-dir ((operation-config) :logsdir)
   :log-filename-prefix (str process-type-id "_" *swarmiji-env*)})

(defn syslog-config []
  (:syslog-config (operation-config)))

(defn syslog-local-name []
  (:syslog-local-name (operation-config)))
