(ns org.runa.swarmiji.client.core)

(use 'org.runa.swarmiji.mpi.sevak-proxy)
(require '(org.danlarkin [json :as json]))
(use '[clojure.contrib.duck-streams :only (spit)])

(defn on-swarm [sevak-service & args]
  (let [sevak-data (ref :swarmiji-sevak-init)
	on-swarm-response (fn [response-json-object] 
			    (do
			      (spit "/Users/amit/workspace/swarmiji/swarm.txt" (json/encode-to-str response-json-object))
			      (dosync (ref-set sevak-data response-json-object))))
	on-swarm-proxy-client (new-proxy (name sevak-service) args on-swarm-response)]
    (fn [accessor]
      (cond
	(= accessor :complete?) (not (= :swarmiji-sevak-init @sevak-data))
	(= accessor :value) @sevak-data))))
