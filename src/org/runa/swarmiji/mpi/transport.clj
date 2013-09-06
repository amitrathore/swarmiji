(ns org.runa.swarmiji.mpi.transport
  (:gen-class)
  (:require [kits.structured-logging :as log]
            [org.runa.swarmiji.rabbitmq.connection :as conn]
            [org.runa.swarmiji.rabbitmq.channel :as channel])
  (:use org.runa.swarmiji.config.system-config)
  (:use org.runa.swarmiji.rabbitmq.rabbitmq)
  (:use org.runa.swarmiji.sevak.bindings)
  (:use org.runa.swarmiji.utils.general-utils)
  (:use org.rathore.amit.utils.clojure)
  (:use org.rathore.amit.medusa.core))

(def rabbit-down-messages (atom {}))
(def ^:dynamic *guaranteed-sevaks*)
(def ^:const BROADCASTS-QUEUE-NAME "BROADCASTS_GLOBAL")

(defn send-message-on-queue [q-name q-message-object]
  (with-swarmiji-bindings
    (try
      (channel/send-message channel/DEFAULT-EXCHANGE-NAME channel/DEFAULT-EXCHANGE-TYPE q-name q-message-object)
      (catch Exception e
        #_(log/exception e)))))

(defn send-message-no-declare [q-name q-message-object]
  (with-swarmiji-bindings
    (try
      (channel/send-message-if-queue q-name q-message-object)
      (catch Exception e
        #_(log/exception e)))))

(defn fanout-message-to-all [message-object]
  (channel/send-message (sevak-fanout-exchange-name) channel/FANOUT-EXCHANGE-TYPE BROADCASTS-QUEUE-NAME message-object))

(defmacro multicast-to-sevak-servers [sevak-var & args]
  (let [{:keys [name ns] :as meta-inf} (meta (resolve sevak-var))]
    (if-not meta-inf (throw (Exception. (format "Multicast-to-sevak-servers is unable to resolve %s" sevak-var))))
    `(fanout-message-to-all (sevak-queue-message-no-return ~(ns-qualified-name name ns) (list ~@args)))))

(defn should-fallback [sevak-name]
  (some #{(keyword sevak-name)} *guaranteed-sevaks*))

(defn send-and-register-callback [realtime? return-q-name custom-handler request-object]
  (let [chan (channel/create-channel)
        consumer (channel/consumer-for chan channel/DEFAULT-EXCHANGE-NAME channel/DEFAULT-EXCHANGE-TYPE return-q-name return-q-name)
        on-response (fn [msg]
                      (with-swarmiji-bindings
                        (try
                          (custom-handler msg)
                          (finally
			    (.queueDelete chan return-q-name)
			    (channel/close-channel chan)))))
        f (fn []
            (send-message-on-queue (queue-sevak-q-name realtime?) request-object)
            (on-response (channel/delivery-from chan consumer)))]
    (f)
    {:channel chan :queue return-q-name :consumer consumer}))

(defn add-to-rabbit-down-queue [realtime? return-queue-name custom-handler request-object]
  (swap! rabbit-down-messages
         assoc
         (System/currentTimeMillis)
         [realtime? return-queue-name custom-handler request-object]))

(defn register-callback-or-fallback [realtime? return-q-name custom-handler request-object]
  (try
    (send-and-register-callback realtime? return-q-name custom-handler request-object)
    (catch java.net.ConnectException ce
      (if (should-fallback (:sevak-service-name request-object))
        (add-to-rabbit-down-queue realtime? return-q-name custom-handler request-object)))))

(defn retry-message [timestamp [realtime? return-queue-name custom-handler request-object]]
  (with-swarmiji-bindings
    (try
      (send-and-register-callback realtime? return-queue-name custom-handler request-object)
      (swap! rabbit-down-messages dissoc timestamp)
      (catch java.net.ConnectException ce
        #_(log/error {:message "RabbitMQ still down, will retry"
                    :rabbit-down-messages (count @rabbit-down-messages)})) ;;ignore, will try again later
      (catch Exception e
        #_(log/error {:message "Trouble in swarmiji auto-retry!"})
        #_(log/exception e)))))

(defn retry-periodically [sleep-millis]
  (Thread/sleep sleep-millis)
  (doseq [[timestamp payload] @rabbit-down-messages]
    (retry-message timestamp payload))
  (recur sleep-millis))

(defrunonce start-retry-rabbit [sleep-millis]
  (future
    (retry-periodically sleep-millis)))

(defn init-rabbit []
  #_(log/info {:message "Init Rabbit"
               :host (queue-host)})
  (conn/init-connection-factory (queue-host) (queue-username) (queue-password))
  (start-retry-rabbit 10000))
