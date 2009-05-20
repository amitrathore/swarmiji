(ns org.runa.swarmiji.config.system-config)

(use 'org.runa.swarmiji.utils.general-utils)

(def *swarmiji-env* (or (.get (System/getenv) "SWARMIJI_ENV") "test"))
(def swarmiji-home (or (.get (System/getenv) "SWARMIJI_HOME") (str (System/getProperty "user.home") "/workspace/swarmiji")))

(def operation-configs {
    "test" {
      :swarmiji-username "amit"
      :host "tank.cinchcorp.com"
      :port 61613
      :q-username "guest"
      :q-password "guest"
      :sevak-request-queue (str "RUNA_SWARMIJI_TRANSPORT_" *swarmiji-env* "_")
      :sevak-diagnostics-queue (str "RUNA_SWARMIJI_DIAGNOSTICS_" *swarmiji-env* "_")    
      :distributed-mode false
      :diagnostics-mode true
      :logsdir (str swarmiji-home "/logs")
      :log-to-console true
    }    
    "development" {
      :swarmiji-username "amit"
      :host "tank.cinchcorp.com"
      :port 61613
      :q-username "guest"
      :q-password "guest"
      :sevak-request-queue (str "RUNA_SWARMIJI_TRANSPORT_" *swarmiji-env* "_")
      :sevak-diagnostics-queue (str "RUNA_SWARMIJI_DIAGNOSTICS_" *swarmiji-env* "_")    
      :distributed-mode true
      :diagnostics-mode true
      :logsdir (str swarmiji-home "/logs")
      :log-to-console true
    }
    "production" {
      :swarmiji-username "amit"
      :host "tank.cinchcorp.com"
      :port 61613
      :q-username "guest"
      :q-password "guest"
      :sevak-request-queue (str "RUNA_SWARMIJI_TRANSPORT_" *swarmiji-env* "_")
      :sevak-diagnostics-queue (str "RUNA_SWARMIJI_DIAGNOSTICS_" *swarmiji-env* "_")    
      :distributed-mode true
      :diagnostics-mode true
      :logsdir (str swarmiji-home "/logs")
      :log-to-console false
    }
  }
)

(defn operation-config []
  (if *swarmiji-env*
    (operation-configs *swarmiji-env*)
    (throw (Exception. "SWARMIJI_ENV is not set"))))

(def logfile (str ((operation-config) :logsdir) "/" *swarmiji-env* "_" (random-number-string) ".log"))

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