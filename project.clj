(defproject swarmiji "0.4.1"
  :description "A distributed computing framework to help write and run Clojure code in parallel, across cores and processors"
  :url "http://github.com/runa-dev/swarmiji"
  :dependencies [[org.clojure/clojure            "1.4.0"]
                 [org.danlarkin/clojure-json     "1.1"]
                 [org.clojars.kjw/commons-io     "1.4"]
                 [mysql/mysql-connector-java     "5.1.6"]
                 [com.rabbitmq/amqp-client       "2.5.0"]
                 [org.clojars.sethtrain/postal   "0.2.0"]
                 [clj-utils                      "1.2.2"]
                 [medusa                         "0.1.7"]
                 [clj-record                     "1.1.4"]
                 [org.clojars.amit/swarmiji-java "0.2.0"]]

  :dev-dependencies [[slamhound                "1.3.1"]
                     [swank-clojure            "1.2.1"]]

  :repositories {"releases" "s3p://runa-maven/releases/"}

  :plugins [[lein-swank "1.4.4"]
            [s3-wagon-private "1.1.1"]])
      
