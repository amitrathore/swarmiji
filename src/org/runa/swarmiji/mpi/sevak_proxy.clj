(ns org.runa.swarmiji.mpi.sevak-proxy
  (:import (com.rabbitmq.client Channel Connection ConnectionFactory QueueingConsumer))
  (:use org.runa.swarmiji.mpi.transport)
  (:use org.runa.swarmiji.config.system-config)
  (:use org.runa.swarmiji.utils.general-utils)
  (:use org.runa.swarmiji.sevak.bindings)
  (:use org.rathore.amit.utils.logger)
  (:use org.rathore.amit.utils.clojure)
  (:use org.rathore.amit.utils.config)
  (:use org.rathore.amit.utils.rabbitmq)
  (:use org.rathore.amit.medusa.core)
  (:use alex-and-georges.debug-repl))


(defn register-callback [realtime? return-q-name custom-handler request-object]
  (register-callback-or-fallback realtime? return-q-name custom-handler request-object))

(defn new-proxy 
  ([realtime? sevak-service args callback-function]
     (let [request-object (sevak-queue-message-for-return sevak-service args)
	   return-q-name (request-object :return-queue-name)
	   proxy-object (register-callback realtime? return-q-name callback-function request-object)]
       (log-message "Sending request for" sevak-service "with return-q:" return-q-name)
       proxy-object))
  ([realtime? sevak-service args]
     (let [request-object (sevak-queue-message-no-return sevak-service args)]
       (send-message-on-queue (queue-sevak-q-name realtime?) request-object)
       nil)))
