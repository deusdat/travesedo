(ns travesedo.collection
  "Allows the client to modify collection settings on ArangoDB for a database. 
  For exact responses for each operation see 
  http://docs.arangodb.org/HttpCollection/Getting.html
  It is safe to presume that :db and :collection are required keys for most 
  operations."
  (:refer-clojure :exclude [load])
  (:require [travesedo.common :refer :all]))

(def- collection-resource "/collection")

(defn create
  "Creates a collection defined in the :payload of the context. The map
  can have the following values 
  
  :name - a string naming the new collection. Required.
  
  :wait-for-sync - boolean indicating that the collection should wait for a disk
  sync before returning that the operation result.
  Defaults to false.

  :do-compact - boolean determining if the collection will be compacted.
  Defaults to true.
  
  :journal-size - int the maximal size journal for the collection in bytes.
  Defaults to system value.
  
  :is-system - boolean indicating the collection belongs in the _system db.
  Defaults to false.
  
  :is-volatile - boolean indicating the collection is in-memory only. 
  Defaults to false.
  
  :key-options - a vec of simple maps with the following options. 
  		:type :traditional | :autoincrement 
  		:allow-user-keys - a boolean.
  		:increment - the amount to add to each key for :autoincrement.
  		:offset - initial offset of the autoincrement.

	:type - indicates collection type. Default is 2 for document. 3 indicates
	an edge collection. Everything else is ignored.
	
	:number-of-shards - number of shards for a collection in a cluster. Meanless
	in a single server environment.
	
	:shard-keys - vec of strings indicating the attributes of the document used
	to derive a shard key. Once set, cannot be undone.
	
	Example ctx 
	(def ctx {:conn {:type :simple
                      	:url \"http://localhost:8529\"}
               :async :stored
               :db \"example_db\"
               :connection-timeout 1000
               :socket-timeout 2000
               :accept-all-ssl? true
               :payload {
						:name \"DemoCollection\"
						:wait-for-sync true
						:key-options [{:type :traditional} {:allow-user-keys true}]               
               }})
          
  	If success, returns a map 
  	{ 
  		:id \"1173494600\", 
  		:name testCollectionUsers, 
  		:wait-for-sync false, 
  		:is-volatile false, 
  		:is-system false, 
  		:status 3, 
  		:type 2, 
  		:error false, 
  		:code 200 
	}
  "
  [ctx]
  (let [db-resource (derive-resource ctx collection-resource)]
    (call-arango :post db-resource ctx)))

(defn- find-collection-resource [ctx]
  (derive-resource ctx (str collection-resource "/" (:collection ctx))))

(defn delete-collection
  "Deletes a collection, specified by :collection within the database specified 
  by :db.

	Example ctx
	(def ctx {:conn {:type :simple
                      	:url \"http://localhost:8529\"}
               :async :stored
               :db \"example_db\"
               :collection \"ToBeDeleted\"})  
  
  Returns a map of the form {:code 200, :error false, :id \"16261336386\"} 
  if successful."
  [ctx]
  (let [collection-resource (find-collection-resource ctx) ]
    (call-arango :delete collection-resource ctx)))

(defn get-collection-info
  	"Retrieves the meta-data about a collection. The context needs the 
  	:collection and :db set.
 
	Example ctx
	(def ctx {:conn {:type :simple
                      	:url \"http://localhost:8529\"}
               :async :stored
               :db \"example_db\"
               :collection \"ToBeDeleted\"})  
               
	Returns a map of the form 
  	{
  		:code 200,
 		:error false,
 		:type 2,
 		:status 3,
 		:isSystem false,
 		:name \"people\",
 		:id \"41559742461\"
 	}
  "
  [ctx]
  (let [collection-resource (find-collection-resource ctx)]
    (call-arango :get collection-resource ctx)))

(defn get-collection-properties
  "Retrieves the meta-data about a collection. The context needs the 
  :collection and :db set.
  
	Example ctx
	(def ctx {:conn {:type :simple
                      	:url \"http://localhost:8529\"}
               :async :stored
               :db \"example_db\"
               :collection \"people\"})    
  
  	Upon success your result will like like this map
  	{
  		:journal-size 33554432,
 		:do-compact true,
 		:is-volatile false,
 		:name \"people\",
 		:is-system false,
 		:type 2,
 		:key-options {:type \"traditional\", :allowUserKeys true},
 		:status 3,
 		:id \"43471493117\",
 		:code 200,
 		:error false,
 		:wait-for-sync false
 	}"
  [ctx]
  (let [collection-resource (str (find-collection-resource ctx) "/properties") ]
    (call-arango :get collection-resource ctx)))

(defn get-all-collections 
	"Returns all of the collections for a given database.
	
	Example ctx
	(def ctx {:conn {:type :simple
	                   	:url \"http://localhost:8529\"}
	            :async :stored
	            :db \"example_db\"
	            :exclude-system true})  
	
	Upon success, get map like 
	{:code 200,
	 :error false,
	 :names
	 {:people
	  {:id 44090742781,
	   :name people,
	   :isSystem false,
	   :status 3,
	   :type 2}},
	 :collections
	 [{:id 44090742781,
	   :name people,
	   :isSystem false,
	   :status 3,
	   :type 2}]}          
	"
	[ctx]
	(call-arango :get (find-collection-resource ctx) ctx))

(defn load-collection
  "Loads a given collection into memory. Helpful to prime the first read off a 
  collection.
  :load-count true will return the :count value in the result.
  :load-count false should make the operation faster. 
  :db and :collection are required.
  
  Example ctx
	(def ctx {:conn {:type :simple
	                   	:url \"http://localhost:8529\"}
	            :db \"example_db\"
	            :collection \"people\"
	            :load-count true})  
	            
	Example result
	{
		:code 200,
		:error false,
		:type 2,
		:status 3,
		:count 2,
		:name \"people\",
		:id 44255172605,
		:is-system false
	}"
  [ctx]
  (call-arango :put (str (find-collection-resource ctx) "/load") ctx))

(defn unload 
  "Unloads a given collection from memory. 
	Example ctx
	(def ctx {:conn {:type :simple
	                   	:url \"http://localhost:8529\"}
	            :db \"example_db\"
	            :collection \"people\"})
  
  Example result
	{
		:code 200,
		:error false,
		:type 2,
		:status 3,
		:count 2,
		:name \"people\",
		:id 44255172605,
		:is-system false
	}"
  [ctx]
  (call-arango :put (str (find-collection-resource ctx) "/unload") ctx))

(defn change-properties 
  "Changes a group of properties via the :payload value. The link 
  http://docs.arangodb.org/HttpCollection/Modifying.html
  details the possible map."
  [ctx wait-for-sync journal-size]
  {:pre [(number? journal-size)]}
  (call-arango :put (str (find-collection-resource ctx) "/properties") 
  		(assoc ctx :wait-for-sync wait-for-sync :journal-size journal-size)))

(defn rename! 
  "Renames a collection. The :payload value should be 
  {:name \"new-collection-name\"}"
  [ctx]
  (call-arango :put (str (find-collection-resource ctx) "/rename") ctx))

(defn rotate-journal 
  "Rotates a collection onto a new journal."
  [ctx]
  (call-arango :put (str (find-collection-resource ctx) "/rotate") 
               (conj {:payload {}} ctx)))
