(ns thesis-frontend.message-broker
  (:require [langohr.queue :as lq]
            [langohr.channel :as lch]
            [langohr.core :as rmq])
  (:import (java.net ConnectException)))


(def amqp-url (or (System/getenv "AMQP_URL") 
                  "amqp://guest:guest@localhost:5672"))

(defonce conn (try
                (rmq/connect {:uri amqp-url})
                (catch ConnectException e (prn (str "Connection not established with message broker: "
                                                (.getMessage e))))))

(defonce channel (try 
                   (lch/open conn)
                   (catch Exception e (prn (str "Channel could not be opened: " 
                                                (.getMessage e))))))

(defn predictor-message-queue 
  "Setup the channel queue for the nii predictor."
  []
  
  (do
    (lq/declare channel "py->clj" {:exclusive false :auto-delete false})
    (lq/declare channel "clj->py" {:exclusive false :auto-delete false})
    (println (format "Frontend Connected. Channel id: %d" (.getChannelNumber channel)))))