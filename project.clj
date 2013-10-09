(defproject org.clojars.runa/swarmiji "0.8.3"
  :description "A distributed computing framework to help write and run Clojure code in parallel, across cores and processors"
  :license  {:name "Eclipse Public License - v 1.0"
             :url "http://www.eclipse.org/legal/epl-v10.html"
             :distribution :repo}
  :url "http://github.com/runa-dev/swarmiji"
  :dependencies [[org.clojure/clojure            "1.5.1"]
                 [mysql/mysql-connector-java     "5.1.6"]
                 [com.rabbitmq/amqp-client       "1.8.0"]
                 [org.clojars.runa/clj-utils     "1.2.8"]
                 [org.clojars.runa/medusa        "0.1.11"]
                 [clj-record                     "1.1.4"]
                 [com.taoensso/nippy             "2.1.0"]
                 [org.clojars.amit/swarmiji-java "0.2.0"]
                 [org.clojars.runa/kits          "1.13.4"]]
  :warn-on-reflection true
  :profiles {:dev {:dependencies
                   [[slamhound                "1.3.1"]
                    [swank-clojure            "1.4.0"]]}}
  :plugins [[lein-swank "1.4.4"]
            [s3-wagon-private "1.1.1"]])

