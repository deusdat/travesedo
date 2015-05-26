(defproject deusdatsolutions/travesedo "0.5.0"
  :description "A clojure driver for the ArangoDB server."

  :url "https://github.com/deusdat/travesedo"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-http "1.0.0"]
                 [org.clojure/tools.trace "0.7.8"]]
  :plugins [[lein-kibit "0.0.8"]
            [jonase/eastwood "0.2.1"]]

  :profiles {:dev { :plugins [[com.jakemccrary/lein-test-refresh "0.5.2"]
                              [lein-autoreload "0.1.0"]]}})
