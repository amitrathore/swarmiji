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

(defn init-rabbit []
  (log-message "Swarmiji: RabbitMQ host is" (queue-host))
  (init-rabbitmq-connection (queue-host) (queue-username) (queue-password)))