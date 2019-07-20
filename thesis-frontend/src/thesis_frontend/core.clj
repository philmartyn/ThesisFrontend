(ns thesis-frontend.core
  (:gen-class)
  (:require [langohr.core :as rmq]
            [ring.adapter.jetty :refer [run-jetty]]
            [thesis-frontend.message-broker :as mb]
            [thesis-frontend.handler :as handler]))


(defn shutdown 
  "Shutdown the server, queue connection and channel."
  [channel connection]

  (Thread/sleep 5000) ;; wait for messages to finish.
  (println "Clojure Disconnecting...")
  (rmq/close channel)
  (rmq/close connection))

(defn -main
  [& args]
  
    (mb/predictor-message-queue)
    (run-jetty handler/app {:port (Integer/valueOf (or (System/getenv "NII_PORT") "3006")) :async? true  :join true})
    (shutdown mb/channel mb/conn))