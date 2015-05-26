(ns travesedo.index-test
  (:require 
    [travesedo.index :refer :all]
    [clojure.test :refer :all]))


(def col "test-col")
(def testing-ctx {:db :testing-db,
                  :in-collection col})

(deftest check-helpers
  (testing "Create cap-constraint with size"
    (is (= (conj testing-ctx {:payload {:type :cap :size 10},
                              })
           (make-cap-constraint testing-ctx col {:size 10}))))
  
  (testing "Create cap-constraint with byte-size"
    (is (= (conj testing-ctx {:payload {:type :cap 
                                        :byteSize 16384},
                              })
           (make-cap-constraint testing-ctx col {:byte-size 16384}))))
  
  (testing "Create cap-constraint with byte-size"
    (is (thrown? AssertionError
           (make-cap-constraint testing-ctx col testing-ctx))))
  
  (testing "Create hash-map with one attribute"
    (is (= (conj testing-ctx {:payload {:type :hash, 
                                        :fields [:b], 
                                        :unique false},
                              })
           (make-hash testing-ctx col :b))))
  
  (testing "Create hash-map with multiple attribute"
    (is (= (conj testing-ctx {:payload {:type :hash, 
                                        :fields [:b :a], 
                                        :unique false},
                              }) 
           (make-hash testing-ctx col [:b :a]))))
  
  (testing "Create skiplist with one attribute"
    (is (= (conj testing-ctx {:payload {:type :skiplist, 
                                        :fields [:b], 
                                        :unique false,
                                        :sparse false},
                              }) 
           (make-skiplist testing-ctx col :b false false))))
  
  (testing "Create skiplist with one attribute"
    (is (= (conj testing-ctx {:payload {:type :skiplist, 
                                 :fields [:a :b], 
                                 :unique true,
                                 :sparse true},
                              } )
           (make-skiplist testing-ctx col [:a :b] true true))))
  
  (testing "Create geo with one attribute"
    (is (= (conj testing-ctx {:payload {:type :geo,
                                        :fields [:b]}
                              })
             (make-geo testing-ctx col :b))))
  
  (testing "Create fulltext without minLength"
    (is (= (conj testing-ctx {:payload {:type :fulltext,
                                        :fields [:b]}})
           (make-fulltext testing-ctx col :b))))
  
  (testing "Create fulltext without minLength"
    (is (= (conj testing-ctx {:payload {:type :fulltext,
                                        :fields [:b],
                                        :minLength 15}})
           (make-fulltext testing-ctx col :b 15)))))
