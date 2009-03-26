(ns org.runa.swarmiji.master.web-server)

(import '(com.sun.grizzly.http SelectorThread))
(import '(com.sun.grizzly.tcp Adapter OutputBuffer Request Response))
(import '(com.sun.grizzly.util.buf ByteChunk))
(import '(java.net HttpURLConnection))

(defn is-get? [request]
  (= (.toUpperCase (.method request)) "GET"))

(defn response-as-chunk [grizzly-response response-text]
  (let [response-bytes (.getBytes response-text)
	response-length (count response-bytes)
	out-chunk (ByteChunk.)]
    (.setStatus grizzly-response HttpURLConnection/HTTP_OK)
    (.setContentLength grizzly-response response-length)
    (.setContentType grizzly-response "text/plain")
    (.append out-chunk response-bytes 0 response-length)
    out-chunk))

(defn service-http-request [request response]
  (do
    (println "Recieved request from" (.requestURI request))
    (if (is-get? request)
      (let [out-buffer (.getOutputBuffer response)
	    response-chunk (response-as-chunk response "Hello World!")]
	(.doWrite out-buffer response-chunk response)
	(.finish response)))))
	
(defn after-service [request response]
  (do
    (.recycle request)
    (.recycle response)))

(defn http-request-handler []
  (proxy [Adapter] []
    (service [req res]
      (service-http-request req res))
    (afterService [req res]
      (after-service req res))))
	     