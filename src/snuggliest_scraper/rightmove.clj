(ns snuggliest-scraper.rightmove
  (:require
    [clj-time.core :as time]
    [clj-time.jdbc]
    [clojure.string :as str]
    [net.cgrand.enlive-html :as enlive]
    [snuggliest-scraper.scraper :as scraper :refer [params->query-string extract-first-element-text]]))

(def ^:dynamic *base-url* "http://www.rightmove.co.uk/")

(def ^:dynamic *default-search-params* {:locationIdentifier "REGION^87490"
                                        :radius 0.0
                                        :maxPrice 1850
                                        :minBedrooms 3
                                        :numberOfPropertiesPerPage 50})

(def ^:dynamic *property-path-format* "property-to-rent/property-%s.html")

(def ^:dynamic *property-path* "/property-to-rent/property-")

(def ^:dynamic *search-page-property-ids-selector* [:div.touchsearch-summary-list-item-price :> :a])

(def ^:dynamic *property-prices-selector* [:p#propertyHeaderPrice :strong])

(def ^:dynamic *property-coordinates-selector* [:a.js-ga-minimap :img])

(def ^:dynamic *property-title-selector* [:title])

(def ^:dynamic *property-address-selector* [:div.property-header :address])

(defn search-url
  ([]
    (search-url 0 *default-search-params*))
  ([page-number]
    (search-url page-number *default-search-params*))
  ([page-number params]
    (let [params (merge *default-search-params* params)
          index (* page-number (:numberOfPropertiesPerPage params))
          params (assoc params :index index)]
      (str *base-url* "property-to-rent/find.html?" (params->query-string params)))))

(defn property-url
  [property-id]
  (str *base-url* (format *property-path-format* property-id)))

(defn- property-path?
  [url]
  (.startsWith url *property-path*))

(defn- extract-property-id
  [property-url]
  (re-find #"\d+" property-url))

(defn extract-search-page-property-ids
  [dom]
  (->> (enlive/select dom *search-page-property-ids-selector*)
       (map :attrs)
       (map :href)
       (filter property-path?)
       (map extract-property-id)))

(defn- extract-property-basic-data
  [dom]
  {:title (extract-first-element-text dom *property-title-selector*)
   :address (extract-first-element-text dom *property-address-selector*)})

(defn- extract-property-prices
  [dom]
  (->> (str/replace (extract-first-element-text dom *property-prices-selector*) #"," "")
       (re-seq #"\d+")
       (map str/trim)
       (map #(Integer/parseInt %))
       (zipmap [:ppw :ppm])))

(defn- extract-property-coordinates
  [dom]
  (when-let [minimap-src (get-in (first (enlive/select dom *property-coordinates-selector*)) [:attrs :src])]
    {:latitude (last (re-find #"latitude=(-?\d+\.\d+)" minimap-src))
     :longitude (last (re-find #"longitude=(-?\d+\.\d+)" minimap-src))}))

(defn property-removed?
  [dom]
  (true? (some #(.contains % "has been removed") (enlive/texts (enlive/select dom [:div.alert])))))

(defn property-invalid?
  [dom]
  (true? (some #(.contains % "can't show your page") (enlive/texts (enlive/select dom [:div#message :h2])))))

; REFACTOR
(defn extract-property-data
  [dom]
  (apply merge ((juxt extract-property-basic-data
                      extract-property-prices
                      extract-property-coordinates) 
                 dom)))
  ; TODO: Which approach is better?
  ; (merge (extract-property-basic-data dom)
  ;        (extract-property-prices dom)
  ;        (extract-property-coordinates dom)))

(defn create-db-record
  [id url dom]
  (merge 
    {:code id
     :url url
     :source "RIGHTMOVE"
     :scraped_at (time/now)}
    (cond
      (property-removed? dom) {:status "REMOVED"}
      (property-invalid? dom) {:status "INVALID"}
      :else (assoc (extract-property-data dom) :status "AVAILABLE"))))
