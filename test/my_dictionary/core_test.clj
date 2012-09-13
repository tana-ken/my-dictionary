(ns my-dictionary.core-test
  (:use
   :reload-all
   [my-dictionary.core]
   [clojure.test])
  (:require
   [clj-json.core :as json]
   [clj-time.core :as time]
   [clj-time.coerce :as time-coerce]
   [datomic.api :as d])
  (:import
   (myDictionary.java.AppException)
   (java.lang.RuntimeException)))

(def test-properties (load-properties property-file-path)) 

(def test-dictionary-url (str
                "http://api-pub.dictionary.com/v001?vid="
                (:vid test-properties)
                "&q=test&type=define&site=dictionary"))

(def test-dictionary-xml (slurp "test/data/test-dictionary.xml"))

(def test-thesaurus-xml (slurp "test/data/test-thesaurus.xml"))

(def test-slang-xml (slurp "test/data/test-slang.xml"))

(def test-etymology-xml (slurp "test/data/test-etymology.xml"))

(def test-example-xml (slurp "test/data/test-example.xml"))

(def test-questionanswer-xml (slurp "test/data/test-questionanswer.xml"))

(def test-synonym-xml (slurp "test/data/test-synonym.xml"))

(def test-random-dictionary-xml (slurp "test/data/test-random-dictionary.xml"))

(def test-random-thesaurus-xml (slurp "test/data/test-random-thesaurus.xml"))

(def test-spelling-xml (slurp "test/data/test-spelling.xml"))

(def test-thesaurus-xml (slurp "test/data/test-wotd.xml"))

(def test-dictionary-result {:word "test", :entries '({:pos "noun", :text "the means used to determine the quality, content, etc., of something"} {:pos "noun", :text "examination to evaluate a student or class"} {:pos "verb (used with object)", :text "to subject to a test"})})

(def test-example-result {:word "test", :entries '("noun : to put to the test.  <br>verb (used without object) : People test better in a relaxed environment. <ex>,</ex>to test for diabetes.  <br>.")})

(def test-spelling-result {:word "teest", :entries '("testy" "tees" "weest" "deist" "doest" "toast" "reset" "retest" "tersest" "truest" "teased" "teat" "tee's" "teed" "tests" "Tevet" "detest" "tenet" "tester" "tweet" "teats" "Tet" "taste" "tasty" "EST" "beset" "desert" "est" "tamest" "DST" "teds" "Tess" "Tues" "teas" "text" "ties" "toes" "attest" "Ted's")})

(deftest test-load-properties
  (is (load-properties "test/data/test.s")
      {:test "test"}))

(deftest test-load-properties-ioe
  (is (thrown? myDictionary.java.AppException
               (load-properties "wrong-file-path"))))

(deftest test-load-properties-re
  (is (thrown? java.lang.RuntimeException
               (load-properties "test/data/test-wrong.s"))))

(deftest test-build-url-with-prms
  (is (build-url-with-prms
            root-url
            (list
             [:vid (:vid test-properties)] [:q "test"] [:type "define"] [:site "dictionary"]))
      test-dictionary-url))

(deftest test-get-body
  (is (get-body test-dictionary-url)
      test-dictionary-xml))

(deftest test-get-body-ioe
  (is (thrown? myDictionary.java.AppException
              (get-body ""))))

(deftest test-extract-dictionary
  (is (extract-dictionary test-dictionary-xml)
      test-dictionary-result))

(deftest test-extract-example
  (is (extract-example test-example-xml)
      test-example-result))

(deftest test-extract-spelling
  (is (extract-spelling test-spelling-xml)
      test-spelling-result))

(deftest test-call-api
  (is (call-api root-url (list [:vid (:vid test-properties)] ["q" "test"] ["type" "define"] ["site" "dictionary"]) extract-dictionary)
     (json/generate-string test-dictionary-result)))


(deftest test-interfece-for-client-dictionary
  (is (:status (interface-for-client {:request-method :get :uri "/dictionary/define/test"}))
      200))

(deftest test-interface-for-client-404
  (is (interface-for-client {:request-method :get :uri ""})
      {:status 404, :headers {"Content-Type" "text/html; charset=utf-8"}, :body "Page not found"}))

(def test-url "datomic:mem://test")

(defn pre-test-datomic
  ""
  []
  (do (d/gc-storage (d/connect test-url) (time-coerce/to-date (time/now)))
      (d/delete-database test-url)
      (d/create-database test-url)
      (add-attribute-work-word (d/connect test-url))
      (add-attribute-work-time (d/connect test-url))))

(deftest test-add-a-work
  (is
    (do (pre-test-datomic)
        (add-a-work (d/connect test-url) "test" (time-coerce/to-date (time/now)))
        ((first (find-all (d/connect test-url))) 0))
    "test"))

(deftest test-today?
  (today? (time-coerce/to-date (time/now))))

(deftest test-limit?
  (is
    (do (pre-test-datomic)
        (add-a-work (d/connect test-url) "test" (time-coerce/to-date (time/now)))
        [(limit? 1 (d/connect test-url)) (limit? 2 (d/connect test-url))])
    [false true]))