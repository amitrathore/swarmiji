(ns org.runa.swarmiji.mpi.sevak-proxy
  (:import (com.rabbitmq.client Channel Connection ConnectionFactory QueueingConsumer)))

(use 'org.runa.swarmiji.mpi.transport)
;(require '(org.danlarkin [json :as json]))
(use 'org.runa.swarmiji.config.system-config)
(use 'org.runa.swarmiji.utils.general-utils)
(use 'org.runa.swarmiji.sevak.bindings)
(use 'org.rathore.amit.utils.logger)
(use 'org.rathore.amit.utils.clojure)
(use 'org.rathore.amit.utils.config)
(use 'org.rathore.amit.utils.rabbitmq)

(def STOMP-HEADER (doto (new java.util.HashMap) 
		    (.put "auto-delete" true)))

(defn return-queue-name []
  (random-uuid))

(defn sevak-queue-message [sevak-service args]
  {:return-queue-name (return-queue-name)
   :sevak-service-name sevak-service
   :sevak-service-args args})

(defn register-callback [return-q-name custom-handler]
  (let [conn (new-connection-for (queue-host) (queue-username) (queue-password))
	chan (.createChannel conn)
	wait-for-message (fn [_]
			   (with-open [connection conn]
			     (with-open [channel chan]
			       ;q-declare args: queue-name, passive, durable, exclusive, autoDelete other-args-map
			       (.queueDeclare channel return-q-name); true false false true (new java.util.HashMap))
			       (let [consumer (QueueingConsumer. channel)]
				 (.basicConsume channel return-q-name false consumer)
				 (let [delivery (.nextDelivery consumer)
				       message (read-clojure-str (String. (.getBody delivery)))]
				   (custom-handler message)
				   (.queueDelete channel return-q-name))))))]
    (send-off (agent :_ignore_) wait-for-message)
    {:connection conn :channel chan :queue return-q-name}))

(defn new-proxy [sevak-service args callback-function]
  (let [request-json-object (sevak-queue-message sevak-service args)
	return-q-name (request-json-object :return-queue-name)]
    (send-message-on-queue (queue-sevak-q-name) request-json-object)
    (register-callback return-q-name callback-function)))
		       