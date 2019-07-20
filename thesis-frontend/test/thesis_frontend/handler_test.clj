(ns thesis-frontend.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [thesis-frontend.handler :refer :all]
            [byte-streams :as bs]
            [clojure.java.io :as io]))

(defn str->byte-array
  [^String s]
  (when s 
    (.getBytes s "ASCII")))

(def main-page-response "<!DOCTYPE html>\n<html><head><title>Bipolar Prediction Service</title></head><link href=\"/css/home-layout.css\" rel=\"stylesheet\" type=\"text/css\"><body><div id=\"text-box\"><h1>Bipolar Prediction Service</h1><h2>Upload NII file</h2><div><form action=\"/file\" enctype=\"multipart/form-data\" method=\"post\"><input id=\"file\" name=\"file\" size=\"20\" type=\"file\"><input id=\"submit\" name=\"submit\" type=\"submit\" value=\"submit\"></form></div></div></body></html>")

(deftest test-app
  (testing "main page"
    (let [response (app (mock/request :get "/"))]
      (is (= (:status response) 200))
      (is (= (:body response) main-page-response))))

  (testing "post"
    (let [response-from-queue (str->byte-array "[{\"filename\" \"1.jpg\" \"class-label\" \"bipolar\" \"class-probability\" \"100.00\"}]")]
      ;; Mock out the queuing section
      (with-redefs [publish-and-get-response (constantly ["metadata" response-from-queue])
                    io/file (constantly nil)
                    io/copy (constantly "")
                    sleep-thread (constantly 2000)]
        (testing "happy path - successful response"
          (let [response (app (-> (mock/request :post "/file")
                                  (mock/json-body {:tempfile ""})))]
            (prn response)
            (is (= (:status response) 200))
            ;; Check the PDF is generated
            (is (= (subs (with-open [i (io/input-stream (:body response))] 
                           (bs/convert i String)) 0 5) "%PDF-"))))
  
        (testing "no response"
          (with-redefs [publish-and-get-response (constantly ["metadata" nil])]
            (let [response (app (-> (mock/request :post "/file")
                                    (mock/json-body {:tempfile ""})))]
              (is (= (:status response) 200))
              (is (= (:body response) main-page-response))))))))

  (testing "not-found route"
    (let [response (app (mock/request :get "/invalid"))]
      (is (= (:status response) 404)))))
