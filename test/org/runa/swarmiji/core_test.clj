(ns org.runa.swarmiji.core-test
  (:require [clojure.test :refer :all]
            org.runa.swarmiji.client.client-core
            [org.runa.swarmiji.config.system-config :as config]
            org.runa.swarmiji.monitor.control-message
            org.runa.swarmiji.monitor.recorder
            org.runa.swarmiji.mpi.sevak-proxy
            [org.runa.swarmiji.mpi.transport :as transport]
            org.runa.swarmiji.sevak.bindings
            [org.runa.swarmiji.sevak.sevak-core :as core]
            org.runa.swarmiji.utils.general-utils))


(defn swarmiji-config [distributed-mode?]
  {:operation-configs {:swarmiji-username "furtive"
                       :host "abaranosky-Hercules2-runastack"
                       :port 61613
                       :q-username "guest"
                       :q-password "guest"
                       :sevak-request-queue-prefix "RUNA_SWARMIJI_TRANSPORT_frylock_development_"
                       :sevak-diagnostics-queue-prefix "RUNA_SWARMIJI_DIAGNOSTICS_frylock_"
                       :sevak-fanout-exchange-prefix "RUNA_SWARMIJI_FANOUT_EXCHANGE_development_"
                       :distributed-mode distributed-mode?
                       :diagnostics-mode false
                       :logsdir "/var/log/swarm/furtive/"
                       :log-to-console true
                       :rabbit-prefetch-count 3
                       :rabbit-max-pool-size 72
                       :rabbit-max-idle-size 72
                       :medusa-server-thread-count 72
                       :medusa-client-thread-count 72
                       :reload-namespaces false}})

(defn- init [distributed-mode?]
  (config/set-config (swarmiji-config distributed-mode?))
  (core/boot-sevak-server))

(defmacro ^:private with-fresh-sevaks [& body]
  `(try
     (dosync (ref-set core/sevaks {}))
     ~@body
     (finally
       (dosync (ref-set core/sevaks {})))))

(deftest test-happy-path
  (testing "Can setup, and execute a simple sevak, in both distributed and non-distributed modes"
    (doseq [distributed-mode? [true false]]
      (with-fresh-sevaks
        (init distributed-mode?)

        (core/defsevak increment [n]
          (inc n))

        (is (= 6 ((increment 5) :value))
            (str "DISTRIBUTED MODE?::" distributed-mode?)))))

  (testing "Can setup, and execute a simple (asynchronous) seva, in both distributed and non-distributed modes"
    (doseq [distributed-mode? [true false]]
      (with-fresh-sevaks
        (init distributed-mode?)

        (def number (atom 5))
        
        (core/defseva increment []
          (swap! number inc))

        (increment)
        (Thread/sleep 100)
        
        (is (= 6 @number)
            (str "DISTRIBUTED MODE?::" distributed-mode?))))))

