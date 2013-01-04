(ns org.runa.swarmiji.monitor.control-message
  (:require clj-record.boot)
  (:use [org.runa.swarmiji.config.system-config]))

(def db (swarmiji-mysql-config))
(clj-record.core/init-model)