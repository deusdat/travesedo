 (ns travesedo.common_test
   (:require [clojure.test :refer :all]
            [travesedo.common :refer :all]))

 (deftest db-resource
   (is (= (calc-api-base {:db "mydb"}) "/_db/mydb/_api" )))

  (deftest db-resource-building
   (is (= (calc-api-base {:db "mydb"}) "/_db/mydb/_api" )))

 (deftest find-connection-test
   (is (= (find-connection {:conn {:type :simple :url "http://localhost:8529" :uname "example_user" :password "secret"}})
          {:url "http://localhost:8529" :uname "example_user" :password "secret"}))
   (is (= (find-connection {:conn {:type :replica :method :post :url "http://localhost:8529" :uname "example_user" :password "secret"}})
          {:url "http://localhost:8529" :uname "example_user" :password "secret"}))
   (is (= (find-connection {:conn {:type :replica
                                                     :method :get
                                                     :url "http://localhost:8529"
                                                     :uname "example_user"
                                                     :password "secret"
                                                     :replicas [{:url "http://127.0.0.1:8181"}]}})
          {:url "http://127.0.0.1:8181" :uname "example_user" :password "secret"}) "Should inherit the credentials, but use a new url.")
      (is (= (find-connection {:conn {:type :replica
                                                     :method :get
                                      :url "http://localhost:8529"
                                                     :uname "example_user"
                                                     :password "secret"
                                                     :replicas [{:url "http://127.0.0.1:8181" :uname "bob" :password "builder"}]}})
          {:url "http://127.0.0.1:8181" :uname "bob" :password "builder"}) "Should use replica credentials")
   (is (= (find-connection {:conn {:type :shard
                                                     :method :get
                                                     :uname "example_user"
                                                     :password "secret"
                                                     :coordinators [{:url "http://127.0.0.1:8181"}]}})
          {:url "http://127.0.0.1:8181" :uname "example_user" :password "secret"}) "Should inherit the credentials, but use a new url.")
      (is (= (find-connection {:conn {:type :shard
                                                     :method :get
                                                     :uname "example_user"
                                                     :password "secret"
                                                     :coordinators [{:url "http://coord1:8181" :uname "bob" :password "builder"}]}})
          {:url "http://coord1:8181" :uname "bob" :password "builder"}) "Should use replica credentials"))

 (deftest calling-arango
   (let [resp (call-arango :get  "/_db/testing/_api/database/current" {:wait-for-sync :true :conn {:type :simple :url "http://localhost:8529" :uname "example_user" :password "secret"}})]
     (print resp)
     (is (= "testing" (:name (:result resp))))))

