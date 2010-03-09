(ns org.runa.swarmiji.sevak.sevak-core)

(use 'org.runa.swarmiji.mpi.transport)
(use 'org.runa.swarmiji.client.client-core)
(use 'org.runa.swarmiji.config.system-config)
(use 'org.runa.swarmiji.utils.general-utils)
(use 'org.runa.swarmiji.sevak.bindings)
(use 'org.rathore.amit.utils.config)
(use 'org.rathore.amit.utils.logger)
(use 'org.rathore.amit.utils.clojure)

(def sevaks (ref {}))

(def START-UP-REPORT "START_UP_REPORT")
(def SEVAK-SERVER "SEVAK_SERVER")

(defmacro sevak-runner [sevak-name needs-response sevak-args]
  `(fn ~sevak-args 
      (if (swarmiji-distributed-mode?)
	(if ~needs-response
	    (apply on-swarm (cons ~sevak-name ~sevak-args))
	    (apply on-swarm-no-response (cons ~sevak-name ~sevak-args)))
	(apply on-local (cons (@sevaks ~sevak-name) ~sevak-args)))))

(defmacro defsevak [service-name args & expr]
  `(let [sevak-name# (keyword (str '~service-name))]
     (dosync (ref-set sevaks (assoc @sevaks sevak-name# {:return Boolean/TRUE :fn (fn ~args (do ~@expr))})))
     (def ~service-name (sevak-runner sevak-name# Boolean/TRUE ~args))))

(defmacro defseva [service-name args & expr]
  `(let [seva-name# (keyword (str '~service-name))]
     (dosync (ref-set sevaks (assoc @sevaks seva-name# {:return Boolean/FALSE :fn (fn ~args (do ~@expr))})))
     (def ~service-name (sevak-runner seva-name# Boolean/FALSE ~args))))

(defn handle-sevak-request [service-handler service-args]
  (with-swarmiji-bindings
   (try
    (let [response-with-time (run-and-measure-timing 
			      (apply (:fn service-handler) service-args))
	  value (response-with-time :response)
	  time-elapsed (response-with-time :time-taken)]
      {:response value :status :success :sevak-time time-elapsed})
    (catch Exception e 
      (log-exception e)
      {:exception (exception-name e) :stacktrace (stacktrace e) :status :error}))))

(defn async-sevak-handler [service-handler sevak-name service-args return-q]
  (with-swarmiji-bindings
    (let [response (merge 
		    {:return-q-name return-q :sevak-name sevak-name :sevak-server-pid (process-pid)}
		    (handle-sevak-request service-handler service-args))]
      (if (:return service-handler)
	(send-message-on-queue return-q response)))))

(defn sevak-request-handling-listener [req-str]
  (with-swarmiji-bindings
   (try
    (let [req (read-clojure-str req-str)
	  service-name (req :sevak-service-name) service-args (req :sevak-service-args) return-q (req :return-queue-name)
	  service-handler (@sevaks (keyword service-name))
	  sevak-agent (agent service-handler)]
      (log-message (format "Received Sevak Request : %s with Args : %s " service-name  service-args ) )
      (if (nil? service-handler)
	(throw (Exception. (str "No handler found for: " service-name))))
      (send sevak-agent async-sevak-handler service-name service-args return-q))
    (catch Exception e
      (log-exception e)))))

(defn boot-sevak-server []
  (log-message "Starting sevaks in" *swarmiji-env* "mode")
  (log-message "System config:" (operation-config))
  (log-message "MPI transport Q:" (queue-sevak-q-name))
  (log-message "MPI diagnostics Q:" (queue-diagnostics-q-name))
  (log-message "Sevaks are offering the following" (count @sevaks) "services:" (keys @sevaks))
  (send-message-on-queue (queue-diagnostics-q-name) {:message_type START-UP-REPORT :sevak_server_pid (process-pid) :sevak_name SEVAK-SERVER})
  (future (with-swarmiji-bindings (start-handler-on-queue (sevak-fanout-exchange-name) "fanout" (random-queue-name) sevak-request-handling-listener)))
  (future (with-swarmiji-bindings (start-handler-on-queue (queue-sevak-q-name) sevak-request-handling-listener)))
  (log-message "Sevak Server Started!"))