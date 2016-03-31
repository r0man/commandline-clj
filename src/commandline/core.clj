(ns commandline.core
  (:require [clj-time.format :refer [parse]]
            [clojure.string :as str])
  (:import [java.io PrintWriter]
           [org.apache.commons.cli BasicParser GnuParser PosixParser]
           [org.apache.commons.cli HelpFormatter Option Options]))

(def ^:dynamic *columns*
  "The number of columns used by `print-usage` and `print-help`."
  (try (Integer/parseInt (System/getenv "COLUMNS"))
       (catch Exception _ 80)))

(def ^:dynamic *options*
  "The current command line option specs as a vector of maps. Only
  bound within the body of `with-commandline` and used by
  `print-usage` and `print-help`." nil)

(defn- parse-comma-separated
  "Parse a comma separated list with `f`."
  [s f]
  (->> (str/split (str s) #"\s*,\s*")
       (map f)
       (remove nil?)
       (vec)))

(defmulti parse-argument
  "Convert a command line `argument` to `type`."
  (fn [type argument] type))

(defmethod parse-argument :boolean [type argument]
  (if argument (Boolean/parseBoolean argument)))

(defmethod parse-argument :character [type argument]
  (if argument (first argument)))

(defmethod parse-argument :class [type argument]
  (if argument (Class/forName argument)))

(defmethod parse-argument :double [type argument]
  (if argument (Double/parseDouble argument)))

(defmethod parse-argument :float [type argument]
  (if argument (Float/parseFloat argument)))

(defmethod parse-argument :file [type argument]
  (if argument (java.io.File. argument)))

(defmethod parse-argument :integer [type argument]
  (if argument (Integer/parseInt argument)))

(defmethod parse-argument :integers [type argument]
  (if argument (parse-comma-separated argument #(Integer/parseInt %))))

(defmethod parse-argument :keyword [type argument]
  (if argument (keyword argument)))

(defmethod parse-argument :long [type argument]
  (if argument (Long/parseLong argument)))

(defmethod parse-argument :longs [type argument]
  (if argument (parse-comma-separated argument #(Long/parseLong %))))

(defmethod parse-argument :time [type argument]
  (if argument (parse argument)))

(defmethod parse-argument :default [type argument]
  argument)

(defn string-array
  "Convert `arguments` into an array of strings."
  [arguments]
  (into-array String (map str (or arguments []))))

(defn make-parser
  "Make a new parser `type`, either :basic, :gnu or :posix."
  [type]
  (case type
    :basic (BasicParser.)
    :posix (PosixParser.)
    (GnuParser.)))

(defn option
  "Convert an option map into an Option instance."
  [{:keys [short long description type arg-name required]}]
  (doto (Option.
         (if short (name short))
         (if long (name long))
         (if (or type arg-name) true false)
         description)
    (.setArgName arg-name)
    (.setRequired (or required false))))

(defn options
  "Convert a seq of option maps into an Options instance."
  [option-maps]
  (let [options (Options.)]
    (doseq [option-map option-maps]
      (.addOption options (option option-map)))
    options))

(defn option-map
  "Convert the command line option in vector form into a map."
  [[short long description & [type arg-name required]]]
  {:arg-name arg-name
   :description description
   :long (if long (name long))
   :required required
   :short (if short (name short))
   :type type})

(defn- extract-options
  "Extract the parsed options from `commandline` using `option-maps`."
  [commandline option-maps]
  (reduce
   (fn [options {:keys [arg-name short long type] :as option}]
     (let [value #(if (or arg-name type)
                    (parse-argument type (.getOptionValue commandline %))
                    (.hasOption commandline %))]
       (cond-> options
         long
         (assoc (keyword long) (value long))
         short
         (assoc (keyword short) (value short)))))
   {} option-maps))

(defn parse-commandline
  "Build a command line parser from `option-maps`, parse the
  `arguments` and return a vector of the parsed options and any
  pending arguments."
  [option-maps arguments & [opts]]
  (let [parser (make-parser (:parser opts))
        options (options option-maps)
        commandline (.parse parser options (string-array arguments))]
    [(extract-options commandline option-maps)
     (vec (seq (.getArgs commandline)))]))

(defn- assert-option-binding []
  (assert *options* "Please call `print-help` within the `with-commandline` macro!"))

(defn print-help
  "Print help about the program."
  [syntax & {:keys [header footer pad-left pad-desc width]}]
  (assert-option-binding)
  (let [width (or width *columns*)]
    (.printHelp
     (HelpFormatter.)
     (PrintWriter. *out*)
     width syntax header
     (options *options*)
     (or pad-left 2)
     (or pad-desc 2) footer)
    (.flush *out*)))

(defn print-usage
  "Print the usage information of the program."
  [program & {:keys [width]}]
  (assert-option-binding)
  (let [width (or width *columns*)]
    (.printUsage
     (HelpFormatter.)
     (PrintWriter. *out*)
     width program
     (options *options*))
    (.flush *out*)))

(defmacro with-columns
  "Bind `*columns*` to `size` and evaluate `body`."
  [size & body]
  `(binding [*columns* ~size] ~@body))

(defmacro with-commandline
  "Evaluate `body` with the parsed commandline `option-spec` bound to
  `options-sym` and any pending command line arguments to `arguments-sym`.

  Example:

  (with-commandline
    [[options arguments] [\"-a\" \"/tmp\"]]
    [[a all \"do not hide entries starting with .\"]
     [A almost-all \"do not list implied . and ..\"]
     [b escape \"print octal escapes for nongraphic characters\"]
     [t time \"the time\" :time]
     [nil block-size \"use SIZE-byte blocks\" :integer \"SIZE\"]
     [B ignore-backups \"do not list implied entried ending with ~\"]
     [c nil (str \"with -lt: sort by, and show, ctime (time of last modification of file status information)\n\"
                 \"with -l:  show ctime and sort by name otherwise: sort by ctime\")]
     [C nil \"list entries by columns\"]
     [I ids \"list of integers\" :integers \"IDS\"]]
    (prn \"Options: \" options)
    (prn \"Pending arguments: \" arguments))
  "
  [[[options-sym arguments-sym] arguments & [parser]] option-spec & body]
  `(binding [*options* ~(mapv option-map option-spec)]
     (let [[~(or options-sym (gensym))
            ~(or arguments-sym (gensym))]
           (parse-commandline *options* ~arguments)]
       ~@body)))
