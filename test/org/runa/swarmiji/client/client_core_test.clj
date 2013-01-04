(ns org.runa.swarmiji.client.client-core-test
  (:use [clojure.test :only [deftest is run-tests]]
        org.runa.swarmiji.mpi.transport
        org.runa.swarmiji.sevak.sevak-core
        org.runa.swarmiji.client.client-core
        org.runa.swarmiji.sevak.bindings))

(def test-atom (atom 0))

(defn incomplete-sevak [s]
  (fn [& args]
    (if (= :complete? (first args))
      false
      (apply s args))))

(defsevak inc-atom [n]
  (swap! test-atom + n))

;; TODO - Alex/Francis - Jan 4, 2012 - This test was resurrected from the specs
;; folder.  It looks like it needs rabbit up to run.  So as currently setup
;; this will always fail.
(deftest test-retry-sevaks
  (reset! test-atom 0)
  (let [s1 (inc-atom 3)
        s2 (inc-atom 5)]
    (is (= @test-atom 8))
    (retry-sevaks 10 inc-atom [s1 s2 (incomplete-sevak s1) (incomplete-sevak s2)])
    (is (= @test-atom 16))))
