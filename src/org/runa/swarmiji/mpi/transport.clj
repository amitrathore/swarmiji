(ns org.runa.swarmiji.mpi.transport
  (:import (com.rabbitmq.client ConnectionParameters ConnectionFactory MessageProperties QueueingConsumer)))

(require '(org.runa.swarmiji.mpi [rabbitmq :as rmq]))
(use 'org.runa.swarmiji.config.system-config)
(use 'org.runa.swarmiji.sevak.bindings)
(require '(org.danlarkin [json :as json]))
(import '(net.ser1.stomp Client Listener))
(use 'org.rathore.amit.utils.logger)

(defn new-queue-client []
  (Client. (queue-host) (queue-port), (queue-username) (queue-password)))

(defn send-on-transport [q-name q-message-object]
  (let [client (new-queue-client)
	q-message-string (json/encode-to-str q-message-object)]
    (.send client q-name q-message-string)
    (.disconnect client)))

;(defn queue-message-handler-for-function [the-function]
;  (proxy [Listener] []
;    (message [header-map message-body]
;      (with-swarmiji-bindings
;        (try
;         (the-function (json/decode-from-str message-body))
;	 (catch Exception e
;	   (log-exception e)))))))

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
	(.basicPublish "" q-name MessageProperties/MINIMAL_BASIC
		       (.getBytes (json/encode-to-str q-message-object)))))))

(defn start-queue-message-handler-for-function-amqp [q-name the-function]
  (with-connection connection
    (with-open [messages (rmq/queue-seq connection q-name)]
      (doseq [message messages]
	(the-function (json/decode-from-str message))))))

;(defn send-on-transport-amqp [q-name q-message-object]
;  (with-open [connection (let [params (doto (ConnectionParameters.)
;                                        (.setVirtualHost "/")
;                                        (.setUsername (queue-username))
;                                        (.setPassword (queue-password)))
;			       factory (ConnectionFactory. params)]
;                           (.newConnection factory (queue-host)))
;	      channel (.createChannel connection)]
;    (doto channel
;      (.queueDeclare q-name)
;     (.basicPublish "" q-name MessageProperties/MINIMAL_BASIC
;		     (.getBytes (json/encode-to-str q-message-object))))))

;(defn start-queue-message-handler-for-function-amqp [q-name the-function]
; (with-open [connection (let [params (doto (ConnectionParameters.)
;                                        (.setVirtualHost "/")
;                                        (.setUsername (queue-username))
;                                        (.setPassword (queue-password)))
;			       factory (ConnectionFactory. params)]
;			  (.newConnection factory (queue-host)))]
;  (let [messages (rmq/queue-seq connection q-name)]
;    (doseq [message messages]
;      (the-function (json/decode-from-str message))))))