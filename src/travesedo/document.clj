(ns travesedo.document
  (:require [travesedo.common :refer :all]))

(defn- get-document-root
  [{db-name :db-name}]
  (str "/_db/" db-name "/_api/document/"))

(defn create-document!
  "Creates a document. Put the document in :document of the config"
  [{document :document collection :collection :as config}]
  (let [query-params {:query-params {:collection collection} :body document}
        query-config (merge query-params config)
        resource (get-document-root config)]
    (with-req query-config :resource resource :method :post)))

(defn read-document
  "Gets a document based on it's _id. Specify the database with :db-name.
  :id holds the document _id value. Ids normally look like collection/_key."
  [{id :id :as config}]
    (let [resource (str (get-document-root config) id)]
     (with-req config :resource resource :method :get)))
