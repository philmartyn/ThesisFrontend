(ns thesis-frontend.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [ring.adapter.jetty :refer [run-jetty]]
            [clj-http.client :as http]
            [clojure.java.io :as io]
            [thesis-frontend.handler :refer :all]
            [langohr.basic :as lb]))

(declare test-port)

(defn with-server
  [f]
  (let [server (run-jetty app {:port 1234 :join? false :async? true})
        port (-> server .getConnectors first .getLocalPort)]
    (with-redefs [test-port port]
      (try
        (f)
        (finally
          (.stop server))))))
          

(def tmp-file
  (clojure.java.io/file "/Users/pmartyn/Documents/College_Work/Thesis/Thesis-Frontend/thesis-frontend/test/thesis_frontend/test.txt"))


(defn url [relative]
 (str "http://localhost:" test-port relative))


(defn mock-post []
  (http/post (url "/file")
             {:async? true
              :multipart [{:name "file" :content (clojure.java.io/file tmp-file)}]}
             ;; respond callback
             (fn [response] response)
             ;; raise callback
             (fn [exception] (println "exception message is: " (.getMessage exception)))))

(defn str->byte-array
  [^String s]
  (when s 
    (.getBytes s "ASCII")))

(def main-page-response "<!DOCTYPE html>\n<html><head><title>Bipolar Prediction Service</title></head><link href=\"/css/home-layout.css\" rel=\"stylesheet\" type=\"text/css\"><body><div id=\"text-box\"><h1>Bipolar Prediction Service</h1><h2>Upload NII file</h2><div><form action=\"/file\" enctype=\"multipart/form-data\" method=\"post\"><input id=\"file\" name=\"file\" size=\"20\" type=\"file\"><input id=\"submit\" name=\"submit\" type=\"submit\" value=\"submit\"></form></div></div></body></html>")


(defn get-status
  [response]
  (.toString (.getStatusLine (.get response))))

(deftest test-app
  (let [nii-file  "/Users/pmartyn/Documents/College_Work/Thesis/Thesis-Frontend/thesis-frontend/efs/12345.nii"]
   (testing "main page"
     (let [response (app (mock/request :get "/"))]
       (is (= (:status response) 200))
       (is (= (:body response) main-page-response))))

   (testing "post"
     (let [response-from-queue (str->byte-array "[{\"filename\" \"1.jpg\" \"class-label\" \"bipolar\" \"class-probability\" \"100.00\"}]")]

       ;; Mock out the queuing section
       (with-redefs [lb/publish (constantly nil)
                     lb/get (constantly [{:correlation-id "12345"} response-from-queue])
                     ack (constantly true)
                     uuid (constantly "12345")]

         (testing "happy path - successful response"
           (let [response (mock-post)
                 expected-resp-rext (re-pattern "Content-Disposition: filename=data-response.pdf, Content-Type: application/pdf")
                 actual-resp-text (str (.get response))]
             (println "Response text: " actual-resp-text)
             (is (= (.toString (.getStatusLine (.get response))) "HTTP/1.1 200 OK"))
             (is (= (.exists (io/as-file nii-file)) true))

             ;; Check the PDF is generated
             (is (re-find expected-resp-rext actual-resp-text))))


         (testing "no response"
           (with-redefs [timeout? (constantly true)]
             (let [response (mock-post)
                   expected-resp-str (re-pattern "Content-Type: text/html")
                   actual-resp-str (str (.get response))]
               (is (= (get-status response) "HTTP/1.1 200 OK"))
               (is (re-find expected-resp-str actual-resp-str))))))))

   (testing "not-found route"
     (let [response (app (mock/request :get "/Invalid"))]
       (is (= (:status response) 404))))

   (io/delete-file nii-file)))


(deftest to-json-string-test
  (is (= (to-json-string "nii-file-12345") "{\"filename\":\"nii-file-12345\"}"))
  (is (= (to-json-string "2006-CHMPRAGE-98.nii") "{\"filename\":\"2006-CHMPRAGE-98.nii\"}")))


(deftest read-bytes->json-test
  (is (= (read-json-bytes (str->byte-array "123")) 123))
  (is (= (read-json-bytes (str->byte-array "[{\"results\" \"1234\"} {\"results\" \"9876\"}]")) [{"results" "1234"} {"results" "9876"}])))

(deftest timeout?-test
  (is (= (timeout? 61) true))
  (is (= (timeout? 59) false)))

(use-fixtures :once with-server)