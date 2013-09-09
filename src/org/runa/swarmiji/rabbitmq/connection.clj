(ns org.runa.swarmiji.rabbitmq.connection
  (:require [kits.structured-logging :as log])
  (:import (com.rabbitmq.client AlreadyClosedException
                                Connection
                                ConnectionFactory)))

(def connection-factory (atom nil))

(def ^ThreadLocal connection-local (ThreadLocal.))

(defn clear-thread-local-conn []
  (.set connection-local nil))

(defn init-connection-factory [host username password]
  (reset! connection-factory (doto (ConnectionFactory.)
                               (.setVirtualHost "/")
                               (.setUsername username)
                               (.setPassword password)
                               (.setHost host))))

(defn ^Connection new-connection []
  (.newConnection ^ConnectionFactory @connection-factory))

(defn reset-thread-local-connection []
  (let [new-conn (new-connection)] 
     (.set connection-local new-conn)
     new-conn))

(defn ^Connection get-or-create-thread-local-connection []
  (or (.get connection-local)
      (reset-thread-local-connection)))

(defn close [^Connection c]
  (try
    (.close c)
    (catch Exception e
      #_(log/exception e)
      #_(log/error {:message "Ignoring problem when closing connection."}))))

;; TODO: use this in SOMEWHERE ...
(defn connection-valid? [^Connection c]
  (try
    (.ensureIsOpen c)
    true
    (catch AlreadyClosedException _
      false)))
