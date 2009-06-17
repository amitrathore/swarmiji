(ns org.runa.swarmiji.http.web-server-2)

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

(defn singularize-values [a-map]
  (if (empty? a-map)
    {}
    (let [kv (fn [e]
	       {(first e) (aget (last e) 0)})]
      (apply merge (map kv a-map)))))

(defn is-get? [request]
  (= (.toUpperCase (str (.getMethod request))) "GET"))

(defn params-map-from [request]
  (let [p-map (into {} (.getParameterMap request))]
    (singularize-values p-map)))

(defn is-jsonp? [request]
  ((params-map-from request) "jsonp"))

(defn jsonp-callback [request]
  ((params-map-from request) "jsonp"))

(defn only-jsonp-param? [params-map]
  (and (= 1 (count params-map))
       (= "jsonp" (first (keys params-map)))))

(defn is-restful? [request]
  (let [params-map (params-map-from request)]
    (or (empty? params-map) 
	(only-jsonp-param? params-map))))

(defn route-for [request handlers]
  (let [registered (keys handlers)
	uri-string (.getRequestURI request)]
    (first (filter #(.startsWith uri-string %) registered))))

(defn handler-for [request handlers]
  (handlers (route-for request handlers)))

(defn parsed-params-from-uri [request handlers]
  (let [uri-string (.getRequestURI request)
	requested-route (route-for request handlers)
	params-string (.substring uri-string (count requested-route))]
    (rest (.split params-string "/"))))

(defn params-for [request handlers]
  (if (is-restful? request)
    (parsed-params-from-uri request handlers)
    (params-map-from request)))
    
(defn response-from [handler params is-restful]
  (if is-restful
    (apply handler params)
    (handler params)))

(defn prepare-response [response-text request]
  (if (is-jsonp? request)
    (str (jsonp-callback request)  "(" (json/encode-to-str response-text) ")")
    response-text))
    
(defn service-http-request [handler-functions request response]
  (if (is-get? request)
    (let [requested-route (route-for request handler-functions)
	  handler (handler-for request handler-functions)]
      (if handler
	(let [params (params-for request handler-functions)
	      is-restful (is-restful? request)
	      response-text (response-from handler params is-restful)]
	  (log-message "Recieved request for (" requested-route params ")")
	  (.println (.getWriter response) (prepare-response response-text request)))
	(log-message "Unable to respond to" requested-route)))))

(defn grizzly-adapter-for [handler-functions-as-route-map]
  (proxy [GrizzlyAdapter] []
    (service [req res]
      (with-swarmiji-bindings 
        (service-http-request handler-functions-as-route-map req res)))))

(defn boot-web-server [handler-functions-as-route-map port]
  (let [gws (GrizzlyWebServer. port)]
    (.addGrizzlyAdapter gws (grizzly-adapter-for handler-functions-as-route-map))
    (log-message "web-server-2: Using config:" (operation-config))
    (log-message "Started swarmiji-http-gateway on port" port)
    (.start gws)))