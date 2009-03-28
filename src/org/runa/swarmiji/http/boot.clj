(require '(org.runa.swarmiji.http [web-server :as web-server]))
(require '(org.runa.swarmiji.http [grizzly-utils :as grizzly-utils]))

(let [handler (web-server/http-request-handler)]
  (grizzly-utils/start-web-server handler 8020))
      