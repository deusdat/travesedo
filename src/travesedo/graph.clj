(ns travesedo.graph
  "Allows the client to interact with the graph half of ArangoDB."
  (:refer-clojure :exclude [load])
  (:require [travesedo.common :refer :all]
            [clojure.string]))

(def- traversal-resource "/traversal")
(def- graph-root "/gharial")

(defn create-graph-resource [ctx]
  (str (derive-resource ctx graph-root) "/" (:graph ctx)))

(defn list-graphs
  "Returns all of the graphs in the database provided in the context"
  [ctx]
  (let [resource (derive-resource ctx graph-root)]
    (call-arango :get resource ctx)))

(defn- swap-graph-keys [map depth-keys swap-map]
  (update-in map depth-keys 
                 clojure.set/rename-keys 
                 swap-map))

(defn create-graph!
  "Creates a new graph as configured. The context needs the structure.
  {:db \"db-name\",
   :payload {:name \"graph-name\",
             :edge-definitions [ {:collection \"edge-name\",
                                  :from [\"vert-coll1\",
                                         \"vert-coll2]}],
                                  :to [\"vert-coll3\",
                                      [\"vert-coll4\"]
                                  }],
             :orphan-collections [\"orph-coll1\"]}}"
  [ctx]
  (let [resource (derive-resource ctx graph-root)
        cleaned-outer (swap-graph-keys ctx [:payload] 
                        {:edge-definitions :edgeDefinitions,
                         :orphan-collections :orphanCollections})]
    (println resource)
    (swap-graph-keys (call-arango :post resource cleaned-outer)
                    [:graph] {:edgeDefinitions :edge-definitions,
                              :orphanCollections :orphan-collections })))

(defn delete-graph!
  "Deletes a graph from the database. ctx :graph names the graph to delete."
  [ctx]
  (call-arango :delete (create-graph-resource ctx) ctx))

(defn traverse
  "Executes a traversal on the server. The :body value of the context should
  look like what's posted in 25.13 of the manual.

  The values for the keys :filter, :visitor, :init, :expander, and :sort should
  be  string that can get dropped into a JS function the server. It's a little
  weird, but it's do to a limitation of JSON: no functions allowed.

  Here's a filter example:
  :filter \"if(vertex.name === 'Bob') { return 'exclude'} else return ''\""
  [ctx]
  (let [resource (derive-resource ctx traversal-resource)]
    (call-arango :post resource ctx)))


(defn- define-edge-collection
  "Helper function to create an edge collection path using graph language"
  [from to relationship]
  {:collection relationship, :from from, :to to})

(defn- define-x-path
  [{graph :graph :as ctx} element]
  (clojure.string/join "/" [(derive-resource ctx graph-root) graph element]))

(defn- define-edge-path-by-id
  [{id :id, :as ctx}]
  (clojure.string/join "/" [(define-x-path ctx "edge") id]))

(defn- define-vertex-path-by-id
  [{id :id, :as ctx}]
  (clojure.string/join "/" [(define-x-path ctx "vertex") id]))

(defn- define-edge-path
  [{graph :graph coll :collection :as ctx}]
  (clojure.string/join "/" 
                       [(define-x-path ctx "edge") (name coll)]))

(defn define-edge-path-by-key
  [{edge-key :edge-key :as ctx}]
  (clojure.string/join "/" [(define-edge-path ctx) edge-key]))

(defn find-edge-path
  [{edge-key :edge-key :as ctx}]
  (if edge-key
               (str (define-edge-path ctx) "/" edge-key)
               (define-edge-path-by-id ctx)))

(defn define-vertext-path 
  "Returns the path to the vertex collection"
  [{col :collection :as ctx}]
  (str (define-x-path ctx "vertex") "/" col))


(defn find-vertext-path
  "Returns the path to a specific vertex defined by either :_id for the 
   document, or by the pair of :collection and :vertex-key"
  [{vertex-key :vertex-key :as ctx}]
  (if vertex-key
      (str (define-vertext-path ctx) "/" vertex-key)
      (define-vertex-path-by-id ctx)))

(defn create-edge!
    "Inserts an edge record into the specified edge collection. In the ctx,
     specify the :graph :collection and the :payload, with minimum value 
    {:from :to }. You can put any other attributes in your edge that you want."
    [ctx]
    (let [converted-ctx (swap-graph-keys ctx [:payload] 
                          {:to :_to, :from :_from})]
      (call-arango :post (define-edge-path converted-ctx) converted-ctx)))

(defn modify-edge!
  "Modifies the attributes of an edge with new values defined in the :payload
   of the ctx. You can specify the edge by either passing :_id or :collection
  and :edge-key.
  Example ctx that sets the quanity attribute to 10.
  {..db stuff.. 
   :graph \"Example\", 
   :collection \"owns\", 
   :edge-key 8827712,
   :payload {:quantity 10}}

  Example result:
  {:rev \"82716136933\", 
   :edge {:from \"profile/81258157541\", 
          :to \"product/81204090341\", 
          :_id \"own/82550199781\", 
          :_key \"82550199781\", 
          :_rev \"82716136933\", 
          :count 2, 
          :quantity 14, 
          :u \"product/81204090341profile/81258157541\"}, 
    :code 200, 
    :error false}
  "
  [ctx]
  (call-arango :patch (find-edge-path ctx) ctx))

(defn replace-edge!
  "Replaces the none edgy information (:to :from) with whatever data is in 
   :payload. Returns the revision for the edge.
   
   Example Input:
   {:collection \"own\", 
    :edge-key 82550199781, 
    :graph \"JustThinking\",
    :payload {:something :different}}

   Example Result:
   {:rev \"82811295205\", 
    :edge {:from \"profile/81258157541\", 
           :to \"product/81204090341\", 
           :_id \"own/82550199781\", 
           :_key \"82550199781\", 
           :_rev \"82811295205\", 
           :something \"different\"}, 
    :code 200, 
    :error false}
  "
  [ctx]
  (call-arango :put (find-edge-path ctx) ctx))

(defn delete-edge!
  "Deletes an edge from the edge collection/relationship. In order to identify
   the edge you must specify the :graph as well as either the :_id 
   (ie, owns/12312312) or the pair of keys :edge-key and :collection."
  [{edge-key :edge-key :as ctx}]
  (let [path (find-edge-path ctx)] 
    (call-arango :delete path ctx)))

(defn get-edge
  "Returns an edge based on the the :_id or the pairing of :collection and 
   :edge-key.

   Sample result:
   {:rev \"82550199781\", 
    :edge {:_id \"own/82550199781\", 
           :_key \"82550199781\", 
           :_rev \"82550199781\", 
           :from \"profile/81258157541\", 
           :to \"product/81204090341\", 
           :count 2, 
           :u \"product/81204090341profile/81258157541\"}, 
    :code 200, 
    :error false}"
  [ctx]
  (swap-graph-keys (call-arango :get (find-edge-path ctx) ctx) 
    [:edge] {:_from :from, :_to :to}))

(defn create-vertex!
  "Creates a vertex in the collection. Supports :wait-for-sync.

  Example ctx
  {...db stuff...
   :collection \"profile\",
   :graph \"Example\",
   :payload {:first-name \"Amber\",
             :last-name \"Davenport\"}}

  Example Result
  {:rev \"83159553509\", 
   :vertex {:_id \"profile/83159553509\", 
            :_rev \"83159553509\", 
            :_key \"83159553509\"}, 
   :code 202, 
   :error false}
  "
  [ctx]
  (call-arango :post (define-vertext-path ctx) ctx))

(defn delete-vertex!
  "Removes a vertex from a collection. ArangoDB will make sure that there
   are no dangling edges between the vertex and any other vertices.
  
   Example ctx
   {...db stuff...
    :collection \"profile\" 
    :graph \"Example\"
    :vertex-key \"83159553509\"}

   Example Successfull Result
   {:removed true, :code 202, :error false}
  "
  [ctx]
  (call-arango :delete (find-vertext-path ctx) ctx))

(defn modify-vertex!
  [ctx]
  "Modifies the attributes of an existing vertex to the values found in 
   :payload. If the attribute doesn't exist, it's added. Supports 
   :wait-for-sync, :if-match and :keep-null.

   Example ctx
   {...db stuff...
    :collection \"profile\",
    :graph \"Example\",
    :vertex-key 83395220965,
    :payload {:age \"Never ask a lady that\"}}

   Example Result
   {:rev \"83563648485\", 
    :vertex {:_id \"profile/83395220965\", 
             :_rev \"83563648485\", 
             :_oldRev \"83446732261\", 
             :_key \"83395220965\"}, 
    :code 202, 
    :error false}
  "
  [ctx]
  (call-arango :patch (find-vertext-path ctx) ctx))

(defn replace-vertex!
  "Replaces a vertex's content with the :payload value. Supports :wait-for-sync
   and :if-match settings.

   Example ctx
   {...db stuff...
    :collection \"profile\"
    :graph \"Example\"
    :vertex-key \"83159553509\"
    :payload {:first-name \"Ams\", :last-name \"Stout\"}}

   Example Result
   {:rev \"83446732261\", 
    :vertex {:_id \"profile/83395220965\", 
             :_rev \"83446732261\", 
             :_oldRev \"83395220965\", 
             :_key \"83395220965\"}, 
    :code 202, 
    :error false}
  "
  [ctx]
  (call-arango :put (find-vertext-path ctx) ctx))

(defn get-vertex
  "Retrieves a vertex based on its :collection and :vertex-key, or its
   :_id. 

   Example ctx
   {...db stuff...
    :collection \"profile\"
    :graph \"Example\"
    :vertex-key \"83159553509\"}

   Example Result
   {:rev \"83623941605\", 
    :vertex {:_id \"profile/83395220965\", 
             :_key \"83395220965\", 
             :_rev \"83623941605\", 
             :first-name \"Ams\", 
             :last-name \"Stout\", 
             :age \"Never ask a lady that\"}, 
     :code 200, 
     :error false}
  "
  [ctx]
  (call-arango :get (find-vertext-path ctx) ctx))