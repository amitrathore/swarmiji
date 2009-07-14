(ns org.runa.swarmiji.mpi.transport)

(use 'org.runa.swarmiji.config.system-config)
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


(defn queue-message-handler-for-function [the-function]
  (proxy [Listener] []
    (message [header-map message-body]
      (try
        (the-function (json/decode-from-str message-body))
	(catch Exception e
	  (log-exception e))))))
    