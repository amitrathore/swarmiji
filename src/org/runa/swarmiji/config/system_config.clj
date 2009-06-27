(ns org.runa.swarmiji.config.system-config)

(use 'org.runa.swarmiji.utils.general-utils)

(def *swarmiji-env* (or (.get (System/getenv) "SWARMIJI_ENV") "test"))
(def swarmiji-home (or (.get (System/getenv) "SWARMIJI_HOME") (str (System/getProperty "user.home") "/workspace/swarmiji")))

(load-file (str swarmiji-home "/config/config.clj"))

(defn environment-specific-config-from [configs]
  (if *swarmiji-env*
    (configs *swarmiji-env*)
    (throw (Exception. "SWARMIJI_ENV is not set"))))

(defn operation-config []
  (environment-specific-config-from operation-configs))

(defn swarmiji-mysql-config []
  (environment-specific-config-from swarmiji-mysql-configs))

(def logfile (str ((operation-config) :logsdir) "/" *swarmiji-env* "_" (process-pid) ".log"))

(defn swarmiji-user []
  ((operation-config) :swarmiji-username))

(defn queue-host []
  ((operation-config) :host))

(defn queue-port []
  ((operation-config) :port))

(defn queue-username []
  ((operation-config) :q-username))

(defn queue-password []
  ((operation-config) :q-password))

(defn queue-sevak-q-name []
  (str ((operation-config) :sevak-request-queue) (swarmiji-user)))

(defn queue-diagnostics-q-name []
  (str ((operation-config) :sevak-diagnostics-queue) (swarmiji-user)))

(defn swarmiji-distributed-mode? []
  ((operation-config) :distributed-mode))

(defn swarmiji-diagnostics-mode? []
  ((operation-config) :diagnostics-mode))

(defn log-to-console? []
  ((operation-config) :log-to-console))