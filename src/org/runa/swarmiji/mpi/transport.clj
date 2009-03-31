(ns org.runa.swarmiji.mpi.transport)

(require '(org.danlarkin [json :as json]))
(import '(net.ser1.stomp Client Listener))

(defn send-on-transport [q-name q-message-object]
  (let [client (Client. "tank.cinchcorp.com" 61613, "guest" "guest")
	q-message-string (json/encode-to-str q-message-object)]
    (.send client q-name q-message-string)
    client))