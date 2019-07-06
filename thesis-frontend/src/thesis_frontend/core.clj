(ns thesis-frontend.core
  (:gen-class)
  (:require [langohr.channel :as lch]
            [langohr.core :as rmq]
            [langohr.queue :as lq]
            [ring.adapter.jetty :refer [run-jetty]]
            [thesis-frontend.handler :as handler]))
   
(defn json-comm [channel]
  (do
    (lq/declare channel "jsonpy2clj" {:exclusive false :auto-delete false})
    (lq/declare channel "jsonclj2py" {:exclusive false :auto-delete false})
    (println (format "Clojure Connected. Channel id: %d" (.getChannelNumber channel)))))

(defn shutdown [channel connection]
  (Thread/sleep 5000) ;; wait for messages to finish.
  (println "Clojure Disconnecting...")
  (rmq/close channel)
  (rmq/close connection))

(defn -main
  [& args]
  (let [conn (rmq/connect {:uri handler/amqp-url})
        ch (lch/open conn)]
    (json-comm ch)
    (run-jetty handler/app {:port (Integer/valueOf (or (System/getenv "port") "3006")) :join true})
    (shutdown ch conn)))