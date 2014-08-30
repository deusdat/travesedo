(ns travesedo.database
  (:require [travesedo.common :refer :all]))

(def root-resource "/_api/database")

(defn init-config [] {})

(defn list-databases [config]
  (with-req config :method :get :resource root-resource))

(defn delete-database!
  "Deletes a database by name.
  :db-name is required as a top level key."
  [{db-name :db-name :as config}]
  {:pre [db-name]}
  (with-req config :resource (str root-resource "/" db-name) :method :delete))

(defn create-database!
  "Creates a database with the name specified in config :db-name.
  To create a database with users as well include a list of users 
  at :users

  Example config for creating a database with one user
  {:conns [{:server-url \"http://localhost:8529\"}] 
  :db-name \"mydb\"
  :users [{:username \"jdavenpo\" 
           :password \"testing\" 
           :extra {:likes :cats}}]}"
  [{db-name :db-name users :users :as config}]
  (with-req config 
    :resource root-resource 
    :method :post 
    :body (merge {:name db-name} (if users {:users users}))))
