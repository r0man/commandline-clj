(defproject commandline-clj "0.3.0"
  :description "Clojure command line parsing library."
  :min-lein-version "2.0.0"
  :url "https://github.com/r0man/commandline-clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :deploy-repositories [["releases" :clojars]]
  :dependencies [[clj-time "0.12.0"]
                 [commons-cli/commons-cli "1.3.1"]
                 [org.clojure/clojure "1.8.0"]]
  :aliases {"lint" ["do" ["eastwood"]]
            "ci" ["do" ["difftest"] ["lint"]]}
  :eastwood {:exclude-linters [:suspicious-expression]}
  :profiles {:provided {:plugins [[jonase/eastwood "0.2.3"]
                                  [lein-difftest "2.0.0"]]}})
