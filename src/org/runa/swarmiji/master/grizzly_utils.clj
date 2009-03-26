(ns org.runa.swarmiji.master.grizzly-utils)

(import '(com.sun.grizzly.http SelectorThread))

(defn start-web-server [http-request-handler port]
  (let [st (SelectorThread.)]
    (.setAdapter st http-request-handler)
    (.setPort st port)
    (.initEndpoint st)
    (.startEndpoint st)))
