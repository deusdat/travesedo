(ns travesedo.transaction
  (:require [travesedo.common :refer :all]))

(defn calc-transaction-base 
  "Generates the root resource for transaction activities.
  Similar to  /_db/testing/_api/curor"
  [ctx]
  (derive-resource ctx "/transaction") )

(defn transact
  "Executes a transaction on the server. The :payload of the ctx should be a
  map with the following fields.
  
  :collections - a map with two possible keys :read and :write. Each key can
  be either a list of collection names like ['person', 'address'] or a
  singular string like 'person'. :collections is not necessary for read-only
  execution. If you will modify a collection, you must specify it in the :write
  list; otherwise the transaction will fail.
  
  :action - a string that represents a JavaScript function.
  
  :params - either a list or map of parameters to pass to the function 
  specified in the :action function.
  
  :waitForSync - true or false.
  
  :lockTimeout - the number of seconds ArangoDB will hold a lock."
  [ctx]
  (let [resource (calc-transaction-base ctx)]
    (call-arango :post resource ctx)))
