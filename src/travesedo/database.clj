(ns travesedo.database
  "Handles all of the database api features of ArangoDB."
  (:require [travesedo.common :refer :all]))

(defn get-database-info [ctx]
  "Retrieves information about the server specified by :db value in the ctx.
  Returns a map with the form
  {:result {:name \"testing\",
                 :id \"5417636485\",
                 :path \"/var/lib/arangodb/databases/database-5417636485\",
                 :isSystem false},
     :error false,
     :code 200
     :job-id \"123123\"}"
  (let [db-resource (calc-resource-base ctx)
          current-resource (str db-resource "/database/current")]
    (call-arango :get current-resource ctx)))

(defn list-user-access [ctx]
  (let [db-resource (calc-resource-base ctx)
          current-resource (str db-resource "/database/user")]
    (call-arango :get current-resource ctx)))

(defn list-databases [ctx]
  "Lists the existing databases. If __:db__ to anythin other than _system, an exception will occur.
  Returns map:
  {
  :result [
    \"_system\"
  ],
  :error false,
  :code 200
  }"
  (let [db-resource (calc-resource-base ctx)
          current-resource (str db-resource "/database")]
    (call-arango :get current-resource ctx)))

(defn create-database [ctx]
  "Creates a new database. The :payload key should be a map like so
  {:name \"db-name\"}  or {:name \"db-name\", :users [ {:username \"user\", :password \"password\"}]}"
  (call-arango :post (str api-resource "/database") ctx))

(defn drop-database [ctx]
  "Drops a database and all its contents. Database specified at __:db__"
  (call-arango :delete (str "/_api/database/" (:db ctx)) ctx))
