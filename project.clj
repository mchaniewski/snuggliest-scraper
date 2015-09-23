(defproject snuggliest-scraper "0.1.0-SNAPSHOT"
  :description "Snuggliest scraper."
  :url "https://github.com/mchaniewski/snuggliest-scraper"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[clj-time "0.10.0"]
                 [enlive "1.1.6"]
                 [environ "1.0.1"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/data.json "0.2.6"]
                 ; HTTP client.
                 [aleph "0.4.0"]
                 [manifold "0.1.0"]
                 ; Database.
                 [migratus "0.8.4"]
                 [org.clojure/java.jdbc "0.4.1"]
                 [org.postgresql/postgresql "9.4-1202-jdbc42"]
                 [yesql "0.4.2"]
                 ; Logging.
                 [ch.qos.logback/logback-classic "1.1.3"]
                 [org.clojure/tools.logging "0.3.1"]]
  :min-lein-version "2.0.0"
  :plugins [[lein-environ "1.0.1"]]
  :main ^:skip-aot snuggliest-scraper.core
  :target-path "target/%s"
  :jvm-opts ["-Xms64m" "-Xmx128m"]
  :repl-options {:timeout 200000}
  :profiles {:uberjar {:aot :all}
             :production {:env {:production true}}
             :dev {:dependencies [[org.clojure/tools.trace "0.7.8"]]
                   :env {:db-connection-string "//localhost:5432/snuggliest"
                         :db-user              "snuggliest"
                         :db-password          "snuggliest"}}})
