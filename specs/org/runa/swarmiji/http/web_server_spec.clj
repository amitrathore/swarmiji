(ns swarmiji-spec)

(use 'clojure.contrib.test-is)
(require '(org.runa.swarmiji.http [web-server :as ws]))

(deftest test-route-recognition
  (let [handlers {"/reports/funnel" #() "/tests/merchants" #() "/random" #()}
	route1 (ws/requested-route-from "/reports/funnel/3/1/2" handlers)
	route2 (ws/requested-route-from "/tests/merchants/chairs" handlers)
	route3 (ws/requested-route-from "/random/1232231" handlers)]
    (is (= "/reports/funnel" route1))
    (is (= "/tests/merchants" route2))
    (is (= "/random" route3))))

(deftest test-params-recognition
  (is (= (ws/params-for-dispatch "/reports/funnel/3/1/2" "/reports/funnel") ["3" "1" "2"]))
  (is (= (ws/params-for-dispatch "/tests/merchants/chairs" "/tests/merchants") ["chairs"]))
  (is (= (ws/params-for-dispatch "/random/1232231" "/random") ["1232231"])))