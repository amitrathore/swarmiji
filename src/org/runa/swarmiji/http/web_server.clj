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

(def jsonp-marker "jsonp=")

(defn is-get? [request]
  (= (.toUpperCase (str (.getMethod request))) "GET"))

(defn is-jsonp? [uri-string]
  (.contains uri-string jsonp-marker))

(defn is-rest-like-request? [request]
  (empty? (.getParameterMap request)))

(defn request-uri-from-jsonp-uri [jsonp-uri-string]
  (let [end-point (- (.indexOf jsonp-uri-string jsonp-marker) 1)]
    (.substring jsonp-uri-string 0 end-point)))

(defn requested-route-from [uri-string handler-functions]
  (let [registered (keys handler-functions)]
    (first (filter #(.startsWith uri-string %) registered))))

(defn callback-fname [uri-string]
  (let [callback-token (last (.split uri-string "/"))]
    (last (.split callback-token "="))))

(defn params-string-from [uri-string requested-route]
  (let [uri-to-use (if (is-jsonp? uri-string)
		     (request-uri-from-jsonp-uri uri-string)
		     uri-string)]
    (.substring uri-to-use (count requested-route))))

(defn singularize-map [a-map]
  (let [first-val (fn [kv]
		    {(first kv) (first (last kv))})]
    (apply merge (map first-val a-map))))

(defn params-for-mapped-dispatch [uri-string requested-route]
  (let [params-string (params-string-from uri-string requested-route)]
    (rest (.split params-string "/"))))

(defn params-for-dispatch [request requested-route]
  (let [params (into {} (.getParameterMap request))]
    (if (empty? params)
      (params-for-mapped-dispatch (.getRequestURI request) requested-route)
      (singularize-map params))))

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
	(let [params (params-for-dispatch request request-route)
	      _ (log-message "Recieved request for (" request-route params ")")
	      response-text (if (is-rest-like-request? request)
			      (apply route-handler params)
			      (route-handler params))]
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