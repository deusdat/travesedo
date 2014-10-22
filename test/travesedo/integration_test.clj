(ns travesedo.integration-test
  (:require [clojure.test :refer :all]
                [travesedo.database :as db]
                [travesedo.collection :as col]
                [travesedo.query :as q]
                [travesedo.document :as doc]))

;; This is really an integration test that runs heavily over the
;; query API, but leverages the collections/documents too.

;; Rand generator from
;; http://blog.raphinou.com/2009/03/generate-random-string-in-clojure.html

(def VALID-CHARS
  (map char (concat (range 48 58) ; 0-9
    (range 66 91) ; A-Z
    (range 97 123)))) ; a-z

(defn random-char []
  (rand-nth VALID-CHARS))

(defn random-str [length]
  (apply str (take length (repeatedly random-char))))

;; Connection
(def ctx {:conn {:type :simple, :url "http://localhost:8529"}
               :db  (random-str 15), :wait-for-sync :true} ;; wait to avoid write misses.
               )

(defn setup-fixure [f]
  "Setups the database for the tests"
  (is (= false (:error (db/create (assoc ctx :payload {:name (:db ctx)})) )))
  (col/create (assoc ctx :payload {:name "people"}))
  (doc/create (assoc ctx
                :in-collection "people"
                :payload {:name "JPatrick Davenport" :age 32}))
  (doc/create (assoc ctx
                :in-collection "people"
                :payload {:name "Amber Davenport" :age 31})  )
  (try (f)
    (finally
    (db/drop ctx))))


(use-fixtures :once setup-fixure)

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
      (is (false? (:error batched)))
      (is (true? (:has-more batched)))
      (is (= 1 (count (:result batched))))
      (testing "Delete a cursor"
        (let [ cursor (select-keys batched [:cursor-id])
              deleted-cursor (q/delete (conj ctx cursor))]
          (is (nil? (:has-more deleted-cursor)))
          (is (false? (:error deleted-cursor)))
          (is (= 202 (:code deleted-cursor)))))))
  (testing "Query all documents AQL"
    (let [aql "FOR p IN people RETURN p"
           payload { :payload {:query aql :batch-size 1}}
           batched (q/aql-query-all (conj ctx payload))]
      (is (= 2 (:count batched)))
      (is (false? (:error batched)))
      )))

(deftest parse-aql-tests
  (testing "Parsing a valid AQL."
    (let [aql "FOR p IN people RETURN p"
          resp (q/parse-aql-query (assoc ctx :payload {:query aql}))]
      (is (= 200 (:code resp)))
      )))

(deftest query-by-example
  (testing "Tries to query for a doc based on name example."
    (let [example {:example {:name "JPatrick Davenport"} :collection "people"}
          new-ctx (assoc ctx :payload example)
          resp (q/by-example new-ctx)
          person (first (:result resp))]

      (is (= 1 (count (:result resp))))
      (is (= "JPatrick Davenport" (:name person ))))))
