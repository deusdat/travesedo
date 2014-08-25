(ns travesedo.core
  (:require [travesedo.util :as tutil]))

(defn with-connection 
  "Adds a connection configuration to a pre-existing general connection"
  [gen-config conn-config]  
  (update-in gen-config [:conns] #(into [] (conj % conn-config))))

(defn with-host [host-name conn-config]
  (tutil/add-config conn-config :host host-name))

(defn with-port [port conn-config]
  (tutil/add-config conn-config :port port))

(defn with-username [uname conn-config]
  (tutil/add-config [conn-config :username uname]))

(defn with-password 
  "Sets a password for the connection configuration."
  [password conn-config]
  (tutil/add-config [conn-config :password password]))

