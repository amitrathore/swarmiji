(ns org.runa.swarmiji.client.client-core
  (:gen-class)
  (:use
   org.runa.swarmiji.mpi.sevak-proxy
   org.runa.swarmiji.mpi.transport
   org.runa.swarmiji.sevak.bindings
   org.runa.swarmiji.config.system-config
   org.runa.swarmiji.utils.general-utils
   org.rathore.amit.utils.config
   org.rathore.amit.utils.logger
   org.rathore.amit.utils.clojure)
  (:import
   (java.io StringWriter)
   (org.runa.swarmiji.exception SevakErrors)
   (java.util.concurrent TimeoutException TimeUnit CountDownLatch)))


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
    (let [{:keys [channel queue]} sevak-proxy]
      (try
        (with-swarmiji-bindings
          (.queueDelete channel queue)
          (catch Exception e))))))
                                        ;no-op, this sevak-proxy should be aborted, thats it

(defn on-swarm [realtime? sevak-service & args]
  (let [sevak-start (ref (System/currentTimeMillis))
        total-sevak-time (ref nil)
        latch (CountDownLatch. 1)
        sevak-data (ref swarmiji-sevak-init-value)
        complete? (fn [] (not (= swarmiji-sevak-init-value @sevak-data)))
        success? (fn [] (= (:status @sevak-data) :success))
        sevak-name (fn [] (sevak-name-from @sevak-data))
        sevak-time (fn [] (time-on-server @sevak-data))
        messaging-time (fn [] (- @total-sevak-time (sevak-time)))
        on-swarm-response (fn [[response-obj ack-fn]]
                            (ack-fn)
                            (dosync
                              (ref-set sevak-data response-obj)
                              (dosync (ref-set total-sevak-time (- (System/currentTimeMillis) @sevak-start))))
                            (when (and (swarmiji-diagnostics-mode?) (success?))
                              (send-work-report (sevak-name) args (sevak-time) (messaging-time) (return-q @sevak-data) (sevak-server-pid @sevak-data)))
                            (.countDown latch))
        on-swarm-proxy-client (new-proxy realtime? sevak-service args on-swarm-response)]
    (fn [accessor]
      (condp = accessor
        :sevak-name sevak-service
        :args args
        :distributed? true
        :sevak-type :sevak-with-return
        :latch latch
        :disconnect (disconnect-proxy on-swarm-proxy-client)
        :complete? (complete?)
        :value (response-value-from @sevak-data)
        :status (@sevak-data :status)
        :sevak-time (sevak-time)
        :total-time @total-sevak-time
        :messaging-time (messaging-time)
        :exception (@sevak-data :exception)
        :stacktrace (@sevak-data :stacktrace)
        :__inner_ref @sevak-data
        :sevak-proxy on-swarm-proxy-client
        :default (throw (Exception. (str "On-swarm proxy error - unknown message:" accessor)))))))


(defn on-swarm-no-response [realtime? sevak-service & args]
  (new-proxy realtime? sevak-service args)
  nil)

(defn disconnect-all [swarm-requests]
  (doseq [req swarm-requests]
    (req :disconnect)))

(defn log-timeouts [swarm-requests]
  (doseq [r swarm-requests]
    (when (r :distributed?)
      (log-message "Sevak response timed-out on" (r :sevak-name)
                   "for return-q" ((r :sevak-proxy) :queue)))))

(defn throw-exception [allowed-time]
  (throw (RuntimeException. (str "Swarmiji reports: This operation has taken more than " allowed-time " milliseconds."))))

(defn wait-until-completion
  ([swarm-requests allowed-time]
     (wait-until-completion swarm-requests allowed-time throw-exception))
  ([swarm-requests allowed-time error-fn]
     (let [start (System/currentTimeMillis)
           remaining-time #(- allowed-time (- (System/currentTimeMillis) start))
           latches (map :latch swarm-requests)]
       (try
         (doseq [r swarm-requests]
           (let [latch ^CountDownLatch (r :latch)]
             (.await latch (remaining-time) TimeUnit/MILLISECONDS)
             (when (r :distributed?)
               (log-message "Received sevak response on" (r :sevak-name)
                            "for return-q" (return-q (r :__inner_ref)) "with elapsed time" (r :total-time)))))
         (catch TimeoutException _
           (log-timeouts swarm-requests)
           (error-fn allowed-time))
         (finally
           (disconnect-all swarm-requests))))))

(defn wait-until-completion-no-exception
  [swarm-requests allowed-time]
  (wait-until-completion swarm-requests allowed-time (constantly nil)))

(defmacro from-swarm [max-time-allowed swarm-requests & expr]
  `(do
     (wait-until-completion ~swarm-requests ~max-time-allowed)
     ~@expr))

(defmacro from-swarm-no-exception [max-time-allowed swarm-requests & expr]
  `(do
     (wait-until-completion-no-exception ~swarm-requests ~max-time-allowed)
     ~@expr))

(defn retry-sevaks [retry-timeout sevaks sevak-fn]
  (let [incomplete-sevaks (filter #(not (% :complete?)) sevaks)
        new-sevaks (map #(apply sevak-fn (% :args)) incomplete-sevaks)]
    (from-swarm retry-timeout new-sevaks)))

(defn on-local [sevak-service-function & args]
  (let [response-with-time (ref {})
        result (run-and-measure-timing
                 (apply (:fn sevak-service-function) args))]
    (dosync (ref-set response-with-time result))
    (fn [accessor]
      (cond
       (= accessor :sevak-name) sevak-service-function
       (= accessor :args) args
       (= accessor :latch) (CountDownLatch. 0)
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
  (log-message "send-work-report:" sevak-name)
  (let [report {:message_type WORK-REPORT
                :sevak_name sevak-name
                :sevak_args (str args)
                :sevak_time sevak-time
                :messaging_time messaging-time
                :return_q_name return-q
                :sevak_server_pid sevak-server-pid}]
    (send-message-on-queue (queue-diagnostics-q-name) report)))
