(ns org.runa.swarmiji.config.system-config)

(use 'org.runa.swarmiji.utils.general-utils)

(def *swarmiji-env* (or (.get (System/getenv) "SWARMIJI_ENV") "test"))

(def operation-configs {
    "test" {
      :host "tank.cinchcorp.com"
      :port 61613
      :username "guest"
      :password "guest"
      :sevak-request-queue "RUNA_SWARMIJI_TRANSPORT_TEST"
      :distributed-mode false
      :logsdir "/Users/amit/workspace/furtive/logs"
      :log-to-console true
    }    
    "development" {
      ;:host "tank.cinchcorp.com"
      :host "rohanda.local"
      :port 61613
      :username "guest"
      :password "guest"
      :sevak-request-queue "RUNA_SWARMIJI_TRANSPORT_DEVELOPMENT"
      :distributed-mode true
      :logsdir "/Users/amit/workspace/furtive/logs"
      :log-to-console true
    }
    "production" {
      :host "tank.cinchcorp.com"
      :port 61613
      :username "guest"
      :password "guest"
      :sevak-request-queue "RUNA_SWARMIJI_TRANSPORT_PRODUCTION"
      :distributed-mode true
      :logsdir "/Users/amit/workspace/furtive/logs"
      :log-to-console false
    }
  }
)

(defn operation-config []
  (if *swarmiji-env*
    (operation-configs *swarmiji-env*)
    (throw (Exception. "SWARMIJI_ENV is not set"))))


(def logfile (str ((operation-config) :logsdir) "/" *swarmiji-env* "_" (random-number-string) ".log"))

(defn queue-host []
  ((operation-config) :host))

(defn queue-port []
  ((operation-config) :port))

(defn queue-username []
  ((operation-config) :username))

(defn queue-password []
  ((operation-config) :password))

(defn queue-sevak-q-name []
  ((operation-config) :sevak-request-queue))

(defn swarmiji-distributed-mode? []
  ((operation-config) :distributed-mode))

(defn log-to-console? []
  ((operation-config) :log-to-console))