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
      :host "arryn.local"
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
    "staging" {
      :swarmiji-username "furtive"
      :host "staging.cinchcorp.com"
      :port 61613
      :q-username "guest"
      :q-password "guest"
      :sevak-request-queue (str "RUNA_SWARMIJI_TRANSPORT_" *swarmiji-env* "_")
      :sevak-diagnostics-queue (str "RUNA_SWARMIJI_DIAGNOSTICS_" *swarmiji-env* "_")    
      :distributed-mode true
      :diagnostics-mode true
      :logsdir (str swarmiji-home "/mnt/pkgs/swarmiji/logs")
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

(def swarmiji-mysql-configs {
      "development" {
	     :classname "com.mysql.jdbc.Driver" 
	     :subprotocol "mysql" 
	     :user "root" 
	     :password "password" 
	     :subname (str "//localhost/swarmiji_development") 
       }
      "test" {
	     :classname "com.mysql.jdbc.Driver" 
	     :subprotocol "mysql" 
             :user "root" 
	     :password "password" 
	     :subname (str "//localhost/swarmiji_development") 
      }
      "staging" {
	     :classname "com.mysql.jdbc.Driver" 
	     :subprotocol "mysql" 
             :user "root" 
	     :password "password" 
	     :subname (str "//localhost/swarmiji_development") 
      }
   }
)

(defn environment-specific-config-from [configs]
  (if *swarmiji-env*
    (configs *swarmiji-env*)
    (throw (Exception. "SWARMIJI_ENV is not set"))))

(defn operation-config []
  (environment-specific-config-from operation-configs))

(defn swarmiji-mysql-config []
  (environment-specific-config-from swarmiji-mysql-configs))

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