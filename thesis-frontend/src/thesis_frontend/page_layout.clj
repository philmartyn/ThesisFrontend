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
  [error?]
  (let [wrapper [:div#text-box]
        title-line [[:h1 "Bipolar Prediction Service"]]
        subtitle-line [[:h2 "Upload NII file"]]
        error-line [[:h2 "Something went wrong, please try again!"]]
        upload-section [[:div
                         [:form {:action "/file" :method "post" :enctype "multipart/form-data"}
                          [:input#file {:name "file" :type "file" :size "20"}]
                          [:input#submit {:type "submit" :name "submit" :value "submit"}]]]]]

    (into [] 
          (if error?
            (concat wrapper title-line subtitle-line error-line upload-section)
            (concat wrapper title-line subtitle-line upload-section)))))

(defn main-page 
  "Creates the main page of the app."
  ([]
   (header (upload-form false)))
  ([error?]
   (header (upload-form error?))))