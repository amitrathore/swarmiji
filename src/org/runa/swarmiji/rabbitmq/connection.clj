(ns org.runa.swarmiji.rabbitmq.connection
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

(defn ^Connection ensure-thread-local-connection []
  (or (.get connection-local)
      (reset-thread-local-connection)))

(defn close [^Connection c]
  (.close c))

;; TODO: use this in rabbitmq.clj ...
(defn connection-valid? [^Connection c]
  (try
    (.ensureIsOpen c)
    true
    (catch AlreadyClosedException _
      false)))
