(ns my-dictionary.core
  (:use [datomic.api :only [db q] :as d])
  (:require
   :reload-all
   [clojure.tools.logging :as logging]
   [clojure.string :as string]
   [clj-http.client :as http-client]
   [clj-xpath.core :as xpath]
   [clj-json.core :as json]
   [compojure.core :as compojure-core]
   [compojure.route :as compojure-route]
   [clj-time.core :as time]
   [clj-time.coerce :as time-coerce])
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

(defn build-url-with-prms
  "to build url string with parameters"
  [url prms]
  (str url (string/replace-first (apply str (for [[k v] prms] (str "&" (name k) "=" v))) "&" "?")))

(defn get-body
  "to send request and get body element of the response"
  [url]
  (:body (try
           (http-client/get url)
           (catch java.io.IOException ioe (handle-exception ioe)))))

(defn safety-xpath-call
  "to call xpath safely"
  [xpath xml]
  (try
    (xpath/$x xpath xml)
    (catch org.xml.sax.SAXException saxe (handle-exception saxe))))

(defn get-result
   "to extract text from a xml-string"
   [xpath xml-string key-list]
   (for [line (safety-xpath-call xpath xml-string)]
     (reduce get line key-list))) 

(defn- get-query-word
  "to extract query word from a xml-string"
  [xml-string]
  (first (get-result "(/*)[1]" xml-string '(:attrs :query))))

(defn- extract-dictionary-sub
;  "to extract word and entries from a xml-string of thesaurus"
  [xml-string]
  {:word (get-query-word xml-string)
   :entries
     (flatten
       (for [pos (get-result "//partofspeech" xml-string '(:attrs :pos))]
         (for [line (get-result (str "//partofspeech[@pos=\"" pos "\"]/defset/def") xml-string '(:text))] {:pos pos :text line})))})

(defn- convert-to-table-data
  [entry]
  {:c [{:v (:pos entry)} {:v (:text entry)}]})

(defn- create-data
  [entries]
  {:cols
   [{:id "pos" :label "POS" :type "string"}
    {:id "text":label "Meaning" :type "string"}]
   :rows (vec (for [entry entries] (convert-to-table-data entry)))})

(defn extract-dictionary
  ""
  [xml-string]
  (create-data (:entries (extract-dictionary-sub xml-string))))

(defn extract-thesaurus
  "to extract word and entries from a xml-string of thesaurus"
  [xml-string]
  ("have not implemented yet"))

(defn extract-slang
  "to extract word and entries from a xml-string of slang"
  [xml-string]
  ("have not implemented yet"))

(defn extract-etymology
  "to extract word and entries from a xml-string of etymology"
  [xml-string]
  ("have not implemented yet"))

(defn extract-example
  "to extract word and entries from a xml-string of example"
  [xml-string]
  {:word (get-query-word xml-string)
   :entries (get-result "//example" xml-string '(:text))})
  
(defn extract-questionanswer
  "to extract word and entries from a xml-string of questionanswer"
  [xml-string]
  ("have not implemented yet"))

(defn extract-synonyms
  "to extract word and entries from a xml-string of synonyms"
  [xml-string]
  ("have not implemented yet"))

(defn extract-random
  "to extract word from a xml-string of random"
  [xml-string site]
  {:word (first (get-result (str "//" site  "/random_entry") xml-string '(:text)))})

(defn extract-spelling
  "to extract word and entries from a xml-string of spelling"
  [xml-string]
  {:word (get-query-word xml-string)
   :entries (get-result "//suggestion" xml-string '(:text))})

(defn extract-wotd
  "to extract word and entries from a xml-string of woth"
  [xml-string]
  ("have not implemented yet"))

;(def uri "datomic:free://localhost:4334//test")
(def uri "datomic:mem://my-dictionary")

(d/create-database uri)

(def conn
  (d/connect uri))

(defn add-attribute-work-word
  ""
  [cnct]
  (d/transact cnct [{
                     :db/id #db/id[:db.part/db]
                     :db/ident :work/word
                     :db/valueType :db.type/string
                     :db/cardinality :db.cardinality/one
                     :db/doc "A word"
                     :db.install/_attribute :db.part/db}]))

(defn add-attribute-work-time
  ""
  [cnct]
  (d/transact cnct [{
                     :db/id #db/id[:db.part/db]
                     :db/ident :work/time
                     :db/valueType :db.type/instant
                     :db/cardinality :db.cardinality/one
                     :db/doc "Time"
                     :db.install/_attribute :db.part/db}]))

(add-attribute-work-word conn)
(add-attribute-work-time conn)

(defn add-a-work
  ""
  [cnct word date]
  (d/transact cnct [{:db/id #db/id[:db.part/user] :work/word word :work/time (time-coerce/to-date date)}]))

(defn find-all
  ""
  [cnct]
  (q '[:find ?n ?t :where [?e :work/word ?n] [?e :work/time ?t]] (db cnct)))

(defn today?
  ""
  [date]
  (let [now (time/now) today (time/date-time (time/year now) (time/month now) (time/day now))]
    (time/within? (time/interval today (time/plus today (time/days 1))) (time-coerce/from-date date)))) 

(defn find-today
  ""
  [cnct]
  (q '[:find ?n ?t :where [?e :work/word ?n] [?e :work/time ?t] [(my-dictionary.core/today? ?t)]] (db cnct)))

(defn limit?
  ""
  [n cnct]
  (> n (count (find-today cnct))))

(defn call-api
  ""
  [url prms extract-function]
  (if (limit? 50 conn)
    (do
      (add-a-work conn ((nth prms 1) 1) (time-coerce/to-date (time/now)))
      (-> (build-url-with-prms url prms)
          (get-body ,)
          (extract-function ,)
          (json/generate-string ,)))
    (json/generate-string (create-data '({:pos "" :text "I am sorry, this system is busy. Please access again tomorrow"})))))

(def root-url "http://api-pub.dictionary.com/v001")
(def common-headers {"Access-Control-Allow-Origin" "*" "Content-Type" "application/json"})

(compojure-core/defroutes interface-for-client
  ;
  (compojure-core/GET "/dictionary/:word" [word]
                      (merge {:headers common-headers}
                             {:body
                      (call-api root-url
                                (list
                                 [:vid (:vid properties)] ["q" word] ["type" "define"] ["site" "dictionary"])
                      extract-dictionary)}))
  ;
  (compojure-core/GET "/example/:word" [word]
                      (call-api root-url
                                (list
                                 [:vid (:vid properties)] ["q" word] ["type" "example"])
                       extract-example))
  ;
  (compojure-core/context "/random" []
                          ("/dictionary" []
                           (call-api root-url
                                     (list
                                      [:vid (:vid properties)] ["type" "random"] ["site" "dictionary"])
                            extract-random))
                          ("/thesaurus" []
                           (call-api root-url
                                     (list
                                      [:vid (:vid properties)] ["type" "random"] ["site" "thesaurus"])
                            extract-random)))
  ;
  (compojure-core/GET "/spelling/:word" [word]
                      (call-api root-url
                                (list
                                 [:vid (:vid properties)] ["q" word] ["type" "spelling"])
                                extract-spelling))

  ;
  (compojure-core/GET "/find-all" []
                      (str (find-all conn)))
  ;

  (compojure-route/not-found "Page not found"))