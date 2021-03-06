(defproject thesis-frontend "0.1.0"
  :description "Web client and server for NII predictor."
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :source-paths ["src"]
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [compojure "1.6.1"]
                 [hiccup "1.0.5"]
                 [lein-ring "0.12.5"]
                 [ring-server "0.5.0"]
                 [org.clojure/data.json "0.2.6"]
                 [ring/ring-jetty-adapter "1.6.3"]
                 [ring/ring-json "0.4.0"]
                 [clj-pdf "2.3.5"]
                 [com.novemberain/langohr "5.1.0"]
                 [ring/ring-defaults "0.3.2"]]
  :plugins [[lein-ring "0.12.5"]
            [compojure "1.6.1"]]
  :main thesis-frontend.core 
  :aot [thesis-frontend.core
        thesis-frontend.handler
        thesis-frontend.message-broker
        thesis-frontend.pdf
        thesis-frontend.page-layout]
  :ring {:handler thesis-frontend.handler/app
         :auto-refresh? true
         :nrepl {:start? true}}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-devel "1.7.1"]
                        [ring/ring-mock "0.3.2"]]}})
