(ns org.runa.swarmiji.rabbitmq.rabbitmq
  (:require [kits.structured-logging :as log]
            [org.runa.swarmiji.rabbitmq.channel :as channel]
            [org.runa.swarmiji.rabbitmq.connection :as conn]
            [org.runa.swarmiji.utils.general-utils :as utils])
  (:import (com.rabbitmq.client Channel QueueingConsumer)))


(defn- random-queue []
  (str (java.util.UUID/randomUUID)))

(declare guaranteed-delivery-from)

(defn- recover-from-delivery [exchange-name exchange-type queue-name routing-key channel-atom consumer-atom]
  (try 
   (utils/wait-for-seconds (rand-int 7))
   (let [new-channel (channel/create-channel)
         new-consumer (channel/consumer-for new-channel exchange-name exchange-type queue-name routing-key)]
     (reset! channel-atom new-channel)
     (reset! consumer-atom new-consumer)
     #(guaranteed-delivery-from exchange-name exchange-type queue-name routing-key channel-atom consumer-atom))
   (catch Exception e
     #_(log/error {:message "recover-from-delivery: retrying"})
     #_(log/exception e)
     #(recover-from-delivery exchange-name exchange-type queue-name routing-key channel-atom consumer-atom))))

(defn- guaranteed-delivery-from [exchange-name exchange-type queue-name routing-key channel-atom consumer-atom]
  (try
   (channel/delivery-from @channel-atom @consumer-atom)
   (catch Exception e
     #_(log/error "guaranteed-delivery-from: recovering")
     #_(log/exception e)
     #(recover-from-delivery exchange-name exchange-type queue-name routing-key channel-atom consumer-atom))))

(defn- lazy-message-seq [exchange-name exchange-type queue-name routing-key channel-atom consumer-atom]
  (lazy-seq
    (let [message (trampoline guaranteed-delivery-from exchange-name exchange-type queue-name routing-key channel-atom consumer-atom)]
      (cons message (lazy-message-seq exchange-name exchange-type queue-name routing-key channel-atom consumer-atom)))))

(defn- message-seq [exchange-name exchange-type channel queue-name routing-key]
  (let [channel-atom (atom channel) 
        consumer-atom (atom (channel/consumer-for channel exchange-name exchange-type queue-name routing-key))]
    (lazy-message-seq exchange-name exchange-type queue-name routing-key channel-atom consumer-atom)))

(defn start-queue-message-handler 
  ([routing-key handler-fn]
     (start-queue-message-handler channel/DEFAULT-EXCHANGE-NAME channel/DEFAULT-EXCHANGE-TYPE routing-key (random-queue) handler-fn))
  ([queue-name routing-key handler-fn]
     (start-queue-message-handler channel/DEFAULT-EXCHANGE-NAME channel/DEFAULT-EXCHANGE-TYPE routing-key (random-queue) handler-fn))
  ([exchange-name exchange-type routing-key handler-fn]
     (start-queue-message-handler exchange-name exchange-type routing-key (random-queue) handler-fn))
  ([exchange-name exchange-type queue-name routing-key handler-fn]
     (conn/ensure-thread-local-connection)
     (with-open [channel (channel/create-channel)]
       (doseq [[m ack-fn] (message-seq exchange-name exchange-type channel queue-name routing-key)]
         (handler-fn m ack-fn)))))
