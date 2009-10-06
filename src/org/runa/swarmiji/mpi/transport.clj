(ns org.runa.swarmiji.mpi.transport)
(use 'org.runa.swarmiji.config.system-config)
(use 'org.rathore.amit.utils.logger)
(use 'org.rathore.amit.utils.rabbitmq)
(use 'org.rathore.amit.utils.rabbitmq-multiplex)

(defn send-message-on-queue [q-name q-message-object]
  ;(send-on-transport-amqp (queue-host) (queue-username) (queue-password) q-name (str q-message-object)))
  (send-on-q (queue-host) (queue-username) (queue-password) q-name (str q-message-object)))

(defn start-handler-on-queue [q-name handler-function]
  (start-queue-message-handler-for-function-amqp (queue-host) (queue-username) (queue-password) q-name handler-function))