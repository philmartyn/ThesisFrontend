(ns thesis-frontend.handler
  (:gen-class)
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [langohr.basic :as lb]
            [langohr.channel :as lch]
            [langohr.core :as rmq]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [thesis-frontend.page-layout :refer [main-page]]
            [thesis-frontend.pdf :as pdf])) 


(def amqp-url (or (System/getenv "AMQP_URL") 
                  "amqp://guest:guest@localhost:5672"))

(defn to-json-string 
  [filename] 
  (json/write-str {:filename filename}))

(defn read-bytes->json
  "Converts a byte array to String to JSON"
  [^bytes response-data]
  (when response-data 
    (json/read-str (String. response-data "UTF-8"))))

(defn sleep-thread 
  []
  (Thread/sleep 20000))

(defn publish-and-get-response
  "Put filename onto queue and get the response back."
  [ch filename]
  
  (lb/publish ch "" "clj->py" (to-json-string filename) {:content-type "application/json"})
  (sleep-thread)
  (lb/get ch "py->clj"))

(defroutes app-routes
           (GET "/" [] (main-page))

           (POST "/file"
                 {{{tempfile :tempfile filename :filename} :file} :params}

             (let [conn (rmq/connect {:uri amqp-url})
                   ch (lch/open conn)]

               (io/copy tempfile (io/file "resources" "public" filename))

               (let [[_metadata response-data] (publish-and-get-response ch filename)
                     ;response-data-json (read-bytes->json response-data)
                      ]
                 ;(prn "Response Data " response-data-json)
                 (rmq/close ch)
                 (rmq/close conn)
                 
                 (if response-data
                   (pdf/pdf-response response-data)
                   (main-page)))))
            
           (route/resources "/")
           (route/not-found "Not Found"))


(def app
  (let [config (-> api-defaults
                   (assoc-in [:security :anti-forgery] false)
                   (assoc-in [:params :multipart] true))]
    (-> app-routes
        (wrap-defaults config)
        (wrap-resource "public"))))

