(ns org.runa.swarmiji.mpi.sevak-proxy
  (:import (com.rabbitmq.client Channel Connection ConnectionFactory QueueingConsumer)))

(use 'org.runa.swarmiji.mpi.transport)
(use 'org.runa.swarmiji.config.system-config)
(use 'org.runa.swarmiji.utils.general-utils)
(use 'org.runa.swarmiji.sevak.bindings)
(use 'org.rathore.amit.utils.logger)
(use 'org.rathore.amit.utils.clojure)
(use 'org.rathore.amit.utils.config)
(use 'org.rathore.amit.utils.rabbitmq)
(use 'org.rathore.amit.utils.rabbitmq-multiplex)

(def STOMP-HEADER (doto (new java.util.HashMap) 
		    (.put "auto-delete" true)))

(defn return-queue-name []
  (random-uuid))

(defn sevak-queue-message [sevak-service args]
  {:return-queue-name (return-queue-name)
   :sevak-service-name sevak-service
   :sevak-service-args args})

(defn register-callback [return-q-name custom-handler]
  (let [chan (*rabbitmq-multiplexer* :new-channel)
	wait-for-message (fn [_]
			    (with-swarmiji-bindings
			      (try
			       (with-open [channel chan]
					;q-declare args: queue-name, passive, durable, exclusive, autoDelete other-args-map
				 (.queueDeclare channel return-q-name); true false false true (new java.util.HashMap))
				 (let [consumer (QueueingConsumer. channel)]
				   (.basicConsume channel return-q-name false consumer)
				   (let [delivery (.nextDelivery consumer)
					 message (read-clojure-str (String. (.getBody delivery)))]
				     (custom-handler message)
				     (.queueDelete channel return-q-name)
				     (log-message "closed channel" return-q-name)))))
			      (catch InterruptedException ie
				(log-message "Encountered forced sevak-termination!")
				(log-exception ie))))]
	;thread (Thread. wait-for-message)]
    (log-message "got new channel:" (.hashCode chan) "now calling wait-for-message")
    (send-off (agent :_ignore_) wait-for-message)
    ;(.start thread)
    (log-message "called wait-for-message")
    ;{:channel chan :queue return-q-name :thread thread}))
    {:channel chan :queue return-q-name}))

(defn new-proxy [sevak-service args callback-function]
  (let [request-object (sevak-queue-message sevak-service args)
	return-q-name (request-object :return-queue-name)
	proxy-object (register-callback return-q-name callback-function)]
    (log-message "registered callback")
    (send-message-on-queue (queue-sevak-q-name) request-object)
    (log-message "proxy-object:" proxy-object)
    proxy-object))
		       