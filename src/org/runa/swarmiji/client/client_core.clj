(ns org.runa.swarmiji.client.client-core)

(use 'org.runa.swarmiji.mpi.sevak-proxy)
(require '(org.danlarkin [json :as json]))
(use '[clojure.contrib.duck-streams :only (spit)])

(defn response-value-from [sevak-data]
  (if (not (= :success (keyword (sevak-data :status))))
    (throw (Exception. "Sevak has errors!"))
    (sevak-data :response)))

(defn on-swarm [sevak-service & args]
  (let [sevak-data (ref :swarmiji-sevak-init)
	on-swarm-response (fn [response-json-object] 
			      (dosync (ref-set sevak-data response-json-object)))
	on-swarm-proxy-client (new-proxy (name sevak-service) args on-swarm-response)]
    (fn [accessor]
      (cond
	(= accessor :complete?) (not (= :swarmiji-sevak-init @sevak-data))
	(= accessor :value) (response-value-from @sevak-data)
	(= accessor :status) (@sevak-data :status)
	(= accessor :exception) (@sevak-data :exception)
	(= accessor :stacktrace) (@sevak-data :stacktrace)
	(= accessor :_inner_ref) @sevak-data
	:default (throw (Exception. (str "On-swarm proxy error - unknown message:" accessor)))))))

(defn all-complete? [swarm-requests]
  (reduce #(and (%2 :complete?) %1) true swarm-requests))

(defn wait-until-completion [swarm-requests allowed-time]
  (loop [all-complete (all-complete? swarm-requests) elapsed-time 0]
    (if (> elapsed-time allowed-time)
      (throw (RuntimeException. (str "Swarmiji says: This operation has taken more than " allowed-time "milliseconds")))
       (if (not all-complete)
	 (do
	   (Thread/sleep 100)
	   (recur (all-complete? swarm-requests) (+ elapsed-time 100)))))))

(defmacro from-swarm [max-time-allowed swarm-requests expr]
  (list 'do (list 'wait-until-completion swarm-requests max-time-allowed) expr))
