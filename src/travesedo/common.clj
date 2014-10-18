(ns travesedo.common
  (:require [clj-http.client :as client]
                [clojure.string :as cstr]))

(defmacro def- [item value]
  `(def ^{:private true} ~item ~value))

(def- handler-lookup {:get client/get :post client/post  :patch client/patch :put client/put :delete client/delete})

(def- db-root "/_db")

(def- api-resource "/_api")

(def- get-conn #(select-keys % [:url :uname :password]))

(defn find-url[{url :url} resource]
  (str url resource))

(defmulti conn-selector :type)

(defmethod conn-selector :simple [conn]
  (get-conn conn))

;; FIXME replicas and shards share a common need to inherit credentials and pick a connection at random.
(defmethod conn-selector :replica [{ uname :uname password :password url :url method :method :as conn}]
  (if (= :get method)
    (let [replica (rand-nth (:replicas conn))]
      (if (:uname replica)
        (get-conn replica)
        {:url (:url replica) :uname uname :password password}))
    (get-conn conn)))

(defmethod conn-selector :shard [{ uname :uname password :password url :url method :method :as conn}]
  (let [shard (rand-nth (:coordinators conn))]
      (if (:uname shard)
        (get-conn shard)
        {:url (:url shard) :uname uname :password password})))

(defn find-connection [{conn :conn conn-select :conn-select}]
  "Finds the {:url :uname :password} for a given context"
  (if conn-select (conn-select conn) (conn-selector conn)) )


(defn calc-resource-base [{db :db}]
  "Creates the start of every resource based upon the database"
  (str db-root "/" db  api-resource))

(defn- get-value [ctx interested-keys key-mapping]
  "Used to look at a k-v pair to see if the value is interesting. If so, performs a possible transformation of the key"
  {:pre [(set? interested-keys)]};; using a list would result false for every key.
  (into {} (for [[k v] ctx :when (k  interested-keys) ] [(k key-mapping k) (name v)])))

(defn call-arango [method resource ctx]
  (let [handler (method handler-lookup)
          conn (find-connection (assoc-in ctx [:conn :method] method))
          full-url (find-url conn resource)
          auth  {:basic-auth [(:uname conn) (:password conn)]}
          query-params {:query-params (get-value ctx #{:wait-for-sync} {:wait-for-sync "waitForSync"})}
          headers {:query-params (get-value ctx #{:if-match :if-none-match} nil)}
          raw-response  (handler full-url  (conj {:as :json} query-params headers))]
        (println ctx)
        (:body raw-response) ))
