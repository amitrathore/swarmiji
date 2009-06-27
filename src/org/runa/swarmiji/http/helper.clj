(ns org.runa.swarmiji.http.helper)

(import '(com.sun.grizzly.util.http Cookie))

(def *http-helper* :__init__)

(defn cookie-hash [request]
  (let [cookies (.getCookies request)
	kv (fn [c] {(.getName c) (.getValue c)})]
    (apply merge (map kv cookies))))

(defn http-helper [request response]
  (let [cookies (cookie-hash request)
	add-cookie (fn [name value]
		     (.addCookie response (Cookie. name value)))
	read-cookie (fn [name]
		      (if-not (empty? cookies)
			      (cookies name)))
	]
    (fn [command & args]
      (cond
	(= command :add-cookie) (apply add-cookie args)
	(= command :read-cookie) (apply read-cookie args)
	(= command :ip-address) (.getRemoteAddr request)
	(= command :header-names) (do
				    (println "HEADERS:" (enumeration-seq (.getHeaderNames request)))
				    (println ">> user-agent" (.getHeader request "user-agent")))
	:default (throw (Exception. (str "Response-helper: Unknown command, " command)))))))

(defn read-cookie [name]
  (*http-helper* :read-cookie name))

(defn set-cookie [name value]
  (*http-helper* :add-cookie name value))

(defn requester-ip []
  (*http-helper* :ip-address))

(defn destructured-hash [attribs]
  (let [d-pair (fn [attrib]
		 (list attrib (.replace (name attrib) "-" "_")))]		 
  (apply hash-map (mapcat d-pair attribs))))

(defmacro defwebmethod [method-name params & exprs]
  `(defn ~method-name [~(destructured-hash params)]
     (do
       ~@exprs)))