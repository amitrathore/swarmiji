(ns org.runa.swarmiji.rabbitmq.channel
  (:require [kits.structured-logging :as log]
            [org.runa.swarmiji.rabbitmq.connection :as conn]
            [org.runa.swarmiji.utils.general-utils :as utils])
  (:import (com.rabbitmq.client Channel)))


(def ^:dynamic *PREFETCH-COUNT* 1)

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
