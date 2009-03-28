(ns org.runa.swarmiji.http.utils)

(import '(com.sun.grizzly.http.embed GrizzlyWebServer))
(require '(org.runa.swarmiji.http [web-server :as web-server]))


(comment defn start-web-server [handler-functions-as-route-map port]
  (let [st (SelectorThread.)]
    (.setAdapter st (web-server/http-request-handler handler-functions-as-route-map))
    (.setPort st port)
    (.initEndpoint st)
    (.startEndpoint st)))


(defn start-web-server [handler-functions-as-route-map port]
  (let [gws (GrizzlyWebServer. port)]
    (.addGrizzlyAdapter gws (web-server/grizzly-adapter-for handler-functions-as-route-map))
    (.start gws)))

