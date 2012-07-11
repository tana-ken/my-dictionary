(ns my-dictionary.core-test
  (:use
   [my-dictionary.core]
   [clojure.test :only (deftest is)])
  (:require
   :reload-all
   [clojure.test :as test]
   [my-dictionary.core :as core])
  (:import
   (myDictionary.java.AppException)
   (java.lang.RuntimeException)))

(def test-url (str
                "http://api-pub.dictionary.com/v001?vid="
                (:vid properties)
                "&q=test&type=define&site=dictionary"))

(def test-xml (slurp "test/data/test.xml"))

(def result-string "{\"word\":\"test\",\"entries\":[{\"pos\":\"noun\",\"mean\":\"the means used to determine the quality, content, etc., of something\"},{\"pos\":\"noun\",\"mean\":\"examination to evaluate a student or class\"},{\"pos\":\"verb (used with object)\",\"mean\":\"to subject to a test\"}]}")

(deftest test-load-properties
  (is (load-properties "test/data/test.s")
      {:test "test"}))

(deftest test-load-properties-ioe
  (is (thrown? myDictionary.java.AppException
               (load-properties "wrong-file-path"))))

(deftest test-load-properties-re
  (is (thrown? java.lang.RuntimeException
               (load-properties "test/data/test-wrong.s"))))

(deftest test-build-url
  (is (build-url
            "api-pub.dictionary.com"
            (:vid core/properties)
            "test"
            "define"
            "dictionary")
      test-url)) 

(deftest test-extract-xml
  (is (extract-xml test-url)
      test-xml))

(deftest test-extract-xml-ioe
 (is (thrown? myDictionary.java.AppException
              (extract-xml ""))))  

(deftest test-generate-json
  (is (generate-json test-xml)
      result-string))

(deftest test-generate-json-saxe
 (is (thrown? myDictionary.java.AppException
              (generate-json ""))))

(deftest test-from-build-to-get-dictionary-define-json
  (is (from-build-url-to-generate-json "test")
      result-string))

(deftest test-interfece-for-client
  (is (interface-for-client {:request-method :get :uri "/dictionary/define/test"})
      {:status 200, :headers {"Content-Type" "text/html; charset=utf-8"}, :body "{\"word\":\"test\",\"entries\":[{\"pos\":\"noun\",\"mean\":\"the means used to determine the quality, content, etc., of something\"},{\"pos\":\"noun\",\"mean\":\"examination to evaluate a student or class\"},{\"pos\":\"verb (used with object)\",\"mean\":\"to subject to a test\"}]}"}))

(deftest test-interface-for-client-404
  (is (interface-for-client {:request-method :get :uri ""})
      {:status 404, :headers {"Content-Type" "text/html; charset=utf-8"}, :body "Page not found"}))