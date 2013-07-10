(ns org.runa.swarmiji.utils.general-utils
  (:require [kits.structured-logging :as log]
            [org.runa.swarmiji.rabbitmq.rabbitmq :refer [*PREFETCH-COUNT*]])
  (:import (java.lang.management ManagementFactory)
           (java.util UUID)))

(defn random-uuid []
  (str (UUID/randomUUID)))

(defn ns-qualified-name [sevak-name-keyword the-name-space]
  (str (ns-name the-name-space) "/" (name sevak-name-keyword)))

(defn sevak-queue-message-no-return [sevak-service args]
  {:sevak-service-name sevak-service
   :sevak-service-args (vec args)})

(defn random-queue-name 
  ([]
     (random-queue-name (str (System/currentTimeMillis) "_")))
  ([prefix]
     (str prefix (random-uuid))))

(defn return-queue-name [sevak-name]
  (log/info {:message "calling return-queue-name"})
  (str (System/currentTimeMillis) "_" sevak-name "_" (random-uuid)))

(defn sevak-queue-message-for-return [sevak-service args]
  (assoc (sevak-queue-message-no-return sevak-service args) :return-queue-name (return-queue-name sevak-service)))

(defn process-pid []
  (let [m-name (.getName (ManagementFactory/getRuntimeMXBean))]
    (first (.split m-name "@"))))

(defmacro with-prefetch-count [prefetch-count & body]
  `(binding [*PREFETCH-COUNT* ~prefetch-count]
     (do ~@body)))
