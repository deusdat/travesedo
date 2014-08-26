(ns travesedo.common
  (:require [travesedo.http :as client]))

(def exceptional-codes #{403 400 409})

(defn default-exceptional? [code]
  (contains? exceptional-codes code))

(defn process-response
  ([req]
   (process-response req default-exceptional?))

  ([res exceptional?]
   (let [{{job-id :x-arango-async-id} :headers
          body :body
          {res-code :code} :body} res]
     (if (exceptional? res-code) 
       (throw (ex-info (:errorMessage body) res)) 
       {:async-id job-id 
        :result body
        :code res-code})
     )))

(defn move-headers [config]
   (let [header-keys [:async]
         header-items (select-keys config header-keys)]
        (assoc config :header-params header-items)))

(defn with-req
  [base-config & added-configs]
  (let [full-config (move-headers (conj base-config (apply hash-map added-configs)))]
    (println full-config)
    (client/execute full-config)))
