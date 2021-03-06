(ns travesedo.document
  "Interface for working with documents directly."
  (:require [travesedo.common :refer :all]
            [clojure.string :as cstr]))

(def- doc-resource "/document")

(defn calc-document-resource-base
  "Builds a resource path upto, but excluding the document handle."
  [ctx]
  (derive-resource ctx doc-resource))

(defn calc-document-resource
  "Builds a resource path from the database to the document handle.
  For example:  /_db/testing/_api/document/thinking/5476749957"
  [ctx]
  (str (calc-document-resource-base ctx) "/" (:_id ctx)))

(defn find-by-id
  "Searches for a document by its :_id value AKA the document handle.
  Presently throws an exception if a 304 is returned when using 
  :if-none-match rev."
  [ctx]
  (call-arango :get (calc-document-resource ctx) ctx))

(defn create
  "Creates a new document in the collection specified by :in-collection. 
  :create-collection :yes will create a collection if it doesn't exist. 
  This will not work on a cluster. To wait for the document to flush to disk 
  use :wait-for-sync :true. The document should be in the :payload slot."
  [ctx]
  (call-arango :post (calc-document-resource-base ctx) ctx))

(defn replace-doc
  "Replaces a document that already exists. The new document should be in 
  :payload. The :_id field should hold the document handle 
  (collectionname/_key). You may optionally specify :wait-for-sync,
  :rev or :if-match, and :policy values. :rev is the etag for the document. 
  :policy :last will replace the document even if the revisions don't match. 
  :policy :error will cause a revision match, and fail they don't."
  [ctx]
  (call-arango :put ( calc-document-resource ctx) ctx))

(defn patch-doc
  "Replaces a subset of attributes for a given :_id. The map of attributes 
  to update should go in the :payload.You may optionally specify 
  :wait-for-sync, :rev or :if-match, and :policy values. :rev is the etag for 
  the document. :policy :last will replace the document even if the revisions 
  don't match. :policy :error will cause a revision match, and fail they don't.
  If you want to remove attributes from the structure of the document, 
  :keep-null :false will do it."
  [ctx]
  (call-arango :patch ( calc-document-resource ctx) ctx))

(defn delete
  "Deletes a document by its :_id. 
	A partial ctx is {:db \"somedb\" :_id \"collectionName/_key\"}  
  :rev, :policy, :wait-for-sync, and 
  :if-match work as they do in replace-doc."
  [ctx]
  (call-arango :delete ( calc-document-resource ctx) ctx))

(defn find-headers
  "Retrieves only the header fields of a document, does not return the 
  documents themselves. :rev, :if-none-match, :if-match work.
  Document to find's document handle should be in :_id."
  [ctx]
  (assoc (call-arango :head (calc-document-resource ctx) ctx) 
    :code 200 
    :error :false)  )

(defn read-all-docs
  "Finds all of the paths, :_id's, or :key's of a given collection. Specify 
  the collection using :in-collection. The type of the response should may be 
  :id, :key or :path. If you don't specify a type, ArangoDB defaults to :path."
  [ctx]
  (call-arango :get (calc-document-resource-base ctx) ctx))
