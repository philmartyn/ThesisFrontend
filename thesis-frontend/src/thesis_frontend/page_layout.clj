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
    (include-css "/css/home.css")
    [:body body]))

(defn custom-input [type name label placeholder]
  [:div
   [:label label
    [:input {:type type :name name :placeholder placeholder}]]])

(defn registeration-form []
  [:div
   [:h1 "Upload NII data here."]
   
   
   
   #_(form-to [:post "/"]
            (file-upload "NII")
            (custom-input "text" "first-name" "First name" "First name")
            (custom-input "text" "last-name" "Last name" "Last name")
            (custom-input "text" "email" "Email" "Your email")
            (submit-button "Submit"))
   
   
   [:form {:action "/file" :method "post" :enctype "multipart/form-data"}
    [:input {:name "file" :type "file" :size "20"}]
    [:input {:type "submit" :name "submit" :value "submit"}]]
    
   [:form {:action "/predict" :method "get"}
    [:input {:type "submit" :name "predict" :value "predict"}]]
  
  ])

  

(defn result [form-params]
  [:div
   [:h1 "Form data sent to the server"]
   [:div form-params ]
   
   #_(for [[field-name field-value] form-params]
     (do (print field-name)
      
      
      ))])