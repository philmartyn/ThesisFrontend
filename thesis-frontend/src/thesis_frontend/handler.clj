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
            [thesis-frontend.page-layout :as layout])) 


(defn format-file-string
"Formats the JSON to a more readble and writeable to file format."
  [response]
  (reduce (fn [acc {:strs [filename class-label class-probability]}]
            (str acc "\nFilename: " filename "\nCLass-Label: " class-label "\nClass-Probability: " class-probability "\n"))
          ""
          response))

(def amqp-url (get (System/getenv) "CLOUDAMQP_URL" "amqp://guest:guest@localhost:5672"))

(defn jsonstring 
  [filename] 
  (json/write-str {:filename filename}))

(defn main-page []
  (layout/common (layout/upload-form)))

(defn read-bytes->json
  "Converts a byte array to String to JSON"
  [^bytes response-data]
  (json/read-str (String. response-data "UTF-8")))

(defroutes app-routes
           (GET "/" [] (main-page))

           (POST "/file"
                 {{{tempfile :tempfile filename :filename} :file} :params}

             (let [conn (rmq/connect {:uri amqp-url})
                   ch (lch/open conn)]

               (io/copy tempfile (io/file "resources" "public" filename))

               (lb/publish ch "" "jsonclj2py" (jsonstring filename) {:content-type "application/json"})
               (Thread/sleep 20000)
               (let [[_metadata response-data] (lb/get ch "jsonpy2clj")
                     response-data-json (read-bytes->json response-data)]
                 (prn "Response Data " response-data-json)
                 (rmq/close ch)
                 (rmq/close conn)

                 {:status 200
                  :headers {}
                  :body (format-file-string response-data-json)})))
            
           (route/resources "/")
           (route/not-found "Not Found"))


(def app
  (let [config (-> api-defaults
                          (assoc-in [:security :anti-forgery] false)
                          (assoc-in [:params :multipart] true))]
    (-> app-routes
        (wrap-defaults config)
        (wrap-resource "public"))))

