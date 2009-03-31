(ns org.runa.swarmiji.sevak.sevak-core)

(use 'org.runa.swarmiji.mpi.transport)
(import '(net.ser1.stomp Client Listener))
(require '(org.danlarkin [json :as json]))

(def sevaks (ref {}))

(defmacro defsevak [service-name args expr]
  `(let [sevak-name# (keyword (str '~service-name))]
     (dosync (ref-set sevaks (assoc @sevaks sevak-name# (fn ~args ~expr))))))

(defn response-envelope [service-response]
  {:response service-response
   :status "success"})

(defn log-exception [e]
  (println e)
  (println (.getMessage e))
  (.printStackTrace e))

(defn handle-sevak-request [service-handler service-args]
  (try
   (apply service-handler service-args)
   (catch Exception e (log-exception e))))

(defn sevak-request-handling-listener []
  (proxy [Listener] []
    (message [headerMap messageBody]
      (let [req-json (json/decode-from-str messageBody)
	    _ (println "got request" req-json)
	    service-name (req-json :sevak-service-name) service-args (req-json :sevak-service-args) return-q (req-json :return-queue-name)
	    service-handler (@sevaks (keyword service-name))
	    service-response (handle-sevak-request service-handler service-args)
	    response-json-object (response-envelope service-response)]
	(send-on-transport return-q response-json-object)))))

(defn start-sevak-listener []
  (let [client (Client. "tank.cinchcorp.com" 61613, "guest" "guest")
	sevak-request-handler (sevak-request-handling-listener)]
    (.subscribe client "RUNA_SWARMIJI_TRANSPORT" sevak-request-handler)))

(defn boot []
  (do
    (println "Sevaks are offering the following" (count @sevaks) "services:" (keys @sevaks))
    (start-sevak-listener)))