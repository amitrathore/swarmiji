(ns org.runa.swarmiji.config.queue-config)

(def *swarmiji-env* (.get (System/getenv) "SWARMIJI_ENV"))

(def operation-configs {
    "test" {
      :host "tank.cinchcorp.com"
      :port 61613
      :username "guest"
      :password "guest"
      :sevak-request-queue "RUNA_SWARMIJI_TRANSPORT_TEST"
      :distributed-mode false
    }    
    "development" {
      :host "tank.cinchcorp.com"
      ;:host "rohanda.local"
      :port 61613
      :username "guest"
      :password "guest"
      :sevak-request-queue "RUNA_SWARMIJI_TRANSPORT_DEVELOPMENT"
      :distributed-mode true
    }
    "production" {
      :host "tank.cinchcorp.com"
      :port 61613
      :username "guest"
      :password "guest"
      :sevak-request-queue "RUNA_SWARMIJI_TRANSPORT_PRODUCTION"
      :distributed-mode true
    }
  }
)

(defn operation-config []
  (if *swarmiji-env*
    (operation-configs *swarmiji-env*)
    (throw (Exception. "SWARMIJI_ENV is not set"))))

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