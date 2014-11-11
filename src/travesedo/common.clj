(ns travesedo.common
  (:require [clj-http.client :as client]
            [clojure.string :as cstr]))

(defmacro def- [item value]
  `(def ^{:private true} ~item ~value))

(def- db-root "/_db")

(def- api-resource "/_api")

(def- client-config {:as :json 
                     :content-type :json 
                     :coerce :always 
                     :throw-exceptions false })

(defn calc-api-base
  "Creates the start of every resource based upon the database"
  [{db :db}]
  (str db-root "/" db  api-resource))

(defn derive-resource 
  "Calculates a fuller resource than calc-resource-base."
  [ctx resource]
  (str (calc-api-base ctx) resource))

(def- handler-lookup {:get client/get 
                      :post client/post  
                      :patch client/patch 
                      :put client/put 
                      :delete client/delete 
                      :head client/head})

(def- get-conn #(select-keys % [:url :uname :password]))

(defn find-url[{url :url} resource]
  (str url resource))

(defmulti conn-selector :type)

(defmethod conn-selector :simple [conn]
  (get-conn conn))

;; FIXME replicas and shards share a common need to inherit credentials and pick a connection at random.
(defmethod conn-selector :replica [{ uname :uname 
                                    password :password 
                                    url :url method 
                                    :method :as conn}]
  (if (= :get method)
    (let [replica (rand-nth (:replicas conn))]
      (if (:uname replica)
        (get-conn replica)
        {:url (:url replica) :uname uname :password password}))
    (get-conn conn)))

(defmethod conn-selector :shard [{ uname :uname 
                                  password :password 
                                  url :url 
                                  method :method :as conn}]
  (let [shard (rand-nth (:coordinators conn))]
    (if (:uname shard)
      (get-conn shard)
      {:url (:url shard) :uname uname :password password})))

(defn find-connection [{conn :conn conn-select :conn-select}]
  "Finds the {:url :uname :password} for a given context"
  (if conn-select (conn-select conn) (conn-selector conn)) )


(defn get-values 
  "Used to look at a k-v pairs to see if the value is interesting. If so, 
  performs a possible transformation of the key"
  [ctx interested-keys key-mapping]
  {:pre [(set? interested-keys)]};; using a list would result false for every key.
  (into {} (for [[k v] ctx :when (get  interested-keys k) ] 
             [(get key-mapping k k)  (name v)])))

(defn- transform-response [resp]
  "Processes the response from the server to make it match what the driver says 
  the clients should expect"
  (let [headers (get-values (:headers resp) 
                            #{"X-Arango-Async-Id" "Etag"} 
                            {"X-Arango-Async-Id" :job-id, "Etag" :rev})
        body (:body resp)]
    (conj {} body headers)))

(defn call-arango [method resource ctx]
  (let [handler (method handler-lookup)
        conn (find-connection (assoc-in ctx [:conn :method] method))
        full-url (find-url conn resource)
        auth  {:basic-auth [(:uname conn) (:password conn)]}
        query-params {:query-params 
                      (get-values ctx
                                  #{:wait-for-sync :exclude-system :load-count 
                                    :in-collection :create-collection :rev 
                                    :policy :keep-null :type}
                                  {:wait-for-sync "waitForSync", 
                                   :exclude-system "excludeSystem", 
                                   :keep-null "keepNull",
                                   :load-count :count, 
                                   :in-collection "collection", 
                                   :create-collection "createCollection"})}
        headers {:headers (get-values ctx #{:if-match :if-none-match :async} 
                                      {:async "x-arango-async"})}
        body {:form-params (:payload ctx)}
        raw-response  (handler full-url  (conj client-config  
                                               body 
                                               query-params 
                                               headers 
                                               auth))]
    (transform-response raw-response)))
