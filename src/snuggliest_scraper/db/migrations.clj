(ns snuggliest-scraper.db.migrations
  (:require
    [migratus.core :as migratus]
    [snuggliest-scraper.db.core :refer [*db-spec*]]))

(def ^:private config {:store                :database
                       :migration-dir        "snuggliest_scraper/db/migrations"
                       :migration-table-name "migrations"
                       :db *db-spec*})

(defn create 
  "Create new migration with a given name."
  [name]
  (migratus/create config name))

(defn migrate 
  "Apply pending migrations."
  []
  (migratus/migrate config))

(defn rollback 
  "Rollback last applied migration."
  []
  (migratus/rollback config))
