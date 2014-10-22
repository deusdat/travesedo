(ns travesedo.query
  "Manages query execution using AQL rather than simple queries."
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
 (let [q (query ctx)
       clean-ctx (conj (dissoc ctx :query) (select-keys q [:cursor-id]))]
   (loop [continue? (:has-more q) res (:result q)]
     (if  continue?
       (let [nb (next-batch clean-ctx)]
          (recur (:has-more nb) (conj (:result nb) res)))
       (conj {:result (vec (flatten res)), :has-more false, :count (count res)} (select-keys q [:error :code]))))))


(defn parse-aql-query [ctx]
  "Like query but only checks the query for syntaxic correctness. Does not execute the query."
  (map-response-keys (call-arango :post (derive-resource ctx "/query") (map-payload-keys ctx))))

