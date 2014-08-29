(ns travesedo.collection
  (:require [travesedo.common :refer :all]))

(defn- get-collection-root
  [{db-name :db-name}]
  (str "/_db/" db-name "/_api/collection/"))

(defn- get-collection-name 
  [{coll :collection}]
  (if (map? coll) (:name coll) coll))

(defn- get-named-resource 
  [{db-name :db-name coll :collection :as config} & extra]
  (let [coll-name (get-collection-name config)]
    (apply str (get-collection-root config) coll-name extra)))

(defn- create-body
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

(defn truncate-collection!
  [config]
  (with-req config :resource (get-named-resource config "/truncate") 
    :method :put))

(defn collection-stats
  "Returns a list of collection statics like document count.
  Requires the config to contain values for :db-name and :collection

  Results
  the :result key in the return is a map with the following keys
  count: The number of documents currently present in the collection.
  figures.alive.count: The number of curretly active documents in all datafiles 
  and journals of the collection. Documents that are contained in the 
  write-ahead log only are not reported in this figure.

  figures.alive.size: The total size in bytes used by all active documents of
  the collection. Documents that are contained in the write-ahead log only 
  are not reported in this figure.

  figures.dead.count: The number of dead documents. This includes document
  versions that have been deleted or replaced by a newer version. Documents
  deleted or replaced that are contained the write-ahead log only are not 
  reported in this figure.

  figures.dead.size: The total size in bytes used by all dead documents.

  figures.dead.deletion: The total number of deletion markers. Deletion markers
  only contained in the write-ahead log are not reporting in this figure.

  figures.datafiles.count: The number of datafiles.

  figures.datafiles.fileSize: The total filesize of datafiles (in bytes).

  figures.journals.count: The number of journal files.

  figures.journals.fileSize: The total filesize of all journal files (in 
  bytes).

  figures.compactors.count: The number of compactor files.

  figures.compactors.fileSize: The total filesize of all compactor files (in 
  bytes).

  figures.shapefiles.count: The number of shape files. This value is deprecated
  and kept for compatibility reasons only. The value will always be 0 since 
  ArangoDB 2.0 and higher.

  figures.shapefiles.fileSize: The total filesize of the shape files. 
  This value is deprecated and kept for compatibility reasons only. The value 
  will always be 0 in ArangoDB 2.0 and higher.

  figures.shapes.count: The total number of shapes used in the collection. This 
  includes shapes that are not in use anymore. Shapes that are contained in 
  the write-ahead log only are not reported in this figure.

  figures.shapes.size: The total size of all shapes (in bytes). This includes
  shapes that are not in use anymore. Shapes that are contained in the 
  write-ahead log only are not reported in this figure.

  figures.attributes.count: The total number of attributes used in the 
  collection.

  Note: the value includes data of attributes that are not in use anymore. 
  Attributes that are contained in the write-ahead log only are not reported in
  this figure.

  figures.attributes.size: The total size of the attribute data (in bytes). 
  Note: the value includes data of attributes that are not in use anymore. 
  Attributes that are contained in the write-ahead log only are not reported in
  this figure.

  figures.indexes.count: The total number of indexes defined for the
  collection, including the pre-defined indexes (e.g. primary index).

  figures.indexes.size: The total memory allocated for indexes in bytes.
  figures.maxTick: The tick of the last marker that was stored in a journal of 
  the collection. This might be 0 if the collection does not yet have a 
  journal.

  figures.uncollectedLogfileEntries: The number of markers in the write-ahead 
  log for this collection that have not been transferred to journals or 
  datafiles.

  journalSize: The maximal size of the journal in bytes."
  [config]
  (let [resource (get-named-resource config "/figures")]
    (with-req config :resource resource :method :get)))

(defn collection-properties
  [config]
  (let [resource (get-named-resource config "/properties")]
    (with-req config :resource resource :method :get)))

(defn list-collections
  [config]
  (let [resource (get-named-resource config)]
    (with-req config :resource resource :method :get)))

(defn load-collection!
  "Loads the collection specified in the config :db-name :collection into 
  memory. Optionally, you can set :count to :false in the config. This can 
  speed up loading, but count will return 0.

  The :result field in the returned has the following map keys.
  On success an object with the following attributes is returned:

  id: The identifier of the collection.

  name: The name of the collection.

  count: The number of documents inside the collection. This is only returned
  if the count input parameters is set to true or has not been specified.

  status: The status of the collection as number.

  type: The collection type. Valid types are: 2: document collection 
  3: edges collection"
  [config]
  (with-req config :resouce (get-named-resource config "/load") :method :put))

(defn unload-collection!
  "Removes a collection from memory. All but transient collections surive.

  :result contains a map with the following key.   
  id: The identifier of the collection.
  name: The name of the collection.
  status: The status of the collection as number.
  type: The collection type. Valid types are:
  2: document collection
  3: edges collection
  "
  [config]
  (let [resource (get-named-resource config "/unload")]
    (with-req config :resouce resource :method :put)))

(defn rename-collection!
  "Renames a collection specified in the config by :collection with to a new
  name declared in the :document of the form {:name \"newname\"} Specify
  the database with :db-name and the collection to rename with :collection.

  :result is a map with the following keys.

  :id: The identifier of the collection.
  
  :name: The new name of the collection.
  
  :status: The status of the collection as number.
  
  :type: The collection type. Valid types are:
  2: document collection
  3: edges collection"
  [{document :document :as config}]
    (let [resource (get-named-resource config "/rename")]
      (with-req config :resource resource :method :put :body document)))

(defn change-properties!
  "Changes properties of a :collection in a :db-name. The :document
  field can have the following key-values. 
  :wait-for-Sync :true/:false
  :journal-size int in bytes of the new journal size."
  [{document :document :as config}]
  (let [resource (get-named-resource config "/properties")
        map-keys (fn [[k v]] [(camelize (name k)) v])]
    (with-req config :resource resource :method :put 
      :body (into {} (map map-keys document )))))
