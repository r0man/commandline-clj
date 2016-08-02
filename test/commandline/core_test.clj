(ns commandline.core-test
  (:require [clj-time.format :refer [parse]]
            [clojure.test :refer :all]
            [commandline.core :refer :all]
            [clojure.java.io :as io]))

(def option-short-long
  "A short and long option."
  '[A almost-all "do not list implied . and .."])

(def option-long-only
  "A short only option."
  '[nil block-size "use SIZE-byte blocks" :integer "SIZE"])

(deftest test-option-map
  (are [option expected]
      (= (option-map option) expected)
    option-short-long
    {:arg-name nil
     :description "do not list implied . and .."
     :long "almost-all"
     :required nil
     :short "A"
     :type nil}
    option-long-only
    {:arg-name "SIZE"
     :description "use SIZE-byte blocks"
     :long "block-size"
     :required nil
     :short nil
     :type :integer}))

(deftest test-option
  (let [option (option (option-map option-short-long))]
    (is (= (.getOpt option) "A"))
    (is (= (.getLongOpt option) "almost-all"))
    (is (= (.getDescription option) "do not list implied . and .."))
    (is (nil? (.getArgName option)))
    (is (= (.getType option) java.lang.String))
    (is (= (.isRequired option) false))))

(deftest test-options
  (let [option-spec [option-short-long option-long-only]
        options (options (mapv option-map option-spec))]
    (is (= (seq (.getOptions options))
           (map (comp option option-map) option-spec)))))

(deftest test-parse-commandline
  (let [option-spec [option-short-long option-long-only]
        options (mapv option-map option-spec)
        arguments ["--almost-all" "--block-size" "100"]]
    (is (= (parse-commandline options ["-A" "--block-size" "100" "FILE"])
           (parse-commandline options ["--almost-all" "--block-size" "100" "FILE"])
           [{:almost-all true
             :A true
             :block-size 100}
            ["FILE"]]))))

(deftest test-no-bindings
  (with-commandline [[] []]
    [[help nil "print this message"]]
    (is *options*)))

(deftest test-no-bindings-no-options
  (with-commandline [[] []] []
    (is *options*)))

(deftest test-empty-options-empty-arguments
  (with-commandline [[options arguments] []]
    []
    (is (= options {}))
    (is (= arguments []))))

(deftest test-empty-options
  (with-commandline [[options arguments] ["1" "2"]]
    []
    (is (= options {}))
    (is (= arguments ["1" "2"]))))

(deftest test-and-options
  (with-columns 80
    (with-commandline
      [[options arguments]
       ["-help" "-projecthelp" "-version" "-verbose" "-debug"
        "-emacs" "-logfile" "logfile" "-logger" "java.util.logging.Logger"] :gnu]
      [[help nil "print this message"]
       [projecthelp nil "print project help information"]
       [version nil "print the version information and exit"]
       [quiet nil "be extra quiet"]
       [verbose nil "be extra verbose"]
       [debug nil "print debugging information"]
       [emacs nil "produce logging information without adornments"]
       [logfile nil "use given file for log" :file "file"]
       [logger nil "the class which is to perform logging" :class "classname"]
       [listener nil "add an instance of class as a project listener" :class "classname"]
       [buildfile nil "use given buildfile" :file "file"]]
      (is (= options
             {:debug true
              :emacs true
              :help true
              :logfile (io/file "logfile")
              :logger java.util.logging.Logger
              :projecthelp true
              :verbose true
              :version true}))
      (is (= (with-out-str (print-usage "ant"))
             (slurp "test-resources/ant/usage.txt" )))
      (is (= (with-out-str (print-help "ant"))
             (slurp "test-resources/ant/help.txt"))))))

(deftest test-ls-options
  (with-columns 80
    (with-commandline
      [[options [file]]
       ["-a" "--almost-all" "--block-size" "10" "-c" "-t" "2011-09-25T16:45:00.000Z"
        "-I" "1,2,3" "FILE"]]
      [[a all "do not hide entries starting with ."]
       [A almost-all "do not list implied . and .."]
       [b escape "print octal escapes for nongraphic characters"]
       [t time "the time" :time]
       [nil block-size "use SIZE-byte blocks" :integer "SIZE"]
       [B ignore-backups "do not list implied entried ending with ~"]
       [c nil (str "with -lt: sort by, and show, ctime (time of last modification of file status information)\n"
                   "with -l:  show ctime and sort by name otherwise: sort by ctime")]
       [C nil "list entries by columns"]
       [I ids "list of integers" :integers "IDS"]]
      (is (= options
             {:A true
              :I [1 2 3]
              :a true
              :all true
              :almost-all true
              :block-size 10
              :c true
              :ids [1 2 3]
              :t (parse "2011-09-25T16:45:00.000Z")
              :time (parse "2011-09-25T16:45:00.000Z")}))
      (is (= file "FILE"))
      (is (= (with-out-str (print-usage "ls"))
             (slurp "test-resources/ls/usage.txt" )))
      (is (= (with-out-str (print-help "ls"))
             (slurp "test-resources/ls/help.txt"))))))

(deftest test-parse-argument-time
  (is (nil? (parse-argument :time "x")))
  (is (= (parse-argument :time "20110925T164527.395Z")
         (parse "20110925T164527.395Z")))
  (is (= (parse-argument :time "2011-09-25")
         (parse "2011-09-25")))
  (is (= (parse-argument :keyword "kw") :kw))
  (let [result (parse-argument :integers "1,2,3")]
    (is (= result [1 2 3]))
    (is (every? #(= (class %) Integer) result)))
  (let [result (parse-argument :longs "1,2,3")]
    (is (= result [1 2 3]))
    (is (every? #(= (class %) Long ) result))))

(deftest test-parse-argument-doubles
  (is (= (parse-argument :doubles "1.1,2.2")
         [(double 1.1) (double 2.2)])))

(deftest test-parse-argument-floats
  (is (= (parse-argument :floats "1.1,2.2")
         [(float 1.1) (float 2.2)])))
