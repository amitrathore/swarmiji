(def operation-configs {
    "test" {
      :swarmiji-username "koba_on_rohanda"
      :host "arryn.local"
      :port 61613
      :q-username "guest"
      :q-password "guest"
      :sevak-request-queue-prefix (queue-name-prefixed-by "RUNA_SWARMIJI_TRANSPORT_")
      :sevak-diagnostics-queue-prefix (queue-name-prefixed-by "RUNA_SWARMIJI_DIAGNOSTICS_")    
      :distributed-mode false
      :diagnostics-mode true
      :logsdir (str swarmiji-home "/logs")
      :log-to-console true
    }    
    "development" {
      :swarmiji-username "koba_on_rohanda"
      :host "arryn.local"
      :port 61613
      :q-username "guest"
      :q-password "guest"
      :sevak-request-queue-prefix (queue-name-prefixed-by "RUNA_SWARMIJI_TRANSPORT_")
      :sevak-diagnostics-queue-prefix (queue-name-prefixed-by "RUNA_SWARMIJI_DIAGNOSTICS_")    
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
      :sevak-request-queue-prefix (queue-name-prefixed-by "RUNA_SWARMIJI_TRANSPORT_")
      :sevak-diagnostics-queue-prefix (queue-name-prefixed-by "RUNA_SWARMIJI_DIAGNOSTICS_")    
      :distributed-mode true
      :diagnostics-mode true
      :logsdir (str swarmiji-home "/mnt/pkgs/swarmiji/logs")
      :log-to-console true
    }
    "production" {
      :swarmiji-username "furtive"
      :host "mq.production.runa.com"
      :port 61613
      :q-username "guest"
      :q-password "guest"
      :sevak-request-queue-prefix (queue-name-prefixed-by "RUNA_SWARMIJI_TRANSPORT_")
      :sevak-diagnostics-queue-prefix (queue-name-prefixed-by "RUNA_SWARMIJI_DIAGNOSTICS_")    
      :distributed-mode true
      :diagnostics-mode true
      :logsdir "/mnt/log/swarmiji/"
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
       :user "cinch" 
	     :password "ind6678" 
	     :subname (str "//staging.cinchcorp.com/swarmiji_staging") 
      }
      "production" {
	     :classname "com.mysql.jdbc.Driver" 
	     :subprotocol "mysql" 
       :user "cinch" 
	     :password "secret" 
	     :subname (str "//db-primary.production.runa.com/swarmiji_production") 
      }
   }
)

