(ns travesedo.http
  (:require [clj-http.client :as client]
            [clojure.string :as string]))

(def handler-lookup {:get client/get 
                     :post client/post 
                     :patch client/patch 
                     :put client/put
                     :delet client/delete})
(def core-params {:as :json 
                  :content-type :json
                  :coerce :always
                  :throw-exceptions false})

(def exceptional #{400})

(defn execute 
  "Calls a specified coordinator with the desired request spec. :conns should be a list of connection configurations
  for the present cluster. If the ArangoDB instance only has one conns can be a single map with expected keys."
  [{conns :conns
    resource :resource 
    method :method 
    query-params :query-params 
    header-params :header-params
    conn-picker :conn-picker
    body :body
    :as req }]
  {:pre [(not (nil? resource))]}
  (let [handler (method handler-lookup)
        conn ((or conn-picker #(rand-nth %)) (if (vector? conns) conns [conns]))
        url (build-url conn resource)
        data-params (if (map? body) {:form-params body} {:body body})
        req-details (merge core-params data-params query-params)
        {res-body :body status :status :as res} (handler url req-details body)]
    (println handler)
    (println url)
    (println req-details)
    res-body
    ))

(defn build-url [{db-url :db-url} resource]
  (let [cleaned-url (string/replace db-url #"(?<=.)/$" "")]
    (str cleaned-url resource)))
