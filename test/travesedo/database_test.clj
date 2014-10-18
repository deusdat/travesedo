 (ns travesedo.database_test
   (:require [clojure.test :refer :all]
            [travesedo.database :refer :all]))

(def base-context {:conn {:type :simple :url "http://localhost:8529" :uname "jdavenpo" :password "secret"}})

(deftest find-current-info
  (let [db-context (assoc base-context :db "testing")
         resp (get-database-info db-context)]
    (is (= "testing" (:name (:result resp))))))
