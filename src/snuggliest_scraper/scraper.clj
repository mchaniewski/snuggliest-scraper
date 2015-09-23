(ns snuggliest-scraper.scraper
  (:require
    [aleph.http :as http]
    [clojure.string :as str]
    [clojure.tools.logging :as log]
    [environ.core :refer [env]]
    [manifold.deferred :as d]
    [net.cgrand.enlive-html :as enlive]))

(def ^:dynamic *user-agent* 
  (str "SnuggliestScraper/" (env :snuggliest-scraper-version) " (+https://github.com/mchaniewski/snuggliest-scraper)"))

(def ^:dynamic *connection-timeout* 3000)

(defn params->query-string 
  "Converts map of parameters to encoded query string."
  [params]
  (str/join "&" (for [[k v] params
                      :let [qname (name k)
                            qvalue (java.net.URLEncoder/encode (str v))]] 
                  (str qname "=" qvalue))))

(defn- urlconnection
   "Returns URLConnection for the given URL. Connection will timeout after 2 seconds."
  [url]
  (-> (java.net.URL. url)
      .openConnection
      (doto (.setRequestProperty "User-Agent" *user-agent*)
            (.setConnectTimeout *connection-timeout*)
            (.setReadTimeout *connection-timeout*))))

(defn dom
  "Returns dom tree for the given URL."
  [url]
  (try 
    (with-open [inputstream (.getInputStream (urlconnection url))] 
      (enlive/html-resource inputstream))
    (catch java.net.SocketTimeoutException e 
      (log/info "Connection timeout while reading resource:" url)
      nil)))

(defn async-dom
  [{:keys [url] :as scrape-data} callback]
  (log/info "Processing URL:" url)
  (-> (http/get url {:headers {"User-Agent" *user-agent*}
                     :connection-timeout *connection-timeout*
                     :request-timeout *connection-timeout*})
      (d/chain
        :body
        enlive/html-resource
        #(assoc scrape-data :dom %)
        callback)
      (d/catch java.util.concurrent.TimeoutException #(log/info "Connection timeout while reading resource:" url #_%))))

(defn extract-first-element-text
  "Returns text of the first element for the given selector in the given dom."
  [dom selector]
  (enlive/text (first (enlive/select dom selector))))
