(ns org.runa.swarmiji.client.client-core)

(use 'org.runa.swarmiji.mpi.sevak-proxy)
(use 'org.runa.swarmiji.mpi.transport)
(use 'org.runa.swarmiji.utils.logger)
(use 'org.runa.swarmiji.config.system-config)
(use 'org.runa.swarmiji.utils.general-utils)
(require '(org.danlarkin [json :as json]))
(import '(java.io StringWriter))

(def WORK-REPORT "WORK_REPORT")

(declare send-work-report)

(def swarmiji-sevak-init-value :__swarmiji-sevak-init__)

(defn attribute-from-response [sevak-data attrib-name]
  (if (= swarmiji-sevak-init-value sevak-data)
    (throw (Exception. "Sevak not complete!")))
  (if (not (= :success (keyword (sevak-data :status))))
    (throw (Exception. "Sevak has errors!")))
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

(defn on-swarm [sevak-service & args]
  (let [sevak-start (ref (System/currentTimeMillis))
	total-sevak-time (ref nil)
	sevak-data (ref swarmiji-sevak-init-value)
	complete? (fn [] (not (= swarmiji-sevak-init-value @sevak-data)))
	sevak-name (fn [] (sevak-name-from @sevak-data))
	sevak-time (fn [] (time-on-server @sevak-data))
	messaging-time (fn [] (- @total-sevak-time (sevak-time)))
	on-swarm-response (fn [response-json-object] 
			    (dosync (ref-set sevak-data response-json-object))
			    (if (complete?)
			      (do
				(dosync (ref-set total-sevak-time (- (System/currentTimeMillis) @sevak-start)))
				(if (swarmiji-diagnostics-mode?) 
				  (send-work-report (sevak-name) args (sevak-time) (messaging-time) (return-q @sevak-data) (sevak-server-pid @sevak-data))))))
	on-swarm-proxy-client (new-proxy (name sevak-service) args on-swarm-response)]
    (fn [accessor]
      (cond
	(= accessor :distributed?) true
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

(defn all-complete? [swarm-requests]
  (reduce #(and (%2 :complete?) %1) true swarm-requests))

(defn wait-until-completion [swarm-requests allowed-time]
  (loop [all-complete (all-complete? swarm-requests) elapsed-time 0]
    (if (> elapsed-time allowed-time)
      (throw (RuntimeException. (str "Swarmiji reports: This operation has taken more than " allowed-time " milliseconds.")))
       (if (not all-complete)
	 (do
	   (Thread/sleep 100)
	   (recur (all-complete? swarm-requests) (+ elapsed-time 100)))))))

(defmacro from-swarm [max-time-allowed swarm-requests expr]
  (list 'do (list 'wait-until-completion swarm-requests max-time-allowed) expr))

(defn on-local [sevak-service-function & args]
  (let [response-with-time (ref {})]
    (fn [accessor]
      (cond
	(= accessor :distributed?) false
	(= accessor :complete?) true
	(= accessor :status) "success"
	(= accessor :sevak-time) (@response-with-time :time-taken)
	(= accessor :messaging-time) 0
	(= accessor :total-time) (@response-with-time :time-taken)
	(= accessor :exception) nil
	(= accessor :stacktrace) nil
	(= accessor :_inner_ref) @response-with-time
	(= accessor :value) (dosync 
			      (ref-set response-with-time 
				       (simulate-jsonified
					(run-and-measure-timing 
					 (apply sevak-service-function args))))
			      (@response-with-time :response))
	:default (throw (Exception. (str "On-local proxy error - unknown message:" accessor)))))))
    
(defn send-work-report [sevak-name args sevak-time messaging-time return-q sevak-server-pid]
  (let [report {:message_type WORK-REPORT
		:sevak_name sevak-name
		:sevak_args (str args)
		:sevak_time sevak-time
		:messaging_time messaging-time
		:return_q_name return-q
		:sevak_server_pid sevak-server-pid}]
    (log-message "Work report for diagnostics:" report)
    (send-on-transport (queue-diagnostics-q-name) report)))