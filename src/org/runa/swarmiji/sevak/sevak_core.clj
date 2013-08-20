(ns org.runa.swarmiji.sevak.sevak-core
  (:require [kits.structured-logging :as log])
  (:use org.runa.swarmiji.mpi.transport)
  (:use org.runa.swarmiji.rabbitmq.rabbitmq)
  (:use org.runa.swarmiji.client.client-core)
  (:use org.runa.swarmiji.config.system-config)
  (:use org.runa.swarmiji.utils.general-utils)
  (:use org.runa.swarmiji.sevak.bindings)
  (:use org.rathore.amit.utils.config)
  (:use org.rathore.amit.utils.clojure)
  (:use org.rathore.amit.medusa.core)
  (:import (java.util.concurrent Executors
                                 ExecutorService)))

(def sevaks (ref {}))

(def ^:const START-UP-REPORT "START_UP_REPORT")
(def ^:const SEVAK-SERVER "SEVAK_SERVER")

(def ^ExecutorService non-real-time-thread-pool (Executors/newFixedThreadPool 32))

(defn register-sevak [sevak-name function-info]
  (dosync 
   (alter sevaks assoc sevak-name function-info)))

;; implement the actual fn and client fn as mulit-methods
(defmacro create-function-and-sevak [service-name realtime? needs-response? args expr]
  (let [defining-ns *ns*]
    `(let [service-var# (defn ~(vary-meta service-name assoc ::sevak true)
                          ([~@args]
                             ;; this is the function that the client executes
                             (if-not (swarmiji-distributed-mode?)
                               (apply on-local (@sevaks (str (ns-name ~defining-ns) "/" '~service-name)) [~@args :sevak])
                               (if ~needs-response?
                                 (apply on-swarm ~realtime? (str (ns-name ~defining-ns) "/" '~service-name)  [~@args])
                                 (apply on-swarm-no-response ~realtime? (str (ns-name ~defining-ns) "/" '~service-name)  [~@args]))))
                          ([~@args ~'sevak] ;; this is the function that the sevak executes. Executed as (function args :sevak)
                             (when (= :sevak ~'sevak)
                               ~@expr)))
           sevak-name# (keyword (:name (meta service-var#)))]
       (log/info {:message "defining sevak job: "
                  :service-name '~service-name})
       (register-sevak (ns-qualified-name sevak-name# ~defining-ns)
                       {:name sevak-name#
                        :return ~needs-response?
                        :realtime ~realtime?
                        :fn ~service-name}))))

(defmacro defsevak [service-name args & expr]
  `(create-function-and-sevak ~service-name true true ~args ~expr))

(defmacro defseva [service-name args & expr]
  `(create-function-and-sevak ~service-name true false ~args ~expr))

(defmacro defsevak-nr [service-name args & expr]
  `(create-function-and-sevak ~service-name false true ~args ~expr))

(defmacro defseva-nr [service-name args & expr]
  `(create-function-and-sevak ~service-name false false ~args ~expr))

(defn execute-sevak [service-name service-handler service-args]
  (try
    (log/info {:message "execute-sevak"
               :service-name service-name
               :service-handler service-handler
               :service-args service-args})
    (let [response-with-time (run-and-measure-timing 
                              (apply (:fn service-handler) (concat service-args '(:sevak))))
          value (response-with-time :response)
          time-elapsed (response-with-time :time-taken)]
      {:response value :status :success :sevak-time time-elapsed})
    (catch InterruptedException ie
      (throw ie))
    (catch Exception e
      (log/error {:message "SEVAK ERROR"
                  :service-name service-name
                  :args service-args})
      (log/exception e)
      {:exception (-> e .getClass .getName)
       :stacktrace (#'log/stacktrace e)
       :status :error})))

(defn handle-sevak-request [service-handler sevak-name service-args return-q ack-fn]
  (with-swarmiji-bindings
    (try
      (let [response (merge 
                      {:return-q-name return-q :sevak-name sevak-name :sevak-server-pid (process-pid)}
                      (execute-sevak sevak-name service-handler service-args))]
        (when (and return-q (:return service-handler))
          (log/info {:message "handle-sevak-request: Returning request"
                     :sevak-name sevak-name
                     :return-queue return-q
                     :elapsed-time (:sevak-time response)
                     :response response})
          (send-message-no-declare return-q response)))
      (finally
        (ack-fn)))))

(defn sevak-request-handling-listener [message-obj ack-fn real-time?]
  (with-swarmiji-bindings
    (try
      (log/info {:message "sevak-request-handling-listener"})
      (let [service-name (message-obj :sevak-service-name)
            service-args (message-obj :sevak-service-args)
            return-q (message-obj :return-queue-name)
            service-handler (@sevaks service-name)]
        (when (nil? service-handler)
          (ack-fn)
          (throw (Exception. (str "No handler found for: " service-name))))
        (if real-time?
          (medusa-future-thunk return-q
                               #(handle-sevak-request service-handler service-name service-args return-q ack-fn))
          (.submit non-real-time-thread-pool
                   (reify Runnable  ;; couldn't get rid of reflection warning with merely: ^Runnable #(...)
                     (run [_] (handle-sevak-request service-handler service-name service-args return-q ack-fn))))))
      (catch Exception e
        (log/error {:message "SRHL: Error in sevak-request-handling-listener"})
        (log/exception e)))))


(defn start-broadcast-processor []
  (future 
    (with-swarmiji-bindings
      (let [broadcasts-q (random-queue-name "BROADCASTS_LISTENER_")]
        (try
          (log/info {:message "Listening for update broadcasts..."
                     :sevak-fanout-exchange-name (sevak-fanout-exchange-name)})
          (.addShutdownHook (Runtime/getRuntime)
                            (Thread. #(with-swarmiji-bindings (delete-queue broadcasts-q))))
          (start-queue-message-handler (sevak-fanout-exchange-name)
                                       FANOUT-EXCHANGE-TYPE
                                       broadcasts-q
                                       (random-queue-name)
                                       #(sevak-request-handling-listener %1 %2 false))
          (log/info {:message "Done with broadcasts!"})    
          (catch Exception e         
            (log/error {:message "Error in update broadcasts future!"})
            (log/exception e)))))))

(defn start-processor [routing-key real-time? start-log-message]
  (future
    (with-swarmiji-bindings 
      (try
        (with-prefetch-count (rabbitmq-prefetch-count)
          (start-queue-message-handler routing-key
                                       routing-key
                                       (fn [message-obj ack-fn]
                                         (sevak-request-handling-listener message-obj ack-fn real-time?))))
        (log/info {:message "start-processor: Done with sevak requests!"})
        (catch Exception e
          (log/error {:message "start-processor: Error in sevak-servicing future!"})
          (log/exception e))))))


(defn boot-sevak-server []
  (log/info {:message "Starting sevaks in"
             :mode *swarmiji-env*
             :system-config (operation-config)
             :medusa-client-thread-count (medusa-client-thread-count)
             :rabbitmq-prefetch-count (rabbitmq-prefetch-count)
             :services (keys @sevaks)})
  (init-rabbit)
  (init-medusa (medusa-server-thread-count))
  (start-processor (queue-sevak-q-name true) true "Starting to serve realtime sevak requests..." )
  (start-processor (queue-sevak-q-name false) false "Starting to serve non-realtime sevak requests..." )
  (log/info {:message "Sevak Server Started!"}))
