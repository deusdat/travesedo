(ns travesedo.document
  (:require [travesedo.common :refer :all]
            [clojure.string :as cstr]))

(defn- get-document-root
  [{db-name :db-name}]
  (str "/_db/" db-name "/_api/document/"))

(defn create-document!
  "Creates a document. Put the document in :document of the config. Specify 
  the collection in :collection."
  [{document :document collection :collection :as config}]
  (let [query-params {:query-params {:collection collection} :body document}
        query-config (merge query-params config)
        resource (get-document-root config)]
    (with-req query-config :resource resource :method :post)))

(defn read-document
  "Gets a document based on it's _id. Specify the database with :db-name.
  :id holds the document _id value. Ids normally look like collection/_key.
  
  :result in the returned map holds the document from the server."
  [{id :id :as config}]
    (let [cleaned-id (cstr/replace id #"^/_api/document/" "") ;; in case id is overly qualified.
          resource (str (get-document-root config) cleaned-id)]
     (with-req config :resource resource :method :get)))

(defn modify-document!
  [{id :id doc :document :as config} method]
  (let [resource  (str (get-document-root config) id)]
    (with-req config :resource resource :method method :body doc)))

(defn replace-document!
  [config]
  (modify-document! config :put))

(defn patch-document!
  [config]
  (modify-document! config :patch))

(defn delete-document!
  [config]
  (modify-document! config :delete))

(defn read-all-documents
  [{collection :collection :as config}]
  (with-req config :resource (get-document-root config) 
    :method :get 
    :query-params {:collection collection}))
