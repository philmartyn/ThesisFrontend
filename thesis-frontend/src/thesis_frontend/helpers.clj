(ns thesis-frontend.helpers
 (:use [clojure.string :only [trim-newline]]
        [clojure.pprint :only [code-dispatch pprint with-pprint-dispatch *print-right-margin*]]))


  
(defmacro spy
  "Evaluates expr and may write the form and its result to the log. Returns the
  result of expr. Defaults to :debug log level."
  ([expr]
   `(spy :debug ~expr))
  ([level expr]
   `(let [a# ~expr]
      (let [s# (with-out-str
                 (with-pprint-dispatch code-dispatch        ; need a better way
                                       (pprint '~expr)
                                       (print "=> ")
                                       (pprint a#)))]
        (trim-newline s#))
      a#)))