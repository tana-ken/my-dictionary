(ns my-dictionary.core
  (:require
   :reload-all
   [clojure.tools.logging :as logging]
   [clj-http.client :as http-client]
   [clj-xpath.core :as xpath]
   [clj-json.core :as json])
  (:import
   (java.io.IOException)
   (org.xml.sax.SAXException)
   (myDictionary.java.AppException)))

(defn handle-exception
  "to throw application exception"
  [e]
  (do (logging/error (.getMessage e))
    (throw (myDictionary.java.AppException. (.getMessage e) (.getCause e)))))

(defn load-properties
  "to load properties from external file"
  [file-path]
  (read-string (try
                 (slurp file-path)
                 (catch java.io.IOException ioe (handle-exception ioe)))))

(def property-file-path "private/properties.s")

;;; cache properties
(def properties (load-properties property-file-path))

(defn build-url
  "to build url string with parameters"
  [host vid q type site]
   (str "http://" host "/v001?vid=" vid "&q=" q "&type=" type "&site=" site))

(defn extract-xml
  "to send request and extract xml part from reply"
  [url]
  (:body (try
           (http-client/get url)
           (catch java.io.IOException ioe (handle-exception ioe)))))

(defn generate-json
  "to return a hashmap including word and entries"
  [xml-string]
  ;; local function to call xpath/$x safely
  (letfn [(safety-xpath-call
            [xpath xml]
            (try
              (xpath/$x xpath xml)
              (catch org.xml.sax.SAXException saxe (handle-exception saxe))))] ; end of letfn safety-xpath-call
    (let [word (:query (:attrs (first (safety-xpath-call "//dictionary" xml-string))))
          entries (let [pos (for [line (safety-xpath-call "//partofspeech" xml-string)] (:pos (:attrs line)))] ; end of let pos

    ; local function to extract mean from //partofspeach/defset/def
      (letfn [(get-mean
                [each-pos xml-string]
                (for [line (safety-xpath-call (str "//partofspeech[@pos=\"" each-pos "\"]/defset/def") xml-string)] (:text line)))] ; end of letfn get-mean

        ; generating entries
        (for [i pos]
          (for [j (get-mean i xml-string)] {:pos i :mean j}))))] ; end of let word, entries

      ; get-dictionary-define-json (cont.)
      (json/generate-string {:word word :entries (flatten entries)}))))

(defn from-build-url-to-generate-json
  "combining three functions"
  [word]
  (-> (build-url "api-pub.dictionary.com" (:vid properties) word "define" "dictionary")
      (extract-xml ,)
      (generate-json ,)))
