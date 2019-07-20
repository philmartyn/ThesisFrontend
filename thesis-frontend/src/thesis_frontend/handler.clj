(ns thesis-frontend.handler
  (:gen-class)
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [clojure.tools.logging :only [info]]
            [langohr.basic :as lb]
            [clojure.data.json :only [json-str]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [thesis-frontend.page-layout :refer [main-page]]
            [thesis-frontend.message-broker :as mb]
            [thesis-frontend.pdf :as pdf])) 


(defn to-json-string 
  [filename] 
  (json/write-str {:filename filename}))


(defn read-bytes->json
  "Converts a byte array to String to JSON"
  [^bytes response-data] 
  (when response-data 
    (json/read-str (String. response-data "UTF-8"))))


;; Mostly here so I can redef for testing.
(defn sleep-thread 
  []
  (Thread/sleep 15000))


(defn publish-and-get-response
  "Put filename onto queue and get the response back."
  [ch filename]
  
  ;; Put the filename on the message queue to be sent to the predictor
  (lb/publish ch "" "clj->py" (to-json-string filename) {:content-type "application/json"})
  ;; Sleep the thread until the predictor has finished, better to do this properly with an async handler
  (sleep-thread)
  ;; Get the results off the queue. Could use async subscribe function here. Get is simpler though.  
  (lb/get ch "py->clj"))


(defroutes app-routes
           (GET "/" [] (main-page))

           (POST "/file"
                 {{{tempfile :tempfile filename :filename} :file} :params}

               ;; Save the temp file upload to disk 
               (io/copy tempfile (io/file "resources" "public" filename))
               ;; Publish and get the response data
               (let [[_metadata response-data] (publish-and-get-response mb/channel filename)
                     response-data-json (read-bytes->json response-data)]
                 (prn "Response Data " response-data-json)
                 
                 ;; Print the results as a PDF
                 (if response-data
                   (pdf/pdf-response response-data-json)
                   (main-page))))
            
           (route/resources "/")
           (route/not-found "Not Found"))


(def app
  (let [config (-> api-defaults
                   (assoc-in [:security :anti-forgery] false)
                   (assoc-in [:params :multipart] true))]
    (-> app-routes
        (wrap-defaults config)
        (wrap-resource "public"))))

