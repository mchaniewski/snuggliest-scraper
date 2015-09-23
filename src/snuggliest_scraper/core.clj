(ns snuggliest-scraper.core
  (:require
    [aleph.http :as http]
    [clojure.core.async :as async]
    [clojure.java.jdbc :as jdbc]
    [clojure.string :as str]
    [clojure.tools.logging :as log]
    [environ.core :refer [env]]
    [manifold.deferred :as d]
    [snuggliest-scraper.db.core :as db]
    [snuggliest-scraper.rightmove :as rightmove]
    [snuggliest-scraper.scraper :refer [async-dom]])
  (:gen-class))

(defonce ^:private rightmove-search-dom-chan (async/chan 50))

(defonce ^:private rightmove-property-id-chan (async/chan 100))

(defonce ^:private rightmove-property-chan (async/chan 250))

(defn start-async-rightmove-search-producer
  []
  (dotimes [search-page-number 20]
    (let [search-url (rightmove/search-url search-page-number)]
      (async-dom 
        {:url search-url} 
        (partial async/put! rightmove-search-dom-chan)))))

(defn start-async-rightmove-search-consumer
  []
  (async/go-loop []
    (let [search-dom (:dom (async/<! rightmove-search-dom-chan))]
      (as-> search-dom $
            (rightmove/extract-search-page-property-ids $)
            (filter #(not (db/property-exists? "RIGHTMOVE" %)) $)
            (async/onto-chan rightmove-property-id-chan $ false)))
    (recur)))

(defn start-async-rightmove-property-producer
  []
  (async/go-loop []
    (let [property-id (async/<! rightmove-property-id-chan)
          property-url (rightmove/property-url property-id)]
        (async-dom 
          {:id property-id
           :url property-url}
          (partial async/put! rightmove-property-chan)))
    (recur)))

(defn start-async-rightmove-property-consumer
  []
  (async/go-loop []
    (let [property (async/<! rightmove-property-chan)
          {:keys [id url dom]} property]
      (if-not (db/property-exists? "RIGHTMOVE" id)
        (do 
          (log/debug "Saving property:" id url)
          (db/insert-property (rightmove/create-db-record id url dom)))
        (log/debug "Property already exist:" id url)))
    (recur)))

(defn start-all
  []
  (do
    (start-async-rightmove-property-consumer)
    (start-async-rightmove-property-producer)
    (start-async-rightmove-property-producer)
    (start-async-rightmove-search-consumer)
    (start-async-rightmove-search-producer)))

(defn -main
  [& args]
  nil)
