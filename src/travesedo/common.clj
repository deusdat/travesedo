(ns travesedo.common
  (:require [travesedo.http :as client]
            [clojure.string :as cstr]))

(def exceptional-codes #{403 400 409})

;; ArangoDB returns a result field in the body of the db calls.
(defn pull-result-up 
  "Used to pull the :result attribute out of various Arango results."
  [res]
  (let [pulled-up (assoc-in res [:result] (:result (:result res)))]
    (println pulled-up)
    (dissoc pulled-up (keys (:result pulled-up)))))


(defn default-exceptional? [code]
  (contains? exceptional-codes code))

(defn clojurize-key
  [k]
  (let [n (name k)
        cn (cstr/replace n #"[A-Z]" #(str \- (cstr/lower-case %)))]
    (keyword cn)))

(defn clojurize-top-keys 
  [res]
  (into {} (map (fn [[k v]] [(clojurize-key k) v]) res)))


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
       (clojurize-top-keys (merge {:async-id job-id 
                                   :req-time req-time
                                   :result (or (:result body) body)
                                   :code res-code} body))))))

(def not-nil? (complement nil?))

(defn camelize [input-string] 
  (let [words (cstr/split input-string #"[\s_-]+")] 
    (cstr/join "" (cons (cstr/lower-case (first words)) (map cstr/capitalize (rest words))))))

(defn query-mapper
  [k]
  (camelize (name k)))

(defn create-params
  ([config target-holder key-mapper & config-keys]
   (let [params (select-keys config config-keys)
         k-m (or key-mapper identity)
         params (into {} (for [[k v] params] [(key-mapper k) v]))]
     (merge-with merge config {target-holder params}))))


(defn add-query-params
  "Moves driver configuration values into their query parameter locations for the request.
  It will merge any values in :query-params preset by the driver.

  Returns a new configuration with the original values and the :request-params."
  [config]
  (let [out (create-params config 
                           :query-params query-mapper 
                           :wait-for-sync 
                           :exclude-system 
                           :create-collection 
                           :count
                           :rev
                           :policy)]
    out))


(defn- header-mapper
  [k]
  (let [pairs {:async "x-arango-async" :match-revision :if-none-match :no-match-revision :if-match}]
    (k pairs)))

(defn add-header-params [config]
  (create-params config :header-params header-mapper :match-revision :no-match-revision :async))

(defn with-req
  [base-config & added-configs]
  (let [full-config (merge base-config (apply hash-map added-configs))
        full-config (add-query-params full-config)
        full-config (add-header-params full-config)
        res (process-response (client/execute full-config))]
    (if (map? (:result res))
      (apply dissoc res (or (keys (:result res))))
      res)))
