(ns travesedo.collection
  (:require [travesedo.common]))

(defn get-collection-root
  [{db-name :db-name}]
    (str "/_db" db-name "/_api/collection/"))

(defn get-resource 
  [{db-name :db-name coll :collection :as config}]
  (let [coll-name (if (map? coll) (:name coll) coll)]
    (get-collection-root coll-name)))



(create-collection!
  [config]
    (let [body (create-body config)
          resource (get-collection-root config)]
      (with-req config :resource resource  :method :post :body body)))

(delete-collection!
  [config]
  )

(collection-stats
  "Returns a list of collection statics like document count."
  [config]
  )

(list-collections
  [config]
  )
