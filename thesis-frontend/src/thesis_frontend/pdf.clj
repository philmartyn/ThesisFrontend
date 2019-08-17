(ns thesis-frontend.pdf
  (:require [ring.util.response :as resp]
            [clj-pdf.core :as pdf])
  (:import (java.io ByteArrayInputStream ByteArrayOutputStream)))


(defn generate-pdf-response 
  "Create the PDF to return to be sent to the web client. Creates a table layout for each of the images results
  returned from the predictor backend
  
  <out>             ByteArrayOutputStream to hold the PDF data.
  <response-data>   A vector of maps holding the response data from the iamge predictor.
                    Looks like:
                    [{\"filename\" \"1.jpg\" \"class-label\" \"bipolar\" \"class-probability\" \"100.00\"}... <multiple maps>]
  
  Returns <out>."
  [out response-data]
  
  (let [value-or-empty-string (fn [value]
                                (if-not value
                                  ""
                                  (str value)))
                                  
        ;; Create the individual result tables rows.
        create-pdf-table-elements (reduce 
                                    (fn [acc {:strs [filename class-label class-probability]}]
                                      (conj acc [["File Slice:" "" [:cell {:align :right} (value-or-empty-string filename)]]
                                                 ["Class: " "" [:cell {:align :right} (value-or-empty-string class-label)]]
                                                 ["Probability: " [:cell {:align :right :colspan 2} (format "%.7s" (value-or-empty-string class-probability))]]
                                                 [[:cell {:colspan 3 :align :center} "--------------------------------"]]]))
                                    []
                                    response-data)]
    (pdf/pdf
     [{}
      (concat
        [:table {:header ["" [:cell {:size 15 :align :center} "Results"] ""] :align :center :width 50 :border false :cell-border false :spacing -3}]
        (apply concat 
               (concat create-pdf-table-elements)))]
     out)))
     
     
(defn write-pdf-response
  "Creates the Ring response for the PDF."
  [result-bytes]
  
  (with-open [in (ByteArrayInputStream. result-bytes)]
    (-> (resp/response in)
        (resp/header "Content-Disposition" "filename=data-response.pdf")
        (resp/header "Content-Length" (count result-bytes))
        (resp/content-type "application/pdf"))))


(defn pdf-response
  "Creates the PDF results page sent back to the client."
  [response-data]
  
  (try
    (let [out (ByteArrayOutputStream.)]
      (generate-pdf-response out response-data)
      (write-pdf-response (.toByteArray out)))
    (catch Exception ex
      (prn {:error (.getMessage ex)}))))
      
      
      
