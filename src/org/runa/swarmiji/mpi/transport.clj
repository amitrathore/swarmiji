(ns org.runa.swarmiji.mpi.transport)
(use 'org.runa.swarmiji.config.system-config)
(use 'org.runa.swarmiji.sevak.bindings)
(use 'org.rathore.amit.utils.logger)
(use 'org.rathore.amit.utils.rabbitmq)

(defn send-message-on-queue [q-name q-message-object]
  (with-swarmiji-bindings
    (with-exception-logging 
      (send-message q-name q-message-object))))

(defn fanout-message-to-all [message-object]
  (send-message (sevak-fanout-exchange-name) FANOUT-EXCHANGE-TYPE "" message-object))

(defn start-handler-on-queue 
  ([q-name handler-function]
     (with-swarmiji-bindings
       (with-exception-logging 
         (start-queue-message-handler-for-function-amqp (queue-host) (queue-username) (queue-password) q-name handler-function))))
  ([exchange-name exchange-type q-name handler-function]
     (with-swarmiji-bindings
       (with-exception-logging 
         (start-queue-message-handler-for-function-amqp (queue-host) (queue-username) (queue-password) exchange-name exchange-type q-name handler-function)))))

(defn init-rabbit []
  (init-rabbitmq-connection (queue-host) (queue-username) (queue-password)))