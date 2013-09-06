(ns org.runa.swarmiji.rabbitmq.channel
  (:require [kits.structured-logging :as log]
            [org.runa.swarmiji.rabbitmq.connection :as conn]
            [org.runa.swarmiji.utils.general-utils :as utils]
            [taoensso.nippy :as nippy])
  (:import (com.rabbitmq.client Channel QueueingConsumer)))


(def ^:dynamic *PREFETCH-COUNT* 1)
(def ^:const DEFAULT-EXCHANGE-NAME "default-exchange")
(def ^:const DEFAULT-EXCHANGE-TYPE "direct")
(def ^:const FANOUT-EXCHANGE-TYPE "fanout")

(defmacro with-prefetch-count [prefetch-count & body]
  `(binding [*PREFETCH-COUNT* ~prefetch-count]
     ~@body))

(defn- create-channel-guaranteed []
  (let [c (conn/ensure-thread-local-connection)]
    ;; is outside try, so rabbit-down-exception bubbles up
    (try 
      (let [^Channel ch (.createChannel c)]
        (.basicQos ch *PREFETCH-COUNT*)
        ch)
      (catch Exception e
        #_(log/error {:message "error creating channel, retrying"})
        #_(log/exception e)
        (conn/close c)
        (conn/clear-thread-local-conn)
        (utils/wait-for-seconds (rand-int 2))
        #(create-channel-guaranteed)))))

(defn ^Channel create-channel []
  (trampoline create-channel-guaranteed))

(defn close-channel [^Channel chan]
  (.close chan))

(defn delete-queue [q-name]
  (with-open [^Channel chan (create-channel)]
    (.queueDelete chan q-name)))

(defn- serialize [x]
  (nippy/freeze x))

(defn- deserialize [^"[Ljava.lang.Byte;" message-body]
  (nippy/thaw message-body))

(defn send-message [exchange-name exchange-type routing-key message-object]
  (conn/ensure-thread-local-connection)
  (with-open [channel (create-channel)]
    (.exchangeDeclare channel exchange-name exchange-type)
    (.queueDeclare channel routing-key false false false nil)
    (.basicPublish channel exchange-name routing-key nil (serialize message-object))))

(defn send-message-if-queue [routing-key message-object]
  (conn/ensure-thread-local-connection)
  (with-open [channel (create-channel)]
    (.basicPublish channel DEFAULT-EXCHANGE-NAME routing-key nil (serialize message-object))))

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
