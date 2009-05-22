(ns org.runa.swarmiji.sevak.sevak-core)

(use 'org.runa.swarmiji.mpi.transport)
(use 'org.runa.swarmiji.utils.exception-utils)
(import '(net.ser1.stomp Client Listener))
(require '(org.danlarkin [json :as json]))
(use 'org.runa.swarmiji.client.client-core)
(use 'org.runa.swarmiji.config.system-config)
(use 'org.runa.swarmiji.utils.general-utils)
(use 'org.runa.swarmiji.utils.logger)

(def sevaks (ref {}))
(def swarmiji-bindings (ref {}))
(def START-UP-REPORT "START_UP_REPORT")
(def SEVAK-SERVER "SEVAK_SERVER")

(defmacro sevak-runner [sevak-name sevak-args]
  `(fn ~sevak-args 
     (if (swarmiji-distributed-mode?)
       (apply on-swarm (cons ~sevak-name ~sevak-args))
       (apply on-local (cons (@sevaks ~sevak-name) ~sevak-args)))))

(defmacro defsevak [service-name args expr]
  `(let [sevak-name# (keyword (str '~service-name))]
     (dosync (ref-set sevaks (assoc @sevaks sevak-name# (fn ~args ~expr))))
     (def ~service-name (sevak-runner sevak-name# ~args))))

(defmacro with-swarmiji-bindings [body]
  `(do
     (push-thread-bindings @swarmiji-bindings)
     (try
      ~body
      (finally
       (pop-thread-bindings)))))

(defn handle-sevak-request [service-handler service-args]
  (try
   (let [response-with-time (run-and-measure-timing 
			     (with-swarmiji-bindings
			      (apply service-handler service-args)))
	 value (response-with-time :response)
	 time-elapsed (response-with-time :time-taken)]
     {:response value :status :success :sevak-time time-elapsed})
     (catch Exception e 
       (log-exception e)
       {:exception (exception-name e) :stacktrace (stacktrace e) :status :error})))

(defn async-sevak-handler [service-handler sevak-name service-args return-q]
  (let [response (merge 
		  {:return-q-name return-q :sevak-name sevak-name :sevak-server-pid (process-pid)}
		  (handle-sevak-request service-handler service-args))]
    (send-on-transport return-q response)))

(defn sevak-request-handling-listener []
  (proxy [Listener] []
    (message [headerMap messageBody]
      (try
        (let [req-json (json/decode-from-str messageBody)
	      _ (log-message "got request" req-json)
	      service-name (req-json :sevak-service-name) service-args (req-json :sevak-service-args) return-q (req-json :return-queue-name)
	      service-handler (@sevaks (keyword service-name))
	      sevak-agent (agent service-handler)]
	  (send sevak-agent async-sevak-handler service-name service-args return-q))
	(catch Exception e
	  (log-exception e))))))

(defn start-sevak-listener []
  (let [client (new-queue-client)
	sevak-request-handler (sevak-request-handling-listener)]
    (.subscribe client (queue-sevak-q-name) sevak-request-handler)))

(defmacro register-bindings [bindings]
  `(dosync (ref-set swarmiji-bindings (hash-map ~@(var-ize bindings)))))

(defmacro binding-for-swarmiji [bindings & expr]
  `(do
     (register-bindings ~bindings)
     (binding [~@bindings]
       ~@expr)))

(defn boot-sevak-server []
  (log-message "Starting sevaks in" *swarmiji-env* "mode")
  (log-message "System config:" (operation-config))
  (log-message "MPI transport Q:" (queue-sevak-q-name))
  (log-message "MPI diagnostics Q:" (queue-diagnostics-q-name))
  (log-message "Sevaks are offering the following" (count @sevaks) "services:" (keys @sevaks))
  (start-sevak-listener)
  (send-on-transport (queue-diagnostics-q-name) {:message_type START-UP-REPORT :sevak_server_pid (process-pid) :sevak_name SEVAK-SERVER}))

  