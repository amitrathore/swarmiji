(ns org.runa.swarmiji.sevak.sevak-core)

(use 'org.runa.swarmiji.mpi.transport)
(use 'org.runa.swarmiji.utils.exception-utils)
(import '(net.ser1.stomp Client Listener))
(require '(org.danlarkin [json :as json]))

(def sevaks (ref {}))

(defmacro defsevak [service-name args expr]
  `(let [sevak-name# (keyword (str '~service-name))]
     (dosync (ref-set sevaks (assoc @sevaks sevak-name# (fn ~args ~expr))))))

(defn handle-sevak-request [service-handler service-args]
  (try
   {:response (apply service-handler service-args) :status :success}
   (catch Exception e (do
			(log-exception e)
			{:exception (exception-name e) :stacktrace (stacktrace e) :status :error}))))

(defn sevak-request-handling-listener []
  (proxy [Listener] []
    (message [headerMap messageBody]
      (let [req-json (json/decode-from-str messageBody)
	    _ (println "got request" req-json)
	    service-name (req-json :sevak-service-name) service-args (req-json :sevak-service-args) return-q (req-json :return-queue-name)
	    service-handler (@sevaks (keyword service-name))
	    response-envelope (handle-sevak-request service-handler service-args)]
	(send-on-transport return-q response-envelope)))))

(defn start-sevak-listener []
  (let [client (Client. "tank.cinchcorp.com" 61613, "guest" "guest")
	sevak-request-handler (sevak-request-handling-listener)]
    (.subscribe client "RUNA_SWARMIJI_TRANSPORT" sevak-request-handler)))

(defn boot []
  (println "Sevaks are offering the following" (count @sevaks) "services:" (keys @sevaks))
  (start-sevak-listener))