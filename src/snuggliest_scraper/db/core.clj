(ns snuggliest-scraper.db.core
  (:require 
    [clojure.java.jdbc :as jdbc]
    [environ.core :refer [env]]
    [yesql.core :as yesql]))

(defonce ^:dynamic *db-spec* {:classname   "org.postgresql.Driver"
                              :subprotocol "postgresql"
                              :subname     (env :db-connection-string)
                              :user        (env :db-user)
                              :password    (env :db-password)})

(yesql/defqueries "snuggliest_scraper/db/sql/queries.sql")

(defn property-exists?
  [source code]
  (not (empty? (find-property *db-spec* source code))))

(defn insert-property
  [property]
  (jdbc/insert! *db-spec* :property property))
