(ns travesedo.query
  "Manages query execution for both simple and AQL queries. Most of the
  queries allow the :batch-size k-v in the :payload. If used and there are more
  documents than batch size, the server will return a :cursor-id. Use
  (next-batch) to get the remainer."
  (:require [travesedo.common :refer :all]
                [clojure.set :as cset]))

(defn calc-cursor-base [ctx]
  "Generates the root resource for cursor activities.
  Similar to  /_db/testing/_api/curor"
 (derive-resource ctx "/cursor") )

(defn map-payload-keys [{payload :payload :as ctx}]
  "Converts idomatic Clojure keyword to ArangoDB camelcase for :payload values."
  (let [mapped-payload (cset/rename-keys payload {:batch-size "batchSize", :bind-vars "bindVars"} )]
    (assoc ctx :payload mapped-payload)))

(defn map-response-keys [response]
 (cset/rename-keys response {:hasMore :has-more :id :cursor-id}))

(defn- manage-cursor [ctx method]
  "Executes the call allowing the method to drive targeted resource."
   (map-response-keys (call-arango method (str (calc-cursor-base ctx) "/" (:cursor-id ctx)) (map-payload-keys ctx))))

(defn next-batch [ctx]
  "Finds the next batch for a :cursor-id. Returns 404, if the cursor is exhausted."
  (manage-cursor ctx :put))

(defn delete [ctx]
  "Deletes the cursor designated at :cursor-id."
  (manage-cursor ctx :delete))

(defn- call-simple [ctx resource]
  "Helper function to coordinate mapping task."
  (map-response-keys (call-arango :put (derive-resource ctx resource) (map-payload-keys ctx))))

(defn return-all [ctx]
  "Returns all of the docs for a collection.
  :payload {:collection :skip :limit}. :skip and :limit are optional."
  (call-simple ctx "/simple/all"))

(defn by-example [ctx]
  "Queries a collection using an example document as the payload."
  (call-simple ctx "/simple/by-example"))

(defn by-example-first [ctx]
  "Queries a collection by example, returning the first document that matches."
  (call-simple ctx "/simple/first-example"))

(defn by-example-hash-index [ctx]
  "Queries for documents by example using a hash index."
  (call-simple ctx "/simple/by-example-hash"))

(defn by-example-skiplist-index [ctx]
  "This will find all documents matching a given example, using the specified skiplist index."
  (call-simple ctx "/simple/by-example-skiplist"))

(defn by-example-bitarray-index[ctx]
  "This will find all documents matching a given example, using the specified bitarray index."
  (call-simple ctx "/simple/by-example-bitarray"))

(defn by-condition-skiplist [ctx]
  "This will find all documents matching a given condition, using the specified skiplist index."
  (call-simple ctx "/simple/by-condition-skiplist"))

(defn by-condition-bitarray [ctx]
  "This will find all documents matching a given condition, using the specified skiplist index."
  (call-simple ctx "/simple/by-condition-bitarray"))

(defn any [ctx]
  "Returns a random document from a collection"
  (call-simple ctx "/simple/any"))

(defn within-range [ctx]
  "Finds all documents within a given range. A skip-list index is required."
  (call-simple ctx "/simple/range"))

(defn near [ctx]
  "The default will find at most 100 documents near the given coordinate.
  The returned list  is sorted according to the distance, with the nearest
  document being first in the list. If  there are near documents of equal
  distance, documents are chosen randomly from this  set until the limit is
  reached."
   (call-simple ctx "/simple/near"))

(defn within [ctx]
  "This will find all documents within a given radius around the
  coordinate (latitude, longitude). The returned list is sorted by distance.

  In order to use the within operator, a geo index must be defined for the
  collection. This index also defines which attribute holds the coordinates
  for the document. If you have more then one geo-spatial index, you can
  use the geo field to select a particular index."
  (call-simple ctx "/simple/within"))

(defn fulltext [ctx]
  "This will find all documents from the collection that match the fulltext
  query specified in query.

  In order to use the fulltext operator, a fulltext index must be defined for
  the collection and the specified attribute."
  (call-simple ctx "/simple/fulltext"))

(defn remove-by-example [ctx]
  "This will find all documents in the collection that match the specified
  example object. Supports :wait-for-sync."
  (call-simple ctx "/simple/remove-by-example"))

(defn replace-by-example [ctx]
  "This will find all documents in the collection that match the specified
  example object, and replace the entire document body with the new
  value specified. Note that document meta-attributes such as _id,
  _key, _from, _to etc. cannot be replaced. Supports :wait-for-sync"
  (call-simple ctx "/simple/replace-by-example"))

(defn update-by-example [ctx]
  "This will find all documents in the collection that match the specified
  example object, and partially update the document body with the new
  value specified. Note that document meta-attributes such as _id, _key,
  _from, _to etc. cannot be replaced."
  (call-simple ctx "/simple/update-by-example"))

(defn first-nth [ctx]
  "This will return the first document(s) from the collection, in the order
  of insertion/update time. When the count argument is supplied, the result
  will be a list of documents, with the \"oldest\" document being first in the
  result list. If the count argument is not supplied, the result is the \"oldest\"
  document of the collection, or null if the collection is empty."
  (call-simple ctx "/simple/first"))

(defn last-nth [ctx]
  "This will return the last documents from the collection, in the order of
  insertion/update time. When the count argument is supplied, the result
  will be a list of documents, with the \"latest\" document being first in the
  result list."
  (call-simple ctx "/simple/last"))

(defn aql-query [ctx]
  "Execute a query against ArangoDB. The query map should be in the :payload slot in the ctx.
  The query map can have the following form:
  {:query \"String of AQL\",
    :count :true/:false,
    :batch-size number,
    :ttl (time to live for cursor in seconds),
    :bind-vars (k-v list of parameters),
    :options (k-v list of options)}
  Everything but :query is optional.

  Upon successful execution, the response will look like this
  {:error false,
    :code http_code_number,
    :result [{...}],
    :has-more true/false,
    :count total_number_of_docs,
    :cursor-id \"cursor id for future calls\",
    :extra {...}}"
  (map-response-keys (call-arango :post (calc-cursor-base ctx) (map-payload-keys ctx))))

(defn aql-query-all[ctx]
  "Executes a query and reads all of the results into the :result field. This is presently an eager operation. Can return a partial load if the server failed part way through."
 (let [q (aql-query ctx)
       clean-ctx (conj (dissoc ctx :query) (select-keys q [:cursor-id]))]
   (loop [continue? (:has-more q) res (:result q)]
     (if  continue?
       (let [nb (next-batch clean-ctx)]
          (recur (:has-more nb) (conj (:result nb) res)))
       (conj {:result (vec (flatten res)), :has-more false, :count (count res)} (select-keys q [:error :code]))))))


(defn parse-aql-query [ctx]
  "Like aql-query but only checks the query for syntaxic correctness. Does not execute the query."
  (map-response-keys (call-arango :post (derive-resource ctx "/query") (map-payload-keys ctx))))

