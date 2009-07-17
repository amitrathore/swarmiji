(ns org.runa.swarmiji.mpi.rabbitmq
  (:import (com.rabbitmq.client Channel Connection ConnectionFactory
                                QueueingConsumer
                                RpcClient StringRpcServer))
  (:use (org.runa.swarmiji.config.system-config)))

(def host-name "arryn.local")
(def queue-name "SimpleQueue")
(def rpc-queue-name "Hello")

(defonce connection (.newConnection (ConnectionFactory.) host-name ))

(defmacro with-channel
  "Execute body with a private channel bound to local var"
  [[var] & body]
  `(with-open [~var (.createChannel connection)]
     ~@body))


(defn runbg [fun]
  (send-off (agent nil) (fn [_] (fun))))



;;;; Send string to queue

(defn send-string
  ([message]
     (send-string message {}))
  ([message {exchange :exchange, routing-key :routing,
             :or {exchange "", routing-key queue-name}}]
     (with-channel [ch]
       (when (= exchange "")
         (.queueDeclare ch routing-key))     
       (.basicPublish ch exchange routing-key nil (.getBytes message)))))



;;;; AMPQ Queue as a sequence

;; Helper function
(defn delivery-seq [ch q]
  (lazy-seq
    (let [d (.nextDelivery q)
          m (String. (.getBody d))]
      (.basicAck ch (.. d getEnvelope getDeliveryTag) false)
      (cons m (delivery-seq ch q)))))

(defn queue-seq [conn queue-name]
  "Reutrn a sequence of the messages in queue with name queue-name"
  (let [ch (.createChannel conn)]
    (.queueDeclare ch queue-name)
    (let [consumer (QueueingConsumer. ch)]
      (.basicConsume ch queue-name consumer)
      (delivery-seq ch consumer))))

;;(def s (queue-seq connection queue-name))


;;;; Calling a RPC service usning raw string

(defn call-hello [s]
  (with-channel [channel]
    (let [service (RpcClient. channel "" rpc-queue-name)]
      (.stringCall service s))))



;;;; Implementation of RPC service for plain string proto

(defn hello-handler [this request]
  (println "Got request:" request)
  (when (= request "stop")
    (.terminateMainloop this))
  (str "Hello, " request "!"))

(defn hello-server []
  (with-channel [channel]
    (.queueDeclare channel rpc-queue-name)
    (with-open [server
                (proxy [StringRpcServer] [channel rpc-queue-name]
                  (handleStringCall
                   ([request] "1")
                   ([request props] (hello-handler this request))))]
      (.mainloop server))))

