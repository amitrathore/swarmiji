(ns org.runa.swarmiji.mpi.sevak-proxy
  (:import (com.rabbitmq.client Channel Connection ConnectionFactory QueueingConsumer)))

(use 'org.runa.swarmiji.mpi.transport)
(require '(org.danlarkin [json :as json]))
(use 'org.runa.swarmiji.config.system-config)
(use 'org.runa.swarmiji.utils.general-utils)
(use 'org.runa.swarmiji.sevak.bindings)
(use 'org.rathore.amit.utils.logger)
(use 'org.rathore.amit.utils.config)

(def STOMP-HEADER (doto (new java.util.HashMap) 
		    (.put "auto-delete" true)))

(defn return-queue-name []
  (random-uuid))

(defn sevak-queue-message [sevak-service args]
  (let [return-q-name (return-queue-name)]
    {:return-queue-name return-q-name
     :sevak-service-name sevak-service
     :sevak-service-args args}))

(defn register-callback [return-q-name custom-handler]
  (with-connection connection
    (with-open [channel (.createChannel connection)]
      ;q-declare args: queue-name, passive, durable, exclusive, autoDelete other-args-map
      (.queueDeclare channel return-q-name); true false false true (new java.util.HashMap))
      (let [consumer (QueueingConsumer. channel)]
	(.basicConsume channel return-q-name false consumer)
	(let [delivery (.nextDelivery consumer)
	      message (json/decode-from-str (String. (.getBody delivery)))]
	  (custom-handler message)
	  (.queueDelete channel return-q-name))))))

(defn new-proxy [sevak-service args callback-function]
  (let [request-json-object (sevak-queue-message sevak-service args)
	return-q-name (request-json-object :return-queue-name)]
    (send-on-transport-amqp (queue-sevak-q-name) request-json-object)
    (register-callback return-q-name callback-function)))
		       