(ns swarmiji-spec)

(use 'clojure.contrib.test-is)
(require '(org.runa.swarmiji.http [web-server :as ws]))

(deftest test-route-recognition
  (let [handlers {"/reports/funnel" #() "/tests/merchants" #() "/random" #()}
	route1 (ws/requested-route-from "/reports/funnel/3/1/2" handlers)
	route2 (ws/requested-route-from "/tests/merchants/chairs" handlers)
	route3 (ws/requested-route-from "/random/1232231" handlers)
	route4 (ws/requested-route-from "/reports/funnel/3/1/2/jsonp=blah" handlers)
	route5 (ws/requested-route-from "/tests/merchants/chairs/jsonp=blah" handlers)
	route6 (ws/requested-route-from "/random/1232231/jsonp=blah" handlers)]
    (is (= "/reports/funnel" route1))
    (is (= "/tests/merchants" route2))
    (is (= "/random" route3))
    (is (= "/reports/funnel" route4))
    (is (= "/tests/merchants" route5))
    (is (= "/random" route6))))

(deftest test-callback-fname
  (is (= (ws/callback-fname "/reports/funnel/3/1/2/jsonp=blah") "blah")))

(deftest test-params-recognition
  (is (= (ws/params-for-dispatch "/reports/funnel/3/1/2" "/reports/funnel") ["3" "1" "2"]))
  (is (= (ws/params-for-dispatch "/tests/merchants/chairs" "/tests/merchants") ["chairs"]))
  (is (= (ws/params-for-dispatch "/random/1232231" "/random") ["1232231"])))

(deftest test-params-recognition-for-jsonp
  (is (= (ws/params-for-dispatch "/reports/funnel/3/1/2/jsonp=blah" "/reports/funnel") ["3" "1" "2"]))
  (is (= (ws/params-for-dispatch "/tests/merchants/chairs/jsonp=blah" "/tests/merchants") ["chairs"]))
  (is (= (ws/params-for-dispatch "/random/1232231/jsonp=blah" "/random") ["1232231"])))