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

(def START-UP-REPORT "START_UP_REPORT")
(def SEVAK-SERVER "SEVAK_SERVER")

(def non-real-time-thread-pool (java.util.concurrent.Executors/newFixedThreadPool 32))

(defn register-sevak [sevak-name function-info]
  (dosync 
   (alter sevaks assoc sevak-name function-info)))

;; implement the actual fn and client fn as mulit-methods
(defmacro create-function-and-sevak [service-name realtime? needs-response? args expr]
  (let [defining-ns *ns*]
    `(do
       (defn ~service-name
         ([~@args]
            ;; this is the function that the client executes
            (if-not (swarmiji-distributed-mode?)
              (apply on-local (@sevaks (str (ns-name ~defining-ns) "/" '~service-name)) [~@args :sevak])
              (if ~needs-response?
                (apply on-swarm ~realtime? (str (ns-name ~defining-ns) "/" '~service-name)  [~@args])
                (apply on-swarm-no-response ~realtime? (str (ns-name ~defining-ns) "/" '~service-name)  [~@args]))))
         ([~@args ~'sevak] ;; this is the function that the sevak executes. Executed as (function args :sevak)
            (when (= :sevak ~'sevak)
              (do (println :actual) ~@expr))))
       (println "Defining service-name: " '~service-name)
       (register-sevak (ns-qualified-name (keyword (:name (meta (resolve '~service-name)))) ~defining-ns) (sevak-info (keyword (:name (meta (resolve '~service-name)))) ~realtime? ~needs-response? ~service-name)))))

(defmacro defsevak [service-name args & expr]
  `(create-function-and-sevak ~service-name true true ~args ~expr))

(defmacro defseva [service-name args & expr]
  `(create-function-and-sevak ~service-name true false ~args ~expr))

(defmacro defsevak-nr [service-name args & expr]
  `(create-function-and-sevak ~service-name false true ~args ~expr))

(defmacro defseva-nr [service-name args & expr]
  `(create-function-and-sevak ~service-name false false ~args ~expr))

(defn execute-sevak [service-name service-handler service-args]
  (try
    (println "execute-sevak: [service-name service-handler service-args]" [service-name service-handler service-args])
    (let [response-with-time (run-and-measure-timing 
                              (apply (:fn service-handler) (concat service-args '(:sevak))))
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
      (let [response (merge 
                      {:return-q-name return-q :sevak-name sevak-name :sevak-server-pid (process-pid)}
                      (execute-sevak sevak-name service-handler service-args))]
        (when (and return-q (:return service-handler))
          (println "handle-sevak-request: Returning request for" sevak-name "with return-q:" return-q "elapsed time:"
                       (:sevak-time response))
          (println "handle-sevak-request: " response)
          (send-message-no-declare return-q response)))
      (finally
       (ack-fn)))))

(defn sevak-request-handling-listener [req-str ack-fn real-time?]
  (with-swarmiji-bindings
    (try
      (println "sevak-request-handling-listener")
      (let [req (read-string req-str)
            service-name (req :sevak-service-name) 
            service-args (req :sevak-service-args) 
            return-q (req :return-queue-name)
            service-handler (@sevaks service-name)]

        (when (nil? service-handler)
          (ack-fn)
          (throw (Exception. (str "No handler found for: " service-name))))
        (if real-time?
	  (do 
            (medusa-future-thunk return-q
                                 #(handle-sevak-request service-handler service-name service-args return-q ack-fn)))
	  (.submit non-real-time-thread-pool
                   #(handle-sevak-request service-handler service-name service-args return-q ack-fn))))
      (catch Exception e
        (log-message "SRHL: Error in sevak-request-handling-listener:" (class e))
        (log-exception e)))))


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

(defn start-processor [routing-key real-time? start-log-message]
  (future
   (with-swarmiji-bindings 
     (try
       (with-prefetch-count (rabbitmq-prefetch-count)
         (start-queue-message-handler routing-key routing-key (fn [req-str ack-fn] (sevak-request-handling-listener req-str ack-fn real-time?))))
       (log-message "start-processor: Done with sevak requests!")
       (catch Exception e
         (log-message "start-processor: Error in sevak-servicing future!")
         (log-exception e))))))


(defn boot-sevak-server []
  (log-message "Starting sevaks in" *swarmiji-env* "mode")
  (log-message "System config:" (operation-config))
  (log-message "Medusa client threads:" (medusa-client-thread-count))
  (log-message "RabbitMQ prefetch-count:" (rabbitmq-prefetch-count))
  (log-message "Sevaks are offering the following" (count @sevaks) "services:" (keys @sevaks))
  (init-rabbit)
  (init-medusa (medusa-server-thread-count))
  (start-processor (queue-sevak-q-name true) true "Starting to serve realtime sevak requests..." )
  (start-processor (queue-sevak-q-name false) false "Starting to serve non-realtime sevak requests..." )
  (log-message "Sevak Server Started!"))
