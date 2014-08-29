(ns travesedo.common
  (:require [travesedo.http :as client]
            [clojure.string :as cstr]))

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

(defn camelize [input-string] 
  (let [words (cstr/split input-string #"[\s_-]+")] 
    (cstr/join "" (cons (cstr/lower-case (first words)) (map cstr/capitalize (rest words))))))

;; FIXME to make key-mapper passed in better like :as key-mapper-fn or something.
(defn create-params
  ([config target-holder key-mapper & config-keys]
   (let [params (select-keys config config-keys)
         k-m (or key-mapper identity)
         params (into {} (for [[k v] params] [(camelize (k-m (name k))) v]))]
     (merge-with merge config {target-holder params}))))


(defn add-query-params
  "Moves driver configuration values into their query parameter locations for the request.
  It will merge any values in :query-params preset by the driver.

  Returns a new configuration with the original values and the :request-params."
  [config]
  (create-params config :query-params nil :wait-for-sync :exclude-system :create-collection :count))


(defn keep-key-when-value
  [coll keep-value?]
  (select-keys coll (for [[k v] coll :when (keep-value? v)] k)))


(defn move-headers [config]
  (let [{async :async} config
        ;; FIXME change the name on the key and value into a mapped over.
        header-params (keep-key-when-value {"x-arango-async" (name (or  async "false"))} not-nil? )]
    (merge config {:header-params header-params})))

(defn with-req
  [base-config & added-configs]
  (let [full-config (move-headers (merge base-config (add-query-params base-config) (apply hash-map added-configs)))]
    (process-response (client/execute full-config))))
