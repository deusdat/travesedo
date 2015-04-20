(ns travesedo.graph-test
  (:require [clojure.test :refer :all]
            [travesedo.core-test :refer :all]
            [travesedo.graph :as g]
            [travesedo.collection :as col]))

(defn setup-vertexes-fixture [f]
  (col/create (assoc ctx :payload {:name "v1"}))
  (col/create (assoc ctx :payload {:name "v2"}))
  (f))

(use-fixtures :once (compose-fixtures 
                      setup-database-fixure
                      setup-vertexes-fixture))

(deftest check-base-graph
  (testing "Proper creation of edge path"
    (is (= "/_db/test/_api/gharial/social/edge/knows"
           (g/create-edge-path {:db "test" 
                                :graph "social" 
                                :collection "knows"})))))

(deftest graph-lifecycle
  (testing "Create a new graph"
    (let [ctx (assoc ctx :payload 
                {:name "testing-graph",
                 :edge-definitions [{:collection "owns",
                                     :from ["v1",
                                            "v3"],
                                     :to ["v2"]}]})
          res (g/create-graph ctx)]
      (println res)
      (is (= 201 (:code res)) "Should get a 201 response")
      (is (get-in res [:graph :edge-definitions]))
    )))