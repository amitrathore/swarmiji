(ns org.runa.swarmiji.http.helper)

(import '(com.sun.grizzly.util.http Cookie)
	'(org.apache.turbine.util BrowserDetector))

(def *http-helper* :__init__)

(defn cookie-hash [request]
  (let [cookies (.getCookies request)
	kv (fn [c] {(.getName c) (.getValue c)})]
    (apply merge (map kv cookies))))

(defn browser-detector [request]
  (let [user-agent (.getHeader request "user-agent")]
    (if user-agent
      (BrowserDetector. user-agent))))

(defn http-helper [request response]
  (let [browser (browser-detector request)
	cookies (cookie-hash request)
	add-cookie (fn [name value]
		     (.addCookie response (Cookie. name value)))
	read-cookie (fn [name]
		      (if-not (empty? cookies)
			      (cookies name)))]
    (fn [command & args]
      (cond
	(= command :add-cookie) (apply add-cookie args)
	(= command :read-cookie) (apply read-cookie args)
	(= command :ip-address) (.getRemoteAddr request)
	(= command :browser-name) (if browser (.getBrowserName browser))
	(= command :browser-version) (if browser (.getBrowserVersion browser))
	(= command :operating-system) (if browser (.getBrowserPlatform browser))
	:default (throw (Exception. (str "Response-helper: Unknown command, " command)))))))

(defn read-cookie [name] (*http-helper* :read-cookie name))
(defn set-cookie [name value] (*http-helper* :add-cookie name value))
(defn requester-ip [] (*http-helper* :ip-address))
(defn browser-name [] (*http-helper* :browser-name))
(defn browser-version [] (*http-helper* :browser-version))
(defn operating-system [] (*http-helper* :operating-system))

(defn destructured-hash [attribs]
  (let [d-pair (fn [attrib]
		 (list attrib (.replace (name attrib) "-" "_")))]		 
  (apply hash-map (mapcat d-pair attribs))))

(defmacro defwebmethod [method-name params & exprs]
  `(defn ~method-name [~(destructured-hash params)]
     (do
       ~@exprs)))