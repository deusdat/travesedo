(ns travesedo.database
  (:require [travesedo.common :refer :all]))

(defn get-database-info [ctx]
  (let [db-resource (calc-resource-base ctx)
          current-resource (str db-resource "/database/current/")]
    (call-arango :get current-resource ctx)))
