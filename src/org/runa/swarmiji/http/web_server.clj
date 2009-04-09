(ns org.runa.swarmiji.http.web-server)

(import '(com.sun.grizzly.http SelectorThread))
(import '(com.sun.grizzly.http.embed GrizzlyWebServer))
(import '(com.sun.grizzly.tcp.http11 GrizzlyAdapter))
(import '(com.sun.grizzly.util.buf ByteChunk))
(import '(java.net HttpURLConnection))
(use 'org.runa.swarmiji.utils.general-utils)
(use 'org.runa.swarmiji.utils.logger)
(require '(org.danlarkin [json :as json]))
(use 'org.runa.swarmiji.config.system-config)
(use 'org.runa.swarmiji.sevak.sevak-core)

(def jsonp-marker "/jsonp")

(defn is-get? [request]
  (= (.toUpperCase (str (.getMethod request))) "GET"))

(defn is-jsonp? [uri-string]
  (.startsWith uri-string jsonp-marker))

(defn request-uri-from-jsonp-uri [jsonp-uri-string]
  (.substring jsonp-uri-string (count jsonp-marker)))

(defn requested-route-from [uri-string handler-functions]
  (let [registered (keys handler-functions)
	lookup-handler-from (fn [uri-to-use] (first (filter #(.startsWith uri-to-use %) registered)))]
    (if (is-jsonp? uri-string)
      (lookup-handler-from (request-uri-from-jsonp-uri uri-string))
      (lookup-handler-from uri-string))))    

(defn callback-fname [uri-string]
  (let [callback-token (last (.split uri-string "/"))]
    (last (.split callback-token "="))))

(defn params-string-from [uri-without-jsonp-marker requested-route]
  (.substring uri-without-jsonp-marker (count requested-route)))

(defn params-from-jsonp-uri [uri-string requested-route]
  (let [uri-without-marker (request-uri-from-jsonp-uri uri-string)
	param-string (params-string-from uri-without-marker requested-route)
	tokens (.split param-string "/")]
    (rest (butlast tokens))))

(defn params-from-regular-uri [uri-string requested-route]
  (let [params-string (params-string-from uri-string requested-route)]
    (rest (.split params-string "/"))))

(defn params-for-dispatch [uri-string requested-route]
  (if (is-jsonp? uri-string)
    (params-from-jsonp-uri uri-string requested-route)
    (params-from-regular-uri uri-string requested-route)))

(defn prepare-response [uri-string response-text]
  (if (is-jsonp? uri-string)
    (str (callback-fname uri-string)"(" (json/encode-to-str response-text) ")")
    response-text))

(defn service-http-request [handler-functions request response]
  (if (is-get? request)
    (let [request-uri (.getRequestURI request)
	  request-route (requested-route-from request-uri handler-functions)
	  route-handler (handler-functions request-route)]
      (if route-handler
	(let [params (params-for-dispatch request-uri request-route)
	      _ (log-message "Recieved request for (" request-route params ")")
	      response-text (apply route-handler params)]
	  (.println (.getWriter response) (prepare-response request-uri response-text)))
	(log-message "Unable to respond to" request-uri)))))

(defn grizzly-adapter-for [handler-functions-as-route-map]
  (proxy [GrizzlyAdapter] []
    (service [req res]
      (with-swarmiji-bindings 
        (service-http-request handler-functions-as-route-map req res)))))

(defn boot-web-server [handler-functions-as-route-map port]
  (let [gws (GrizzlyWebServer. port)]
    (.addGrizzlyAdapter gws (grizzly-adapter-for handler-functions-as-route-map))
    (log-message "Using config:" (operation-config))
    (log-message "Started swarmiji-http-gateway on port" port)
    (.start gws)))