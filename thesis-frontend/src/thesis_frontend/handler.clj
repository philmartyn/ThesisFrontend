(ns thesis-frontend.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [langohr.basic :as lb]
            [clojure.data.json :refer [read-str
                                       write-str]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [thesis-frontend.page-layout :refer [main-page]]
            [thesis-frontend.message-broker :as mb]
            [thesis-frontend.pdf :as pdf]
            [clojure.java.io :as io])
  (:import (java.util UUID))) 


(defn to-json-string 
  [filename]
  (write-str {:filename filename}))


(defn read-json-bytes
  "Converts a byte array to String to JSON"
  [^bytes response-data] 
  (when response-data 
    (read-str (String. response-data "UTF-8"))))


(defn uuid 
[]
  (.toString (UUID/randomUUID)))


;; This is for ease of testing.
(defn ack 
  [channel delivery-tag]
  (lb/ack channel delivery-tag))


(defn nack 
  [channel delivery-tag]
  (lb/nack channel delivery-tag false true))


(defn timeout? [attempt]
  (> attempt 60))


(defn send-and-receive-rpc
  [filename request-correlation-id]
  (println (.getName (Thread/currentThread)))
  (let [_ (println "Correlation ID" request-correlation-id)
        
        channel @mb/channel-persist
        _ (println "Channel " channel)
       
        callback-queue @mb/callback-queue-persist
        _ (println "Callback Queue " callback-queue)
       
        queue-name (get callback-queue :queue)
        _ (println "Queue name " queue-name)
       
        _ (try 
            (lb/publish channel "" "clj->py" (to-json-string filename) {:content-type "application/json"
                                                                        :reply-to queue-name
                                                                        :correlation-id request-correlation-id})
            (catch Exception e (prn "Couldn't publish message to channel!" (.getMessage e))))
        ;; Loop to poll the message broker every second looking for response.
        ;; Didn't want to use subscribe here cause the get function 
        ;; seemed a lot simpler to wrap my head around and this method works well
        ;; with the POST handler as it blocks response from being sent back to the client. 
        response (loop [attempt 0]
                   (println "Attempt number: " attempt)
                   (println "Timing out: " (timeout? attempt))
                   
                   ;; Establish a 60 second timeout here.
                   (if-not (timeout? attempt)
                     
                     (do 
                       (Thread/sleep 1000)
                       (if-let [[{:keys [correlation-id 
                                         delivery-tag] :as metadata} response] (lb/get channel queue-name false)]
                         ;; If there's a response check if the correlation-ids match
                         (do 
                           (println "Response correlation id: " correlation-id)
                           (println "Correlation id match: " (= request-correlation-id correlation-id))
                           
                           (if (= request-correlation-id correlation-id)
                             ;; If they do ack the mesage and return the response.
                             (do
                               (ack channel delivery-tag)
                               response)
                             ;; Otherwise nack the message back onto the queue and recur
                             
                             (do
                               (println "Mismatched Correlation ID" correlation-id)
                               (nack channel delivery-tag)
                               (recur (inc attempt)))))
                         
                         (recur (inc attempt))))
                     :no-response))

        _ (println "After publish" response)]

    (if (keyword? response)
      response
      (read-json-bytes response))))


(defn copy-file 
  [source dest-path]
  (io/copy source (io/file dest-path)))


(def nii-file-path (or (System/getenv "EFS_FILE_PATH")
                       "/Users/pmartyn/Documents/College_Work/Thesis/Thesis-Frontend/thesis-frontend/efs/"))


(defroutes app-routes
           (GET "/" [] (main-page))

           (POST "/file" []
              (fn 
              [request response raise]
               (println "Request: " request)
               (println "Thread: " (.getName (Thread/currentThread)))
               
               (let [{{{tempfile :tempfile
                        filename :filename} :file} :params} request
                     _ (println "Filename: " filename)
                     
                     request-correlation-id (uuid)
                     
                     _ (try
                         (copy-file tempfile (str nii-file-path request-correlation-id ".nii"))
                         (catch Exception e (prn "Failed to save file: " (.getMessage e))))
                     
                     resp (delay (send-and-receive-rpc filename request-correlation-id))]
                 
                 (let [resp @resp]
                   (println "Response after async: " resp)
                 
                   (if (keyword? resp)
                     (response (main-page :error))
                     (response (pdf/pdf-response resp)))))))

           (route/resources "/")
           (route/not-found "Not Found"))


(def app
  (let [config (-> api-defaults
                   (assoc-in [:security :anti-forgery] false)
                   (assoc-in [:params :multipart] true))]
    (-> app-routes
        (wrap-defaults config)
        (wrap-resource "public"))))

