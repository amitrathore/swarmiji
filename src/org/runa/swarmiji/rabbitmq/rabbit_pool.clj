(ns org.runa.swarmiji.rabbitmq.rabbit-pool
  (:import (com.rabbitmq.client AlreadyClosedException
                                ConnectionFactory)
           (com.rabbitmq.client.impl AMQConnection)
           (org.apache.commons.pool BasePoolableObjectFactory)
           (org.apache.commons.pool.impl GenericObjectPool)))

(def pool (atom nil))
(def max-pool-size (atom 10))

(declare connection-valid?)

(defn new-rabbit-connection [host username password]
  (.newConnection
   (doto (ConnectionFactory.)
     (.setVirtualHost "/")
     (.setUsername username)
     (.setPassword password)
     (.setHost host))))

(defn connection-valid? [c]
  (try
   (.ensureIsOpen c)
   true
   (catch AlreadyClosedException ace
     false)))

(defn connection-factory [host username password]
  (proxy [BasePoolableObjectFactory] []
    (makeObject []
      (new-rabbit-connection host username password))
    (validateObject [c]
      (connection-valid? c))
    (destroyObject [c]
      (try
       (.close ^AMQConnection c)
       (catch Exception e)))))

(defn init-pool [host username password max-pool max-idle]
  (reset! max-pool-size max-pool)
  (reset! pool (doto (GenericObjectPool. (connection-factory host username password))
                 (.setMaxActive max-pool)
                 (.setLifo false)
                 (.setWhenExhaustedAction GenericObjectPool/WHEN_EXHAUSTED_BLOCK)
                 (.setMaxIdle max-idle)
                 (.setTestWhileIdle true))))

(defn pool-status []
  [(.getNumActive @pool) (.getNumIdle @pool) @max-pool-size])

(defn get-connection-from-pool []
  (.borrowObject ^GenericObjectPool @pool))

(defn return-connection-to-pool [c]
  (.returnObject ^GenericObjectPool @pool c))

(defn invalidate-connection [c]
  (.invalidateObject @pool c))
