(ns travesedo.aql
  (:require [travesedo.common :refer :all]))

(defn get-root-cursor
  [{db-name :db-name :as config}]
  (str "/_db/" db-name "/_api/cursor/"))

(defn get-cursor
  "Retrieves the next portion of a cursor after the initial query.
  Input {:cursor \"cursorID\" :db-name \"database1\"}

  Returns "
  [{cursor :cursor :as config}]
  (let [resource (str (get-root-cursor config) cursor)] 
    (with-req config resource :method :put)))

(defn query
  [{query-body :query-body :as config}]
  (let [resource (get-root-cursor config)]
    (with-req config :resource resource :method :post :body query-body)))


