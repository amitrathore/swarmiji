(ns org.runa.swarmiji.client.core)

(use 'org.runa.swarmiji.mpi.sevak-proxy)
(require '(org.danlarkin [json :as json]))
(use '[clojure.contrib.duck-streams :only (spit)])

(defn on-swarm [sevak-service & args]
  (let [sevak-data (ref :swarmiji-sevak-init)
	on-swarm-response (fn [response-json-object] 
			      (dosync (ref-set sevak-data response-json-object)))
	on-swarm-proxy-client (new-proxy (name sevak-service) args on-swarm-response)]
    (fn [accessor]
      (cond
	(= accessor :complete?) (not (= :swarmiji-sevak-init @sevak-data))
	(= accessor :value) @sevak-data))))

(defn all-complete? [swarm-requests]
  (reduce #(and (%2 :complete?) %1) true swarm-requests))

(defn wait-until-completion [swarm-requests]
  (loop [all-complete (all-complete? swarm-requests)]
    (if (not all-complete)
      (do
	(Thread/sleep 100)
	(recur (all-complete? swarm-requests))))))

(defmacro from-swarm [swarm-requests expr]
  (list 'do (list 'wait-until-completion swarm-requests) expr))
  