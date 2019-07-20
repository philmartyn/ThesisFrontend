(ns thesis-frontend.page-layout
  (:require [hiccup.page :refer [html5 include-css]]))

(defn header 
  "Creates the header for the body of the main page."
  [& body]
  (html5
    [:head
     [:title "Bipolar Prediction Service"]]
    (include-css "/css/home-layout.css")
    [:body body]))

(defn upload-form 
  "Create the file upload and submit button for the main page."
  []
  [:div#text-box
   [:h1 "Bipolar Prediction Service"]
   [:h2 "Upload NII file"]
   [:div
    [:form {:action "/file" :method "post" :enctype "multipart/form-data"}
     [:input#file {:name "file" :type "file" :size "20"}]
     [:input#submit {:type "submit" :name "submit" :value "submit"}]]]])

(defn main-page 
  "Creates the main page of the app."
  []
  (header (upload-form)))