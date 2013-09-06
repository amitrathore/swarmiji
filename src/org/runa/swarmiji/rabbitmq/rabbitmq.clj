(ns org.runa.swarmiji.rabbitmq.rabbitmq
  (:require [kits.structured-logging :as log]
            [taoensso.nippy :as nippy]
            [org.runa.swarmiji.rabbitmq.channel :as channel]
            [org.runa.swarmiji.rabbitmq.connection :as conn]
            [org.runa.swarmiji.utils.general-utils :as utils])
  (:import (com.rabbitmq.client Channel QueueingConsumer)))

(def ^:const DEFAULT-EXCHANGE-NAME "default-exchange")
(def ^:const DEFAULT-EXCHANGE-TYPE "direct")
(def ^:const FANOUT-EXCHANGE-TYPE "fanout")

(defn- serialize [x]
  (nippy/freeze x))

(defn- deserialize [^"[Ljava.lang.Byte;" message-body]
  (nippy/thaw message-body))

(defn send-message
  ([routing-key message-object]
     (send-message DEFAULT-EXCHANGE-NAME DEFAULT-EXCHANGE-TYPE routing-key message-object))
  ([exchange-name exchange-type routing-key message-object]
     (conn/ensure-thread-local-connection)
     (with-open [channel (channel/create-channel)]
       (.exchangeDeclare channel exchange-name exchange-type)
       (.queueDeclare channel routing-key false false false nil)
       (.basicPublish channel exchange-name routing-key nil (serialize message-object)))))

(defn send-message-if-queue
  ([routing-key message-object]
     (send-message-if-queue DEFAULT-EXCHANGE-NAME DEFAULT-EXCHANGE-TYPE routing-key message-object))
  ([exchange-name exchange-type routing-key message-object]
     (conn/ensure-thread-local-connection)
     (with-open [channel (channel/create-channel)]
       (.basicPublish channel exchange-name routing-key nil (serialize message-object)))))

(defn delivery-from [^Channel channel ^QueueingConsumer consumer]
  (let [delivery (.nextDelivery consumer)]
    [(deserialize (.getBody delivery))
     #(.basicAck channel (.. delivery getEnvelope getDeliveryTag) false)]))

(defn consumer-for [^Channel channel exchange-name exchange-type queue-name routing-key]
  (let [consumer (QueueingConsumer. channel)]
    (.exchangeDeclare channel exchange-name exchange-type)
    (.queueDeclare channel queue-name false false false nil)
    (.queueBind channel queue-name exchange-name routing-key)
    (.basicConsume channel queue-name consumer)
    consumer))

(defn random-queue []
  (str (java.util.UUID/randomUUID)))

(defn next-message-from
  ([queue-name]
     (next-message-from DEFAULT-EXCHANGE-NAME DEFAULT-EXCHANGE-TYPE queue-name queue-name))
  ([exchange-name exchange-type routing-key]
     (next-message-from exchange-name exchange-type (random-queue) routing-key))
  ([exchange-name exchange-type queue-name routing-key]
     (conn/ensure-thread-local-connection)
     (with-open [channel (channel/create-channel)]
       (let [consumer (consumer-for channel exchange-name exchange-type queue-name routing-key)]
         (delivery-from channel consumer)))))

(declare guaranteed-delivery-from)

(defn recover-from-delivery [exchange-name exchange-type queue-name routing-key channel-atom consumer-atom]
  (try 
   (utils/wait-for-seconds (rand-int 7))
   (let [new-channel (channel/create-channel)
         new-consumer (consumer-for new-channel exchange-name exchange-type queue-name routing-key)]
     (reset! channel-atom new-channel)
     (reset! consumer-atom new-consumer)
     #(guaranteed-delivery-from exchange-name exchange-type queue-name routing-key channel-atom consumer-atom))
   (catch Exception e
     #_(log/error {:message "recover-from-delivery: retrying"})
     #_(log/exception e)
     #(recover-from-delivery exchange-name exchange-type queue-name routing-key channel-atom consumer-atom))))

(defn guaranteed-delivery-from [exchange-name exchange-type queue-name routing-key channel-atom consumer-atom]
  (try
   (delivery-from @channel-atom @consumer-atom)
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
        consumer-atom (atom (consumer-for channel exchange-name exchange-type queue-name routing-key))]
    (lazy-message-seq exchange-name exchange-type queue-name routing-key channel-atom consumer-atom)))

(defn start-queue-message-handler 
  ([routing-key handler-fn]
     (start-queue-message-handler DEFAULT-EXCHANGE-NAME DEFAULT-EXCHANGE-TYPE routing-key (random-queue) handler-fn))
  ([queue-name routing-key handler-fn]
     (start-queue-message-handler DEFAULT-EXCHANGE-NAME DEFAULT-EXCHANGE-TYPE routing-key (random-queue) handler-fn))
  ([exchange-name exchange-type routing-key handler-fn]
     (start-queue-message-handler exchange-name exchange-type routing-key (random-queue) handler-fn))
  ([exchange-name exchange-type queue-name routing-key handler-fn]
     (conn/ensure-thread-local-connection)
     (with-open [channel (channel/create-channel)]
       (doseq [[m ack-fn] (message-seq exchange-name exchange-type channel queue-name routing-key)]
         (handler-fn m ack-fn)))))
