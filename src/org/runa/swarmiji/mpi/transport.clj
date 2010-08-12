(ns org.runa.swarmiji.mpi.transport)
(use 'org.runa.swarmiji.config.system-config)
(use 'org.runa.swarmiji.sevak.bindings)
(use 'org.rathore.amit.utils.logger)
(use 'org.rathore.amit.utils.clojure)
(use 'org.rathore.amit.utils.rabbitmq)
(use 'org.rathore.amit.medusa.core)

(def rabbit-down-messages (atom {}))

(declare add-to-rabbit-down-queue start-retry-rabbit)

(defn send-message-no-declare [q-name q-message-object]
  (with-swarmiji-bindings
    (with-exception-logging 
      (send-message-if-queue q-name q-message-object))))

(defn send-message-on-queue [q-name q-message-object]
  (with-swarmiji-bindings
    (with-exception-logging 
      (send-message q-name q-message-object))))

(defn fanout-message-to-all [message-object]
  (send-message (sevak-fanout-exchange-name) FANOUT-EXCHANGE-TYPE "" message-object))

(defn init-rabbit []
  (log-message "Swarmiji: RabbitMQ host is" (queue-host))
  (init-rabbitmq-connection (queue-host) (queue-username) (queue-password))
  (start-retry-rabbit 10000))

(defn send-and-register-callback [return-q-name custom-handler request-object]
  (let [chan (create-channel)
        consumer (consumer-for chan DEFAULT-EXCHANGE-NAME DEFAULT-EXCHANGE-TYPE return-q-name return-q-name)
        on-response (fn [msg]
                      (custom-handler (read-string msg))
                      (.queueDelete chan return-q-name)
                      (.close chan))
        f (fn []
            (send-message-on-queue (queue-sevak-q-name) request-object)
            (on-response (delivery-from chan consumer)))]
    (log-message "[" (number-of-queued-tasks) "]: Dispatching request")
    (medusa-future-thunk return-q-name f)
    {:channel chan :queue return-q-name :consumer consumer}))

(defn register-callback-or-fallback [return-q-name custom-handler request-object]
  (try
   (send-and-register-callback return-q-name custom-handler request-object)
   (catch java.net.ConnectException ce
     (add-to-rabbit-down-queue return-q-name custom-handler request-object))))

(defn add-to-rabbit-down-queue [return-queue-name custom-handler request-object]
  (swap! rabbit-down-messages assoc (System/currentTimeMillis) [return-queue-name custom-handler request-object]))

(defn retry-message [timestamp [return-queue-name custom-handler request-object]]
   (with-swarmiji-bindings
     (try
      (send-and-register-callback return-queue-name custom-handler request-object)
      (swap! rabbit-down-messages dissoc timestamp)
      (catch java.net.ConnectException ce) ;;ignore, will try again later
      (catch Exception e 
        (log-exception e "Trouble in swarmiji auto-retry!")))))

(defn retry-periodically [sleep-millis]
  (Thread/sleep sleep-millis)
  (doseq [[timestamp payload] @rabbit-down-messages]
    (retry-message timestamp payload))
  (recur sleep-millis))

(defrunonce start-retry-rabbit [sleep-millis]
  (future
    (retry-periodically sleep-millis)))
