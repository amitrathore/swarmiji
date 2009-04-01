(ns org.runa.swarmiji.http.web-server)

(import '(com.sun.grizzly.http SelectorThread))
(import '(com.sun.grizzly.http.embed GrizzlyWebServer))
(import '(com.sun.grizzly.tcp.http11 GrizzlyAdapter))
(import '(com.sun.grizzly.util.buf ByteChunk))
(import '(java.net HttpURLConnection))

(defn is-get? [request]
  (= (.toUpperCase (str (.getMethod request))) "GET"))

(defn requested-route-from [uri-string handler-functions]
  (let [registered (keys handler-functions)]
    (first (filter #(.startsWith uri-string %) registered))))

(defn params-for-dispatch [uri-string requested-route]
  (let [params-string (.substring uri-string (count requested-route))]
    (rest (.split (.substring uri-string (count requested-route)) "/"))))

(defn service-http-request [handler-functions request response]
  (if (is-get? request)
    (let [request-uri (.getRequestURI request)
	  request-route (requested-route-from request-uri handler-functions)
	  route-handler (handler-functions request-route)]
      (if route-handler
	(let [params (params-for-dispatch request-uri request-route)
	      _ (println "Recieved request for (" request-route params ")")
	      response-text (apply route-handler params)]
	  (.println (.getWriter response) response-text))
	(println "Unable to respond to" request-uri)))))

(defn grizzly-adapter-for [handler-functions-as-route-map]
  (proxy [GrizzlyAdapter] []
    (service [req res]
      (service-http-request handler-functions-as-route-map req res))))

(defn start-web-server [handler-functions-as-route-map port]
  (let [gws (GrizzlyWebServer. port)]
    (.addGrizzlyAdapter gws (grizzly-adapter-for handler-functions-as-route-map))
    (println "Started swarmiji-http-gateway on port" port)
    (.start gws)))