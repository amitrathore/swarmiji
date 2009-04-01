(ns org.runa.swarmiji.config.queue-config)

(def *swarmiji-env* (.get (System/getenv) "SWARMIJI_ENV"))

(def rabbitmq-config {
    "development" {
      :host "tank.cinchcorp.com"
      :port 61613
      :username "guest"
      :password "guest"
      :sevak-request-queue "RUNA_SWARMIJI_TRANSPORT_DEVELOPMENT"
    }
    "production" {
      :host "tank.cinchcorp.com"
      :port 61613
      :username "guest"
      :password "guest"
      :sevak-request-queue "RUNA_SWARMIJI_TRANSPORT_PRODUCTION"
    }
  }
)

(defn queue-config []
  (rabbitmq-config *swarmiji-env*))

(defn queue-host []
  ((queue-config) :host))

(defn queue-port []
  ((queue-config) :port))

(defn queue-username []
  ((queue-config) :username))

(defn queue-password []
  ((queue-config) :password))

(defn queue-sevak-q-name []
  ((queue-config) :sevak-request-queue))