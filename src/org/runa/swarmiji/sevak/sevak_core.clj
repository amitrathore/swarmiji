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
(def sevak-bindings (ref {}))

(defmacro sevak-runner [sevak-name sevak-args]
  `(fn ~sevak-args 
     (if (swarmiji-distributed-mode?)
       (apply on-swarm (cons ~sevak-name ~sevak-args))
       (apply on-local (cons (@sevaks ~sevak-name) ~sevak-args)))))

(defmacro defsevak [service-name args expr]
  `(let [sevak-name# (keyword (str '~service-name))]
     (dosync (ref-set sevaks (assoc @sevaks sevak-name# (fn ~args ~expr))))
     (def ~service-name (sevak-runner sevak-name# ~args))))

(defn handle-sevak-request [service-handler service-args]
  (try
   (push-thread-bindings @sevak-bindings)
   {:response (apply service-handler service-args) :status :success}
   (catch Exception e 
     (log-exception e)
     {:exception (exception-name e) :stacktrace (stacktrace e) :status :error})
   (finally
    (pop-thread-bindings))))

(defn sevak-request-handling-listener []
  (proxy [Listener] []
    (message [headerMap messageBody]
      (let [req-json (json/decode-from-str messageBody)
	    _ (log-message "got request" req-json)
	    service-name (req-json :sevak-service-name) service-args (req-json :sevak-service-args) return-q (req-json :return-queue-name)
	    service-handler (@sevaks (keyword service-name))
	    response-envelope (handle-sevak-request service-handler service-args)]
	(send-on-transport return-q response-envelope)))))

(defn start-sevak-listener []
  (let [client (new-queue-client)
	sevak-request-handler (sevak-request-handling-listener)]
    (.subscribe client (queue-sevak-q-name) sevak-request-handler)))

(defmacro register-bindings [bindings]
  `(dosync (ref-set sevak-bindings (hash-map ~@(var-ize bindings)))))

(defmacro binding-for-swarmiji [bindings expr]
  `(do
     (register-bindings ~bindings)
     (binding [~@bindings]
       ~expr)))

(defn boot-sevak-server []
  (log-message "Starting sevaks in" *swarmiji-env* "mode")
  (log-message "RabbitMQ config" (operation-config))
  (log-message "Sevaks are offering the following" (count @sevaks) "services:" (keys @sevaks))
  (start-sevak-listener))

  