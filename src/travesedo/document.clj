(ns travesedo.document
  "Interface for working with documents directly."
  (:require [travesedo.common :refer :all]
            [clojure.string :as cstr]))

(def- doc-resource "/document")

(defn calc-document-resource-base [ctx]
 (str (calc-resource-base ctx) doc-resource ))

(defn calc-document-resource [ctx]
  (str (calc-document-resource-base ctx) "/" (:_id ctx)))

(defn find-by-id [ctx]
  "Searches for a document by its :_id value AKA the document handle.
  Presently throws an exception if a 304 is returned when using :if-none-match rev."
  (call-arango :get (calc-document-resource ctx) ctx))

(defn create [ctx]
  "Creates a new document in the collection specified by :in-collection. :create-collection :yes
  will create a collection if it doesn't exist. This will not work on a cluster. To wait for the document
  to flush to disk use :wait-for-sync :true. The document should be in the :payload slot."
  (call-arango :post (calc-document-resource-base ctx) ctx))
