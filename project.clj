(defproject swarmiji "0.3.8"
  :description "A distributed computing framework to help write and run Clojure code in parallel, across cores and processors"
  :url "http://github.com/runa-dev/swarmiji"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [org.clojure/clojure-contrib  "1.2.0"]
                 [org.danlarkin/clojure-json   "1.1"]
                 [org.clojars.kjw/commons-io   "1.4"]
                 [mysql/mysql-connector-java   "5.1.6"]
                 [com.rabbitmq/amqp-client     "2.5.0"]
                 [org.clojars.sethtrain/postal "0.2.0"]
                 [clj-utils                    "1.1.0"]
                 [medusa                       "0.1.7"]
                 [org.clojars.macourtney/clj-record "1.0.1"]
                 [org.clojars.amit/swarmiji-java "0.2.0"]]
  :dev-dependencies [[swank-clojure            "1.2.1"]]

  :repositories {"releases"     "s3p://runa-maven/releases/"}

  :plugins [[lein-swank "1.4.4"]
            [lein-difftest "1.3.8"]
            [s3-wagon-private "1.1.1"]])
      
