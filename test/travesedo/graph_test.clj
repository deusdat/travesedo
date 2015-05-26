(ns travesedo.graph-test
  (:require 
    [clojure.test :refer :all]
    [travesedo.document :as doc]
    [travesedo.core-test :refer :all]
    [travesedo.graph :as g]
    [travesedo.collection :as col]))

(def from-col "v1")
(def to-col "v2")
(def graph-name "test-creation")
(def relation "owns")


(defn setup-vertexes-fixture [f]
  (col/create (assoc ctx :payload {:name from-col}))
  (col/create (assoc ctx :payload {:name to-col}))
  
  (def from-v (doc/create (assoc ctx
                            :in-collection from-col
                            :payload {:name "JPatrick Davenport" :age 33})))
  (def to-v (doc/create (assoc ctx
                          :in-collection to-col
                          :payload {:name "Amber Davenport" :age 32})))
  (f))

(use-fixtures :once (compose-fixtures 
                      setup-database-fixture
                      setup-vertexes-fixture))


(deftest check-graph-integration
  (testing "Creates a new graph"
    (is (empty? (:graphs (g/list-graphs ctx))))
    (let [gbody {:name graph-name,
                 :edge-definitions [{:collection relation,
                                     :from [from-col],
                                     :to [to-col]}]
                 :orphan-collections []},
          create-results (g/create-graph! 
                           (assoc ctx :payload gbody))]
      (is (= 201 (:code create-results)))
      (is (= gbody (dissoc (:graph create-results) :_id :_rev)))
      (is (not-empty (:graphs (g/list-graphs ctx))))))
  
  (testing "Inserts an edge between two vertexes"
   (let [edge-payload {:from (:_id from-v), :to (:_id to-v), :arb :attr},
         new-edge (g/create-edge! (merge ctx 
                                    {:payload edge-payload,
                                     :collection relation,
                                     :graph graph-name}))]
     (is (= 201 (:code new-edge)))))
  
  (testing "Fail to insert edge, but to/from"
    (let [edge-payload {:from "123", :to (:_id to-v), :arb :attr},
         new-edge (g/create-edge! (merge ctx 
                                    {:payload edge-payload,
                                     :collection relation,
                                     :graph graph-name}))]
     (is (= 404 (:code new-edge))))))
