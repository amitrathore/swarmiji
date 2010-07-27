(defproject swarmiji "0.3.1"
  :description "A distributed computing framework to help write and run Clojure code in parallel, across cores and processors"
  :url "http://github.com/amitrathore/swarmiji"
  :dependencies [[org.clojure/clojure "1.1.0"]
                 [org.clojure/clojure-contrib "1.1.0"]
                 [org.danlarkin/clojure-json "1.1"]
                 [org.clojars.kjw/commons-io "1.4"]
                 [mysql/mysql-connector-java "5.1.6"]
                 [rabbitmq-client "1.7.0"]
                 [org.clojars.sethtrain/postal "0.2.0"]
                 [org.clojars.runa/clj-utils "0.1.3"]
                 [org.clojars.runa/medusa "0.1.3"]
                 [org.clojars.macourtney/clj-record "1.0.1"]
                 [org.clojars.amit/swarmiji-java "0.2.0"]]
  :dev-dependencies [[swank-clojure "1.2.1"]])

