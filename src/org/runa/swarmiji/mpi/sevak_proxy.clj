(ns org.runa.swarmiji.mpi.sevak-proxy
  (:import (com.rabbitmq.client Channel Connection ConnectionFactory QueueingConsumer)))

(use 'org.runa.swarmiji.mpi.transport)
(require '(org.danlarkin [json :as json]))
(import '(net.ser1.stomp Client Listener))
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

;(defn listener-proxy [q-client return-q-name custom-handler]
;  (proxy [Listener] []
;    (message [headerMap messageBody]
;      (with-swarmiji-bindings	     
;      (do
;	(custom-handler (json/decode-from-str messageBody))
;	(.unsubscribe q-client return-q-name)
;	(.disconnect q-client))))))

;(defn register-callback [return-q-name custom-handler]
;  (let [client (new-queue-client)
;	callback (listener-proxy client return-q-name custom-handler)]
;    (.subscribe client return-q-name callback)
;    client))

(defn register-callback [return-q-name custom-handler]
  (with-connection connection
    (with-open [channel (.createChannel connection)]
      (.queueDeclare channel return-q-name)
      (let [consumer (QueueingConsumer. channel)]
	(.basicConsume channel return-q-name false consumer)
	(let [delivery (.nextDelivery consumer)
	      message (json/decode-from-str (String. (.getBody delivery)))]
	  (custom-handler message))))))

(defn new-proxy [sevak-service args callback-function]
  (let [request-json-object (sevak-queue-message sevak-service args)
	return-q-name (request-json-object :return-queue-name)]
    (send-on-transport-amqp (queue-sevak-q-name) request-json-object)
    (register-callback return-q-name callback-function)))
		       