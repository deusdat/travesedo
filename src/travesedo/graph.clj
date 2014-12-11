(ns travesedo.graph
  "Allows the client to interact with the graph half of ArangoDB."
  (:refer-clojure :exclude [load])
  (:require [travesedo.common :refer :all]
                  [clojure.string]))

(def- traversal-resource "/traversal")
(def- graph-root "/gharial")

(defn create-edge-path
  [{graph :graph coll :collection :as ctx}]
  (clojure.string/join "/" [(derive-resource ctx graph-root) graph "edge" coll]))

(defn traverse
  "Executes a traversal on the server. The :body value of the context should
  look like what's posted in 25.13 of the manual.

  The values for the keys :filter, :visitor, :init, :expander, and :sort should
  be  string that can get dropped into a JS function the server. It's a little
  weird, but it's do to a limitation of JSON: no functions allowed.

  Here's a filter example:
  :filter \"if(vertex.name === 'Bob') { return 'exclude'} else return ''\""
  [ctx]
  (let [resource (derive-resource traversal-resource)]
    (call-arango :post resource ctx)))

(defn create-edge
    "Inserts a new edge between two pre-existing vertices. The client must
    specify the name of the graph via the :graph key in the context. It must
    specify the edge-collection via the :collection key. The :body must have
    the minimum structure of
    {:_to '_idOfDestinationVertex'
      :_from '_idOfSourceVertex'
    } You can add as many other values to this base form as needed."
    [ctx]
    (call-arango :post (create-edge-path ctx) ctx))
