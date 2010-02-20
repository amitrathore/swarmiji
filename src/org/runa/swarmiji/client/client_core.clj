(ns org.runa.swarmiji.client.client-core)

(use 'org.runa.swarmiji.mpi.sevak-proxy)
(use 'org.runa.swarmiji.mpi.transport)
(use 'org.runa.swarmiji.sevak.bindings)
(use 'org.runa.swarmiji.config.system-config)
(use 'org.runa.swarmiji.utils.general-utils)
(import '(java.io StringWriter))
(import '(org.runa.swarmiji.exception SevakErrors))
(use 'org.rathore.amit.utils.config)
(use 'org.rathore.amit.utils.logger)
(use 'org.rathore.amit.utils.clojure)

(def WORK-REPORT "WORK_REPORT")

(declare send-work-report)

(def swarmiji-sevak-init-value :__swarmiji-sevak-init__)

(defn attribute-from-response [sevak-data attrib-name]
  (if (= swarmiji-sevak-init-value sevak-data)
    (throw (Exception. "Sevak not complete!")))
  (if (not (= :success (keyword (sevak-data :status))))
    (throw (SevakErrors. "Sevak has errors!")))
  (sevak-data attrib-name))

(defn response-value-from [sevak-data]
  (attribute-from-response sevak-data :response))

(defn time-on-server [sevak-data]
  (attribute-from-response sevak-data :sevak-time))

(defn return-q [sevak-data]
  (attribute-from-response sevak-data :return-q-name))

(defn sevak-server-pid [sevak-data]
  (attribute-from-response sevak-data :sevak-server-pid))

(defn sevak-name-from [sevak-data]
  (attribute-from-response sevak-data :sevak-name))

(defn disconnect-proxy [sevak-proxy]
  (if sevak-proxy 
    (let [chan (:channel sevak-proxy) queue (:queue sevak-proxy)]
      (try
       (with-swarmiji-bindings
	 (.queueDelete chan queue)
	 (catch Exception e))))))
         ;no-op, this sevak-proxy should be aborted, thats it

(defn on-swarm [sevak-service & args]
  (let [sevak-start (ref (System/currentTimeMillis))
	total-sevak-time (ref nil)
	sevak-data (ref swarmiji-sevak-init-value)
	complete? (fn [] (not (= swarmiji-sevak-init-value @sevak-data)))
	success? (fn [] (= (:status @sevak-data) :success))
	sevak-name (fn [] (sevak-name-from @sevak-data))
	sevak-time (fn [] (time-on-server @sevak-data))
	messaging-time (fn [] (- @total-sevak-time (sevak-time)))
	on-swarm-response (fn [response-object]
			    (dosync (ref-set sevak-data response-object))
			     (do
			       (dosync (ref-set total-sevak-time (- (System/currentTimeMillis) @sevak-start)))
			       (if (and (swarmiji-diagnostics-mode?) (success?))
				 (send-work-report (sevak-name) args (sevak-time) (messaging-time) (return-q @sevak-data) (sevak-server-pid @sevak-data)))))
	on-swarm-proxy-client (new-proxy (name sevak-service) args on-swarm-response)]
    (fn [accessor]
      (cond
	(= accessor :sevak-name) (name sevak-service)
	(= accessor :args) args
	(= accessor :distributed?) true
	(= accessor :sevak-type) :sevak-with-return
	(= accessor :disconnect) (disconnect-proxy on-swarm-proxy-client)
	(= accessor :complete?) (complete?)
	(= accessor :value) (response-value-from @sevak-data)
	(= accessor :status) (@sevak-data :status)
	(= accessor :sevak-time) (sevak-time)
	(= accessor :total-time) @total-sevak-time
	(= accessor :messaging-time) (messaging-time)
	(= accessor :exception) (@sevak-data :exception)
	(= accessor :stacktrace) (@sevak-data :stacktrace)
	(= accessor :__inner_ref) @sevak-data
	:default (throw (Exception. (str "On-swarm proxy error - unknown message:" accessor)))))))


(defn on-swarm-no-response [sevak-service & args]
  (new-proxy (name sevak-service) args)
  nil)

(defn all-complete? [swarm-requests]
  (every? #(% :complete?) swarm-requests))

(defn disconnect-all [swarm-requests]
  (doseq [req swarm-requests]
    (req :disconnect)))

(defn wait-until-completion [swarm-requests allowed-time]
  (loop [all-complete (all-complete? swarm-requests) elapsed-time 0]
    (if (> elapsed-time allowed-time)
      (do
	(disconnect-all swarm-requests)
	(throw (RuntimeException. (str "Swarmiji reports: This operation has taken more than " allowed-time " milliseconds."))))
       (if (not all-complete)
	 (do
	   (Thread/sleep 100)
	   (recur (all-complete? swarm-requests) (+ elapsed-time 100)))))))

(defmacro from-swarm [max-time-allowed swarm-requests & expr]
  `(do
     (wait-until-completion ~swarm-requests ~max-time-allowed)
     ~@expr))

(defn on-local [sevak-service-function & args]
  (let [response-with-time (ref {})]
    (dosync 
     (ref-set response-with-time 
              (simulate-serialized
               (run-and-measure-timing 
                (apply (:fn sevak-service-function) args)))))
    (fn [accessor]
      (cond
	(= accessor :sevak-name) sevak-service-function
	(= accessor :args) args
	(= accessor :distributed?) false
	(= accessor :disconnect) nil
	(= accessor :complete?) true
	(= accessor :status) "success"
	(= accessor :sevak-time) (@response-with-time :time-taken)
	(= accessor :messaging-time) 0
	(= accessor :total-time) (@response-with-time :time-taken)
	(= accessor :exception) nil
	(= accessor :stacktrace) nil
	(= accessor :_inner_ref) @response-with-time
	(= accessor :value) (@response-with-time :response)
	:default (throw (Exception. (str "On-local proxy error - unknown message:" accessor)))))))
    
(defn send-work-report [sevak-name args sevak-time messaging-time return-q sevak-server-pid]
  (let [report {:message_type WORK-REPORT
		:sevak_name sevak-name
		:sevak_args (str args)
		:sevak_time sevak-time
		:messaging_time messaging-time
		:return_q_name return-q
		:sevak_server_pid sevak-server-pid}]
    (send-message-on-queue (queue-diagnostics-q-name) report)))