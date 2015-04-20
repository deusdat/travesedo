(ns travesedo.integration-test
  (:require [clojure.test :refer :all]
            [travesedo.core-test :refer :all]
            [clojure.pprint :as cpp]
            [travesedo.database :as db]
            [travesedo.collection :as col]
            [travesedo.query :as q]
            [travesedo.document :as doc]))

;; This is really an integration test that runs heavily over the
;; query API, but leverages the collections/documents too.

;; Rand generator from
;; http://blog.raphinou.com/2009/03/generate-random-string-in-clojure.html



(defn setup-coll-fixture [f]
  "Setups the database for the tests"
  (col/create (assoc ctx :payload {:name "people"}))
  (doc/create (assoc ctx
                :in-collection "people"
                :payload {:name "JPatrick Davenport" :age 32}))
  (doc/create (assoc ctx
                :in-collection "people"
                :payload {:name "Amber Davenport" :age 31})  )
  (f))


(use-fixtures :once (compose-fixtures 
                      setup-database-fixure
                      setup-coll-fixture))

(defn no-error [resp]
  (is (false? (:error resp))))

(defn contains-n-docs [resp n]
  (is (= n (count (:result resp)))))

(defn amber? [person]
  (is (= "Amber Davenport" (:name person))))

(defn patrick? [person]
  (is (= "JPatrick Davenport" (:name person))))

(defn no-more [resp]
  (is (false? (:has-more resp))))

(deftest aql-query-batch-sizes
  (testing "Against non-existant collection"
    (let [aql  "FOR p IN nonexistant-collection RETURN p"
          batched (q/aql-query (assoc ctx :payload {:query aql :batch-size 1}))]
      (is (nil? (:has-more batched))) "There shouldn't be a data indicator."
      (is (= 404 (:code batched)) "Server should say not found.")
      (is (:error batched))) "Should map the error properly.")
  
  (testing "Query with more results."
    (let [aql "FOR p IN people RETURN p"
          batched (q/aql-query (assoc ctx :payload {:query aql :batch-size 1}))]
      (no-error batched)
      (is (true? (:has-more batched)))
      (contains-n-docs batched 1)
      (testing "Delete a cursor"
        (let [ cursor (select-keys batched [:cursor-id])
              deleted-cursor (q/delete (conj ctx cursor))]
          (is (nil? (:has-more deleted-cursor)))
          (no-error deleted-cursor)
          (is (= 202 (:code deleted-cursor)))))))
  
  (testing "Query all documents AQL"
    (let [aql "FOR p IN people RETURN p"
          payload { :payload {:query aql :batch-size 1}}
          batched (q/aql-query-all (conj ctx payload))]
      (contains-n-docs batched 2)
      (no-error batched)
      (no-more batched))))

(deftest parse-aql-tests
  (testing "Parsing a valid AQL."
    (let [aql "FOR p IN people RETURN p"
          resp (q/parse-aql-query (assoc ctx :payload {:query aql}))]
      (is (= 200 (:code resp)))
      )))

(deftest collection-manipulation
  (testing "Checking details out for a collection"
    (let [stats (col/get-collection-info (assoc ctx :collection "people"))]
      (is (= { :code 200, :error false, :type 2, :status 3, :is-system false,
              :name "people"} (dissoc stats :id)))))	
  
 	(testing "Should return properties for a collection"
    (let [props (col/get-collection-properties (assoc ctx 
                                                 :collection "people"))
          expected {:journal-size 33554432,
                    :do-compact true,
                    :is-volatile false,
                    :name "people",
                    :is-system false,
                    :type 2,
                    :key-options {:type "traditional", :allowUserKeys true},
                    :status 3,
                    :code 200,
                    :error false,
                    :wait-for-sync false}]
      (is (= expected (dissoc props :id)))))
  
  (testing "Get all the collections for the db excluding system."
    (let [cols (col/get-all-collections (assoc ctx :exclude-system true))
          expected {:code 200,
                    :error false,
                    :names
                    {:people
                     {:name "people",
                      :isSystem false,
                      :status 3,
                      :type 2}},
                    :collections
                    [{:name "people",
                      :isSystem false,
                      :status 3,
                      :type 2}]}
          cols-no-people-id (update-in cols [:names :people] dissoc :id)
          cols-no-collections-id (update-in cols-no-people-id 
                                            [:collections] 
                                            #(conj [] (dissoc (first %) :id)))]
      (is (= expected (update-in cols-no-collections-id [:names :people] dissoc :id)))))
 	
  (testing "Should load the collection with count returned"
    (let [resp (dissoc (col/load-collection (assoc ctx :collection "people")) :id)
          expected {:code 200,
                    :error false,
                    :type 2,
                    :status 3,
                    :count 2,
                    :name "people",
                    :is-system false}]
      (is (= expected resp)))) 	
  
  (testing "Should unload the collection with count returned"
    (let [resp (dissoc (col/unload (assoc ctx :collection "people")) :id)
          expected {:code 200,
                    :error false,
                    :type 2,
                    :status 4,
                    :name "people",
                    :is-system false}]
      (cpp/pprint resp)
      (is (= expected resp)))) 	
 	
 	(testing "Create and delete of collection"
    (let [coll "CheckingManip"
          create-resp (col/create (assoc ctx 
                                    :payload {:name coll}))
          delete-resp (col/delete-collection (assoc ctx :collection coll))]
      (do
        (no-error create-resp)
        (no-error delete-resp))))
  )            

(deftest query-by-example
  (testing "Tries to query for a doc based on name example."
    (let [example {:example {:name "JPatrick Davenport"} :collection "people"}
          new-ctx (assoc ctx :payload example)
          resp (q/by-example new-ctx)
          person (first (:result resp))]
      (patrick? person)
      (contains-n-docs resp 1)
      )))

(deftest return-all-docs
  (testing "Gets all of the documents for a collection defined in the 
           collection"
           (let [payload {:payload {:collection "people"}}
                 new-ctx (conj payload ctx)
                 resp (q/return-all new-ctx)
                 col (sort #(compare (:name %) (:name %2)) (:result resp))
                 p1 (first col)
                 p2 (second col)]
             (no-error resp)
             (contains-n-docs resp 2)
             (amber? p1)
             (patrick? p2)))
  
  (testing "Limiting the return to 1 doc"
    (let [payload {:payload {:collection "people" :limit 1}}
          new-ctx (conj payload ctx)
          resp (q/return-all new-ctx)]
      (contains-n-docs resp 1))))
