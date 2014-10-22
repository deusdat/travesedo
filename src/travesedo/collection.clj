(ns travesedo.collection
  "Allows the client to modify collection settings on ArangoDB for a database. For exact
  responses for each operation see http://docs.arangodb.org/HttpCollection/Getting.html
  It is safe to presume that :db and :collection are required keys for most operations."
  (:require [travesedo.common :refer :all]))

(def- collection-resource "/collection")

(defn create-collection [ctx]
  "Creates a collection defined in the __:payload__ key that follows the form at http://docs.arangodb.org/HttpCollection/Creating.html"
  (let [db-resource (derive-resource ctx collection-resource)]
  (call-arango :post db-resource ctx)))

(defn find-collection-resource [ctx]
 (derive-resource ctx (str collection-resource "/" (:collection ctx))))

(defn delete-collection [ctx]
  "Deletes a collection, specified by :collection within the database specified by :db.
  Returns a map of the form {:code 200, :error false, :id \"16261336386\"} if successful."
  (let [collection-resource (find-collection-resource ctx) ]
    (call-arango :delete collection-resource ctx)))

(defn get-collection-info [ctx]
  "Retrieves the meta-data about a collection. The context needs the :collection and :db set."
  (let [collection-resource (find-collection-resource ctx)]
  (call-arango :get collection-resource ctx)))

(defn get-collection-properties [ctx]
  "Retrieves the meta-data about a collection. The context needs the :collection and :db set."
  (let [collection-resource (str (find-collection-resource ctx) "/properties") ]
  (call-arango :get collection-resource ctx)))

(defn get-all-collections [ctx]
     (call-arango :get (find-collection-resource ctx) ctx))

(defn load-collection [ctx]
  "Loads a given collection into memory. Helpful to prime the first read off a collection.
  :load-count :true will return the :count value in the result.
  :load-count :false should make the operation faster. :db and :collection are required."
  (call-arango :put (str (find-collection-resource ctx) "/load") ctx))

(defn unload-collection [ctx]
  "Unloads a given collection from memory. :load-count :true will return the :count value in the result.
  :load-count :false should make the operation faster. :db and :collection are required."
  (call-arango :put (str (find-collection-resource ctx) "/unload") ctx))

(defn change-properties [ctx]
  "Changes a group of properties via the :payload value. The link http://docs.arangodb.org/HttpCollection/Modifying.html
  details the possible map."
  (call-arango :put (str (find-collection-resource ctx) "/properties") ctx))

(defn rename [ctx]
  "Renames a collection. The :payload value should be {:name \"new-collection-name\"}"
  (call-arango :put (str (find-collection-resource ctx) "/rename") ctx))

(defn rotate-journal [ctx]
  "Rotates a collection onto a new journal. "
  (call-arango :put (str (find-collection-resource ctx) "/rotate") (conj {:payload {}} ctx)))
