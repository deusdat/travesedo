(ns travesedo.http
  (:require [clj-http.client :as client]
            [clojure.string :as string]))

(defn- find-auth [{auth :auth} {conn-auth :auth}]
  (if conn-auth conn-auth auth))

(defn- make-auth-token [req conn]
  (let [token (find-auth req conn)]
    {:basic-auth token}))


(defn build-url [{db-url :server-url :as conn} resource]
  (let [cleaned-url (string/replace db-url #"(?<=.)/$" "")]
    (str cleaned-url resource)))

(def handler-lookup {:get client/get 
                     :post client/post 
                     :patch client/patch 
                     :put client/put
                     :delete client/delete})

(def core-params {:as :json 
                  :content-type :json
                  :coerce :always
                  :throw-exceptions false
                  :header-params {:async :false 
                                  :waitForSync :true}})

(defn execute 
  "Calls a specified coordinator with the desired request spec. 
  :conns may be a either a connection map or a list of connection maps
  :conn-picker is a function that takes a list of conn maps and 
  returns the conn to use. If no picker passed, conn is random.
  :query-params are params to go after ? on the URL.
  :header-params go into the HTTP header. Zip is on by default.
  :body is the body of the request. If a map, it's convertered straight to JSON.
  :method is the HTTP methods :get, :post, :patch, :put, :delete"
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
        auth (make-auth-token req conn)
        url (build-url conn resource)
        data-params (if (map? body) {:form-params body} {:body body})
        conn-config (select-keys conn [:conn-timout :socket-timeout])
        req-details (merge 
                      core-params 
                      data-params 
                      query-params 
                      conn-config 
                      {:headers header-params}
                      auth)]
    (handler url req-details)
    ))


