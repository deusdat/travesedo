(ns travesedo.collection
  (:require [travesedo.common :refer :all]))

(defn get-collection-root
  [{db-name :db-name}]
    (str "/_db/" db-name "/_api/collection/"))

(defn get-collection-name 
  [{coll :collection}]
  (if (map? coll) (:name coll) coll))

(defn get-named-resource 
  [{db-name :db-name coll :collection :as config} & extra]
  (let [coll-name (get-collection-name config)]
    (apply str (get-collection-root config) coll-name extra)))

(defn create-body
  [{collection :collection :as config}]
  (if (map? collection) collection {:name collection}))

(defn create-collection!
  [config]
    (let [body (create-body config)
          resource (get-collection-root config)]
      (with-req config :resource resource :method :post :body body)))

(defn delete-collection!
  [config]
  (with-req config :resource (get-named-resource config) :method :delete))

(defn truncate-collection
  [config]
  (with-req config :resource (get-named-resource config "/truncate") :method :put))

(defn collection-stats
  "Returns a list of collection statics like document count."
  [config]
  (with-req config :resource (get-named-resource config "/figures") :method :get))

(defn collection-properties
  [config]
    (with-req config :resource (get-named-resource config "/properties") :method :get))

(defn list-collections
  [config]
  (with-req config :resource (get-named-resource config) :method :get))
