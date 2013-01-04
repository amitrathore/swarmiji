(ns org.runa.swarmiji.mpi.sevak-proxy
  (:require [org.rathore.amit.utils.logger :refer [log-message]]
            [org.runa.swarmiji.config.system-config :refer [queue-sevak-q-name]]
            [org.runa.swarmiji.mpi.transport :refer [register-callback-or-fallback
                                                     send-message-on-queue]]
            [org.runa.swarmiji.utils.general-utils :refer [sevak-queue-message-for-return
                                                           sevak-queue-message-no-return]]))


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
