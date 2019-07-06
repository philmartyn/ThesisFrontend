(ns thesis-frontend.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [thesis-frontend.handler :refer :all]))

(deftest test-app
  (testing "main route"
    (let [response (app (mock/request :get "/"))]
      (is (= (:status response) 200))
      (is (= (:body response) "<!DOCTYPE html>
<html><head><title>Bipolar Prediction Service</title></head><link href=\"/css/home-layout1.css\" rel=\"stylesheet\" type=\"text/css\"><body><div id=\"text-box\"><h1>Bipolar Prediction Service</h1><h2>Upload NII file</h2><div id=\"form-box\"><form action=\"/file\" enctype=\"multipart/form-data\" method=\"post\"><input id=\"file\" name=\"file\" size=\"20\" type=\"file\"><input id=\"submit\" name=\"submit\" type=\"submit\" value=\"submit\"></form></div></div></body></html>"))))

  (testing "not-found route"
    (let [response (app (mock/request :get "/invalid"))]
      (is (= (:status response) 404)))))
