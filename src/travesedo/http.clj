(ns travesedo.http
  (:require [clj-http.client :as client]))

(def handler-lookup {:get client/get 
                     :post client/post 
                     :patch client/patch 
                     :put client/put
                     :delet client/delete})
(def core-params {:as :json 
                  :coerce :always
                  :throw-exceptions false})

(def exceptional #{400})

(defn with-query-parm 
  "Adds a query parameter to the request configuration"
  [p-key p-value req]
  )
;; FIXME need to support config object rather than a never ending line of args.
(defn send-request 
  ([action url]
   (send-request action url nil nil))
  ([action url query-params]
   (send-request action url query-params nil))
  ([action url query-params body]
   (let [handler (action handler-lookup)
         data-params (if (map? body) {:form-params body} {:body body})
         query-params {:query-params query-params}
         req-details (merge query-params data-params core-params)
         {res-body :body} (handler url req-details)]
     ;; Put logic here to handle real errors
     res-body)))
