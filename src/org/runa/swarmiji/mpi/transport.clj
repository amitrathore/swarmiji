(ns org.runa.swarmiji.mpi.transport
  (:import (com.rabbitmq.client ConnectionParameters ConnectionFactory MessageProperties QueueingConsumer)))
(use 'org.runa.swarmiji.config.system-config)
(use 'org.runa.swarmiji.sevak.bindings)
(require '(org.danlarkin [json :as json]))
(use 'org.rathore.amit.utils.logger)

(defn delivery-seq [ch q]
  (lazy-seq
    (let [d (.nextDelivery q)
          m (String. (.getBody d))]
      (.basicAck ch (.. d getEnvelope getDeliveryTag) false)
      (cons m (delivery-seq ch q)))))

(defn queue-seq [conn queue-name]
  (let [ch (.createChannel conn)]
    (.queueDeclare ch queue-name)
    (let [consumer (QueueingConsumer. ch)]
      (.basicConsume ch queue-name consumer)
      (delivery-seq ch consumer))))

(defmacro with-connection [connection & exprs]
  `(with-open [~connection (let [params# (doto (ConnectionParameters.)
					 (.setVirtualHost "/")
					 (.setUsername (queue-username))
					 (.setPassword (queue-password)))
				factory# (ConnectionFactory. params#)]
			    (.newConnection factory# (queue-host)))]
     (do ~@exprs)))

(defn send-on-transport-amqp [q-name q-message-object]
  (with-connection connection 
    (with-open [channel (.createChannel connection)]
      (doto channel
	;q-declare args: queue-name, passive, durable, exclusive, autoDelete other-args-map
	(.queueDeclare q-name); true false false auto-delete-queue (new java.util.HashMap))
	(.basicPublish "" q-name MessageProperties/TEXT_PLAIN (.getBytes (json/encode-to-str q-message-object)))))))

(defn start-queue-message-handler-for-function-amqp [q-name the-function]
  (with-connection connection
    (with-open [messages (queue-seq connection q-name)]
      (doseq [message messages]
	(the-function (json/decode-from-str message))))))