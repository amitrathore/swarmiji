(ns org.runa.swarmiji.mpi.transport)

(use 'org.runa.swarmiji.config.queue-config)
(require '(org.danlarkin [json :as json]))
(import '(net.ser1.stomp Client Listener))

(defn new-queue-client []
  (Client. (queue-host) (queue-port), (queue-username) (queue-password)))

(defn send-on-transport [q-name q-message-object]
  (let [client (new-queue-client)
	q-message-string (json/encode-to-str q-message-object)]
    (.send client q-name q-message-string)
    client))