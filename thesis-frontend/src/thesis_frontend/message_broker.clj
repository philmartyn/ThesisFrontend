(ns thesis-frontend.message-broker
  (:require [langohr.queue :as lq]
            [langohr.channel :as lch]
            [thesis-frontend.retries :as retry]
            [langohr.core :as rmq])
  (:import (java.net ConnectException)
           (clojure.lang ExceptionInfo)))


(def amqp-url (or (System/getenv "AMQP_URL") 
                  "amqp://guest:guest@localhost:5672"))


(def channel-persist (atom nil))
(def connection-persist (atom nil))
(def callback-queue-persist (atom nil))

(defn predictor-message-queue 
  "Setup the connection, channel & queue for the NII predictor."
  []
  (println "Establishing connection and channel to message broker. " amqp-url)
  (let [retry-delay-ms 500
        max-retry-attempts 5
        delay-algo (retry/multiplicative-delay retry-delay-ms 2)

        delay-opts {:delay-calc delay-algo
                    :retry-fn! (fn [attempt]
                                 (prn "Retrying connection in milliseconds..."
                                       (delay-algo attempt)))
                    :halt-on (fn [ex]
                               (and (instance? ExceptionInfo ex)
                                    (not (instance? ConnectException (.getCause ex)))))}
        
  
        conn (try
               (retry/with-max-error-retries delay-opts max-retry-attempts [ConnectException]
                                             (rmq/connect {:uri amqp-url :requested-heartbeat 0}))
               (catch ConnectException e (prn (str "Connection not established with message broker: ")
                                              (.getMessage e))))
        
        _ (reset! connection-persist conn)     
          
        channel (try
                  (lch/open conn)
                  (catch Exception e (prn (str "Channel could not be opened: " 
                                               (.getMessage e)))))                       
        
        _ (reset! channel-persist channel)
        
        callback-q (lq/declare channel "" {:auto-delete false :exclusive true})
        
        _ (reset! callback-queue-persist callback-q)]
    (println (format "Frontend Connected. Channel id: %d" (.getChannelNumber channel)))))