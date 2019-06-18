(ns thesis-frontend.predict-test
  (:require [clojure.test :refer :all]
            [thesis-frontend.predict :as pred]
            [ring.mock.request :as mock]
            [thesis-frontend.handler :refer :all]))
  

  ;
  ;
  ;(deftest predict-test 
  ;  (testing "nii hits function"
  ;    (some? (pred/predict )) 
  ;  
  ;  
  ;  
  ;  )
  ;
  ;)


(deftest test-app
  (testing "main route"
    (let [response (app (mock/request :post "/" #_(clojure.java.io/file "/Users/pmartyn/Documents/MRI-Brain/sub-60046_anat_sub-60046_T1w.nii")))]
      (is (= (:status response) 200))
      (is (= (:body response) "Hello World")))))