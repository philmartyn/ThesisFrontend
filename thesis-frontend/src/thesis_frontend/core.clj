(ns thesis-frontend.core
  (:gen-class)
  (:require [langohr.core :as rmq]
            [ring.adapter.jetty :refer [run-jetty]]
            [thesis-frontend.message-broker :as mb]
            [thesis-frontend.handler :as handler]))


(defn shutdown 
  "Shutdown the server, queue connection and channel."
  []

  (Thread/sleep 5000)
  (println "Server disconnecting...")
  (rmq/close mb/channel-persist)
  (rmq/close mb/connection-persist))

(defn -main
  [& args]
  (let [port (Integer/valueOf (or (System/getenv "NII_PORT") "3006"))]
   (println "Connecting to port: " port)
   (mb/predictor-message-queue)
   (println "Connection and channel established.")
   (run-jetty handler/app {:port port :join false :async? true :async-timeout 60000})
   (shutdown)))