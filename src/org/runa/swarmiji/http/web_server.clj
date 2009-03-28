(ns org.runa.swarmiji.http.web-server)

(import '(com.sun.grizzly.http SelectorThread))
(import '(com.sun.grizzly.tcp.http11 GrizzlyAdapter))
(import '(com.sun.grizzly.util.buf ByteChunk))
(import '(java.net HttpURLConnection))

(defn is-get? [request]
  (= (.toUpperCase (str (.getMethod request))) "GET"))

(defn requested-route-from [request]
  (str (.getRequestURI request)))

;(defn response-as-chunk [grizzly-response response-text]
;  (let [response-bytes (.getBytes response-text)
;	response-length (count response-bytes)
;	out-chunk (ByteChunk.)]
;    (.setStatus grizzly-response HttpURLConnection/HTTP_OK)
;    (.setContentLength grizzly-response response-length)
;    (.setContentType grizzly-response "text/plain")
;    (.append out-chunk response-bytes 0 response-length)
;    out-chunk))

;(defn service-http-request [handler-functions-as-route-map request response]
;  (if (is-get? request)
;    (let [request-route (requested-route-from request)
;	  route-handler (handler-functions-as-route-map request-route)
;	  _ (println "Recieved request from" request-route)
;	  _ (println "Request attributes:" (.getAttributes request))
;	  out-buffer (.getOutputBuffer response)
;	  response-text (route-handler request)
;	  response-chunk (response-as-chunk response response-text)]
;      (.doWrite out-buffer response-chunk response)
;      (.finish response))))

;(defn after-service [request response]
;  (do
;    (.recycle request)
;    (.recycle response)))

(defn params-for-dispatch [parameter-map]
  (map first (.values parameter-map)))

(defn service-http-request [handler-functions-as-route-map request response]
  (if (is-get? request)
    (let [request-route (requested-route-from request)
	  route-handler (handler-functions-as-route-map request-route)
	  params (params-for-dispatch (.getParameterMap request))
	  _ (println "Recieved request from" request-route)
	  _ (println "Request attributes:" params)
	  response-text (apply route-handler params)]
      (.println (.getWriter response) response-text))))

(defn grizzly-adapter-for [handler-functions-as-route-map]
  (proxy [GrizzlyAdapter] []
    (service [req res]
      (service-http-request handler-functions-as-route-map req res))))

;    (afterService [req res]
;      (after-service req res))))
	     