(defproject my-dictionary "0.1.0-SNAPSHOT"
  :description "A tiny web application for requesting Dictionary.com web api"
  :url "http://"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
    [org.clojure/clojure "1.4.0"]
    [org.clojure/tools.logging "0.2.3"]
    [log4j/log4j "1.2.16"]
    [log4j/apache-log4j-extras "1.1"]
    [clj-http "0.4.3"]
    [clj-json "0.5.0"]
    [clj-xpath "1.3.0"]
    [compojure "1.1.1"]
    [ring/ring-jetty-adapter "1.1.1"]
    [clj-time "0.4.4"]
    [com.datomic/datomic-free "0.8.3488"]]
  :ring {:handler my-dictionary.core/interface-for-client}
)
