(ns travesedo.core)

(def default-config {})

(defn- add-config [config new-key value]
  (assoc (or config default-config) new-key value))

(defn with-database [db-name config]
  (add-config config :db db-name))

(defn with-host [ host config]
  (add-config config :host host))

(defn with-port [port config]
  (add-config config :port  port))

(defn with-collection [collection config]
  (add-config config :collection collection))

(defn with-security [flag config]
  "Indicates if the server should be connected over https or http"
  (add-config config :secure flag))

(defn with-username [username config]
  (add-config config :username username))

(defn with-password [password config]
  (add-config config :password password))

