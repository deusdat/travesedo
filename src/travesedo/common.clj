(ns travesedo.common
  (:require [travesedo.http :as client]))

(def exceptional-codes #{403 400 409})

(defn default-exceptional? [code]
  (contains? exceptional-codes code))

(defn process-response
  ([res]
   (process-response res default-exceptional?))

  ([res exceptional?]
   (let [{{job-id :x-arango-async-id} :headers
          body :body
          res-code :status
          req-time :request-time} res]
     (if (exceptional? res-code) 
       (throw (ex-info (:errorMessage body) res)) 
       {:async-id job-id 
        :req-time req-time
        :result body
        :code res-code})
     )))

(def not-nil? (complement nil?))

(defn keep-key-when-value
  [coll keep-value?]
  (select-keys coll (for [[k v] coll :when (keep-value? v)] k)))

(defn move-headers [config]
   (let [{async :async} config
         ;; FIXME change the name on the key and value into a mapped over.
         header-params (keep-key-when-value {"x-arango-async" (name (or async "false"))} not-nil? )]
        (merge config {:header-params header-params})))

(defn with-req
  [base-config & added-configs]
  (let [full-config (move-headers (merge base-config (apply hash-map added-configs)))]
    (process-response (client/execute full-config))))

