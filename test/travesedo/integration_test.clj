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
  (map char (concat  (range 66 91) ; A-Z
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
  (testing "Gets all of the documents for a collection defined in the collection"
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
