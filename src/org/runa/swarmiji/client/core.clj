(ns org.runa.swarmiji.client.core)

(defn on-swarm [sevak-service & args]
  (let [sevak-agent (agent :init)]
    (fn [accessor]
      (cond
	(= accessor :complete?)