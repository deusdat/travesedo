(ns travesedo.core-test
  (:require [clojure.test :refer :all]
            [travesedo.core :refer :all]))

(deftest with--url-and-config
  (testing "Creation of configuration context without a config or url"
    (let [home "http://home"
          config {:database "MyCollection"}]
    (is (= {:host home :database "MyCollection"}
           (with-host home config))))))

