(ns travesedo.database
  "Handles all of the database api features of ArangoDB."
  (:refer-clojure :exclude [drop])
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
  (let [current-resource (derive-resource ctx "/database/current")]
    (call-arango :get current-resource ctx)))

(defn list-user-access [ctx]
  (let [db-users (derive-resource ctx "/database/user")]
    (call-arango :get db-users ctx)))

(defn list-databases [ctx]
  "Lists the existing databases. If __:db__ to anythin other than _system, an exception will occur.
  Returns map:
  {  :result [\"_system\"],
      :error false,
      :code 200}"
  (let [current-resource (derive-resource ctx "/database")]
    (call-arango :get current-resource ctx)))

(defn create [ctx]
  "Creates a new database. The :payload key should be a map like so
  {:name \"db-name\"}  or
  {:name \"db-name\", :users [ {:username \"user\", :password \"password\"}]}"
  (call-arango :post "/_api/database" ctx))

(defn drop [ctx]
  "Drops a database and all its contents. Database specified at __:db__"
  (call-arango :delete (str "/_api/database/" (:db ctx)) ctx))
