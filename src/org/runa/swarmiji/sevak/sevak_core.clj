(ns org.runa.swarmiji.sevak.sevak-core
  (:use org.runa.swarmiji.mpi.transport)
  (:use org.rathore.amit.utils.rabbitmq)
  (:use org.runa.swarmiji.client.client-core)
  (:use org.runa.swarmiji.config.system-config)
  (:use org.runa.swarmiji.utils.general-utils)
  (:use org.runa.swarmiji.sevak.bindings)
  (:use org.rathore.amit.utils.config)
  (:use org.rathore.amit.utils.logger)
  (:use org.rathore.amit.utils.clojure)
  (:use alex-and-georges.debug-repl)
  (:use org.rathore.amit.medusa.core))

(def sevaks (ref {}))
(def namespaces-to-reload (atom []))

(def START-UP-REPORT "START_UP_REPORT")
(def SEVAK-SERVER "SEVAK_SERVER")

(def non-real-time-thread-pool (java.util.concurrent.Executors/newFixedThreadPool 32))

(defn register-sevak [sevak-name function-info]
  (println "registering sevak:" sevak-name)
  (dosync 
   (alter sevaks assoc sevak-name function-info)))

(defmacro sevak-runner [realtime? sevak-name needs-response]
  (let [defining-ns *ns*]
    `(fn [& ~'sevak-args]
       (println "in sevak-runner")
       (if-not (swarmiji-distributed-mode?)
         (apply on-local (@sevaks (ns-qualified-name ~sevak-name ~defining-ns)) ~'sevak-args)
         (if ~needs-response
           (apply on-swarm ~realtime? (ns-qualified-name ~sevak-name ~defining-ns)  ~'sevak-args)
           (apply on-swarm-no-response ~realtime? (ns-qualified-name ~sevak-name ~defining-ns) ~'sevak-args))))))

(defmacro create-sevak-from-function 
  ([function realtime? needs-response?]
     (let [{:keys [ns name]} (meta (resolve function))
           sevak-name-keyword (keyword name)]
       (println "creating sevak-from-function")
       `(do
          (register-sevak (ns-qualified-name ~sevak-name-keyword *ns*) (sevak-info ~sevak-name-keyword ~realtime? ~needs-response? ~function))
          (def ~name (sevak-runner ~realtime? ~sevak-name-keyword ~needs-response?)))))
  ([function]
     `(create-sevak-from-function ~function true true)))

(defmacro create-function-and-sevak [service-name realtime? needs-response? args expr]
  (println "create-function-and-sevak")
  `(do 
     (def ~service-name (fn ~args (do ~@expr)))
     (create-sevak-from-function ~service-name ~realtime? ~needs-response?)))

(defmacro defsevak [service-name args & expr]
  (println "defsevak ...")
  `(create-function-and-sevak ~service-name true true ~args ~expr))

(defmacro defseva [service-name args & expr]
  `(create-function-and-sevak ~service-name true false ~args ~expr))

(defmacro defsevak-nr [service-name args & expr]
  `(create-function-and-sevak ~service-name false true ~args ~expr))

(defmacro defseva-nr [service-name args & expr]
  `(create-function-and-sevak ~service-name false false ~args ~expr))

(defn always-reload-namespaces [& namespaces]
  (log-message "realoading namespaces")
  (reset! namespaces-to-reload namespaces))

(defn reload-namespaces []
  (when (should-reload-namespaces?)
    (log-message "RELOADING:" @namespaces-to-reload)
    (doseq [n @namespaces-to-reload]
      (require n :reload))))

(defn execute-sevak [service-name service-handler service-args]
  (try
    (println "execute-sevak")
    (println "[service-name service-handler service-args]" [service-name service-handler service-args])
    (let [response-with-time (run-and-measure-timing 
                              (apply (:fn service-handler) service-args))
          value (response-with-time :response)
          time-elapsed (response-with-time :time-taken)]
      {:response value :status :success :sevak-time time-elapsed})
    (catch InterruptedException ie
      (throw ie))
    (catch Exception e 
      (log-exception e (str "SEVAK ERROR! " (class e) " detected while running " service-name " with args: " service-args))
      {:exception (exception-name e) :stacktrace (stacktrace e) :status :error})))

(defn handle-sevak-request [service-handler sevak-name service-args return-q ack-fn]
  (with-swarmiji-bindings
    (try
      (println "handle-sevak-request")
      (let [response (merge 
                      {:return-q-name return-q :sevak-name sevak-name :sevak-server-pid (process-pid)}
                      (execute-sevak sevak-name service-handler service-args))]
        (when (and return-q (:return service-handler))
          (log-message "Returning request for" sevak-name "with return-q:" return-q "elapsed time:"
                       (:sevak-time response))
          (send-message-no-declare return-q response)))
      (finally
       (ack-fn)))))

(defn sevak-request-handling-listener [req-str ack-fn real-time?]
  (with-swarmiji-bindings
    (try
      (log-message "sevak-request-handling-listener")
      (reload-namespaces)
      (let [req (read-string req-str)
            service-name (req :sevak-service-name) 
            service-args (req :sevak-service-args) 
            return-q (req :return-queue-name)
            service-handler (@sevaks service-name)]
        (log-message "Received request for" service-name "with args:" service-args "and return-q:" return-q)
        (when (nil? service-handler)
          (ack-fn)
          (throw (Exception. (str "No handler found for: " service-name))))
        (if real-time?
	  (do (log-message "real-time?" real-time?)
              (medusa-future-thunk return-q
                                   #(handle-sevak-request service-handler service-name service-args return-q ack-fn)))
	  (.submit non-real-time-thread-pool
                   #(handle-sevak-request service-handler service-name service-args return-q ack-fn))))
      (catch Exception e
        (log-message "Error in sevak-request-handling-listener:" (class e))
        (log-exception e)))))

(defn start-processor [routing-key real-time? start-log-message]
  (future
   (with-swarmiji-bindings 
     (try
       (with-prefetch-count (rabbitmq-prefetch-count)
         (start-queue-message-handler routing-key routing-key (fn [req-str ack-fn] (sevak-request-handling-listener req-str ack-fn real-time?))))
       (log-message "Done with sevak requests!")
       (catch Exception e
         (log-message "Error in sevak-servicing future!")
         (log-exception e))))))

(defn start-broadcast-processor []
  (future 
   (with-swarmiji-bindings
     (let [broadcasts-q (random-queue-name "BROADCASTS_LISTENER_")]
       (try
         (log-message "Listening for update broadcasts...")
         (log-message (sevak-fanout-exchange-name))
         ;; (.addShutdownHook (Runtime/getRuntime) (Thread. #(with-swarmiji-bindings (delete-queue broadcasts-q))))
         (start-queue-message-handler (sevak-fanout-exchange-name) FANOUT-EXCHANGE-TYPE broadcasts-q (random-queue-name) #(sevak-request-handling-listener %1 %2 false))
         (log-message "Done with broadcasts!")    
         (catch Exception e         
           (log-message "Error in update broadcasts future!")
           (log-exception e)))))))

(defn boot-sevak-server []
  (log-message "Starting sevaks in" *swarmiji-env* "mode")
  (log-message "System config:" (operation-config))
  (log-message "Medusa client threads:" (medusa-client-thread-count))
  (log-message "RabbitMQ prefetch-count:" (rabbitmq-prefetch-count))
  (log-message "Sevaks are offering the following" (count @sevaks) "services:" (keys @sevaks))
  (log-message "Will always reload these namespaces:" @namespaces-to-reload)
  (init-rabbit)
  (init-medusa (medusa-server-thread-count))
  ;; (log-message "Medusa started with" (max-pool-size) "threads")
  (send-message-on-queue (queue-diagnostics-q-name) {:message_type START-UP-REPORT :sevak_server_pid (process-pid) :sevak_name SEVAK-SERVER})
  (start-broadcast-processor)
  (start-processor (queue-sevak-q-name true) true "Starting to serve realtime sevak requests..." )
  (start-processor (queue-sevak-q-name false) false "Starting to serve non-realtime sevak requests..." )
  (log-message "Sevak Server Started!"))
