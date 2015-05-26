(ns travesedo.core-test
  (:require [clojure.test :refer :all]
            [travesedo.database :as db]))


(def VALID-CHARS
  (map char (concat  (range 66 91) ; A-Z
                    (range 97 123)))) ; a-z

(defn random-char []
  (rand-nth VALID-CHARS))

(defn random-str [length]
  (apply str (take length (repeatedly random-char))))

(def ctx {:conn {:type :simple, 
                 :url "http://arangodb:8529"},
          :db  (random-str 15), 
          :wait-for-sync true})

(defn no-error [resp]
  (is (false? (:error resp))))

(defn setup-database-fixture
  "Setups the database for the tests"
  [f]
  (println "Attempting to create the datbase")
  (println "DB name is " (:db ctx))
  (is (= false (:error (db/create (assoc ctx :payload {:name (:db ctx)})) )))
  (try (f)
    (finally
      (db/drop ctx))))