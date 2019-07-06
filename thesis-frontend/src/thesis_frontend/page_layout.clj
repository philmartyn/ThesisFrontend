(ns thesis-frontend.page-layout

  (:require [hiccup.page :refer [html5 include-css]])
  (:require [hiccup.form :refer [form-to submit-button file-upload]])
  (:require [thesis-frontend.helpers :as l]
  )
  )

(defn common [& body]
  (html5
    [:head
     [:title "Bipolar Prediction Service"]]
    (include-css "/css/home-layout.css")
    [:body body]))

;(defn custom-input [type name label placeholder]
;  [:div
;   [:label label
;    [:input {:type type :name name :placeholder placeholder}]]])

(defn upload-form []
  [:div#text-box
   [:h1 "Bipolar Prediction Service"]
   [:h2 "Upload NII file"]
   [:div
    [:form {:action "/file" :method "post" :enctype "multipart/form-data"}
     [:input#file {:name "file" :type "file" :size "20"}]
     [:input#submit {:type "submit" :name "submit" :value "submit"}]]]])

  

;(defn result [form-params]
;  [:div 
;   [:h1 "Form data sent to the server"]
;   ;[:div form-params ]
;   [:ul
;    (for [{:strs [filename class-label class-probability]} form-params]
;      [:li [:li "Filename: " filename] [:li "Class Label: " class-label] [:li "Class Probability: " class-probability]]
;      )]])