(ns travesedo.graph-test
  (:require [clojure.test :refer :all]
            [travesedo.graph :as g]))
(deftest check-base-graph
  (testing "Proper creation of edge path"
    (is (= "/_db/test/_api/gharial/social/edge/knows"
           (g/create-edge-path {:db "test" :graph "social" :collection "knows"})))))
