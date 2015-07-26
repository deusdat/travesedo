(ns travesedo.index
  (:require [travesedo.common :refer :all]
            [clojure.set :as cset]))

(def- index-resource "/index")

(defn calc-index-resource-base
  "Builds a resource path upto, but excluding the document handle."
  [ctx]
  (derive-resource ctx index-resource))

(defn calc-index-handle-resource
  [ctx index-handle]
  (str (calc-index-resource-base ctx) "/" index-handle))

(defn add-index [ctx collection-name index-payload]
  (assoc ctx :in-collection collection-name,
    :payload index-payload))

(defn protect-vector
  [val]
  (if (vector? val) val [val]))

(defn make-hash 
  "Helper function to create a hash index. Takes an existing context and adds
   the proper payload value for a hash index."
  ([ctx collection-name fields]
    (make-hash ctx collection-name fields false))
  ([ctx collection-name fields unique]
    ( add-index ctx collection-name {:type :hash,
                                     :unique unique,
                                     :fields (protect-vector fields)})))

(defn make-cap-constraint 
  "Modifies the context to have the information necessary to create a  
   cap-constraint index.
   The config parameter must specify either :size or :byte-size or both."
  [ctx collection-name config]
  {:pre [(or (contains? config :size) (contains? config :byte-size))]}
  (add-index ctx collection-name (conj {:type :cap} 
                                   (cset/rename-keys config 
                                     {:byte-size :byteSize}))))

(defn make-skiplist 
  "Creates a skiplist index on the collection-name, for the attributes 
   specified,  as well as if it should be sparse and/or unique"
  [ctx collection-name fields unique sparse]
  {:pre [(some? fields) 
         (some? collection-name) 
         (not-empty ctx)]}
  (add-index ctx collection-name {:type :skiplist, 
                                  :unique unique, 
                                  :sparse sparse,
                                  :fields (protect-vector fields)}))

(defn make-geo
  "Creates a geo-index on a collection-name's attributes defined in fields.
  Fields can be either a single attribute with a double array of [lat long],
  or two fields, the first being :lat and the second being :long."
  [ctx collection-name fields] 
  (add-index ctx collection-name {:type :geo,
                                  :fields (protect-vector fields)}))

(defn make-fulltext
  "Creates a full text index on the attribute with the minimum length if 
   provided"
  [ctx collection-name attribute & minLength] 
  (add-index ctx collection-name 
    (conj {:type :fulltext,
           :fields (protect-vector attribute),
           } 
      (when minLength {:minLength (first minLength)}))))


(defn create!
  [ctx]
  (call-arango :post (calc-index-resource-base ctx) ctx))

(defn create-skiplist!
  ([ctx collection-name fields]
    (create-skiplist! ctx collection-name fields false false))
  ([ctx collection-name fields unique sparse]
    (create! (make-skiplist ctx collection-name fields unique sparse))))

(defn create-hash! 
  "Creates a hash index on the set of fields for a collection. Can also be
  unique."
  ([ctx collection-name fields]
    (create-hash! ctx collection-name fields false))
  ([ctx collection-name fields unique]
  (create! (make-hash ctx collection-name fields unique))))

(defn create-cap-constraint!
  "Creates a cap-constraint index on the collection with the proper 
   configuration. The config param should be a map like 
   {:size NUM_DOCS_IN_COLLECTION, :byte-size NUM_IN_BYTES}. Failure to provide
   either will result in an error code returned."
  [ctx collection-name config]
  (create! (make-cap-constraint ctx collection-name config)))

(defn read-all
  "Returns all indexs for the collection.
   {:code 200, 
    :error false, 
    :identifiers {:profile/0 {:id \"profile/0\", 
                              :type \"primary\", 
                              :unique true, 
                              :sparse false,
                              :selectivityEstimate 1, 
                              :fields [\"_key\"]}}, 
                  :indexes [{:id \"profile/0\", 
                             :type \"primary\", 
                             :unique true, 
                             :sparse false, 
                             :selectivityEstimate 1, 
                             :fields [\"_key\"]}]}"
  [ctx collection-name]
  (call-arango :get (calc-index-resource-base ctx) 
    (assoc ctx :in-collection collection-name)))


(defn delete!
  "Deletes an index that matches a criteria defined by fn matches?.
   matches? gets an index entry map, like those returned by read-all; returns
   true on match.

   Returns a seq of delete responses shaped like {:code 200}"
  [ctx collection-name matches?]
  (for [idx (:indexes (read-all ctx collection-name)) :when (matches? idx)]
    (call-arango :delete (calc-index-handle-resource ctx (:id idx)) ctx)))


(defn create-geo!
  [ctx collection-name fields]
  (create! (make-geo ctx collection-name fields)))

(defn delete-geo!
  "Deletes a geo index based on its attribute"
  [ctx collection-name fields]
  (delete! ctx collection-name #(and (.startsWith (:type %) "geo") 
                                     (= (:fields %) fields))))

(defn delete-hash!
  "Helper to delete a hash based on its fields."
  [ctx collection-name fields]
  (delete! ctx collection-name #(and (.startsWith (:type %) "hash") 
                                     (= (mapv name fields) (:fields %)))))

(defn create-fulltext!
  [ctx collection-name attribute & min-length]
  (create! (make-fulltext ctx collection-name attribute min-length)))

