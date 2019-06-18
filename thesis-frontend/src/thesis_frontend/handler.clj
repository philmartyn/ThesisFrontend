(ns thesis-frontend.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [thesis-frontend.page-layout :as layout]
            [compojure.response :as resp]
            [ring.util.response :refer :all]
            [thesis-frontend.predict :as predict]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [clojure.java.io :as io])
           ) 

(def filename-storage (atom ""))

(defn form-page []
  (layout/common (layout/registeration-form)))

(defn result-page [filename #_result #_{:keys [form-params]}]
  (layout/common (layout/result (str filename) #_form-params)))

(defroutes app-routes
           ;(GET "/" [] "Hello World this is this")
           (GET "/" [] (form-page))
           (GET "/predict" [] (-> @filename-storage
                                  (predict/predict-mri)
                                  (result-page)))
           (POST "/file"
                 {{{tempfile :tempfile filename :filename} :file} :params :as params}
             (do (reset! filename-storage filename)
                 (io/copy tempfile (io/file "resources" "public" filename))))
             
           (route/resources "/")
           (route/not-found "Not Found"))


(def app
  (wrap-defaults app-routes (-> api-defaults 
                                (assoc-in [:params :multipart] true)
                                (assoc-in [:responses :not-modified-responses] false))))
