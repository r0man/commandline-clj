(ns commandline.core
  (:import [org.apache.commons.cli BasicParser GnuParser HelpFormatter Option Options PosixParser]
           java.io.PrintWriter))

(def ^:dynamic *columns*
  (try (Integer/parseInt (System/getenv "COLUMNS"))
       (catch Exception _ 80)))

(def ^:dynamic *options* nil)

(defmulti parse-argument
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

(defmethod parse-argument :default [type argument]
  argument)

(defn- flatten-options [options]
  (->> (for [[opt long-opt & rest] options]
         [(concat [opt] rest) (concat [long-opt] rest)])
       (apply concat)
       (remove #(nil? (first %)))))

(defn- argument-bindings [command-line options]
  (->> (for [[opt description type arg-name required] (flatten-options options)]
         `[~opt ~(if (or type arg-name)
                   `(parse-argument ~type (.getOptionValue ~command-line ~(str opt)))
                   `(.hasOption ~command-line ~(str opt)))])
       (apply concat)))

(defn make-parser
  "Make a basic, gnu or posix parser."
  [type]
  (case type
    :basic (BasicParser.)
    :gnu (GnuParser.)
    :posix (PosixParser.)))

(defn make-option
  "Make an option."
  [opt long-opt description & [type arg-name required]]
  (doto (Option. (if opt (name opt)) (if long-opt (name long-opt)) (if (or type arg-name) true false) description)
    (.setArgName arg-name)
    (.setRequired (or required false))))

(defn print-help
  "Print the help for *options* with the specified command line syntax."
  [syntax & {:keys [header footer pad-left pad-desc width]}]
  (let [width (or width *columns*)]
    (.printHelp (HelpFormatter.) (PrintWriter. *out*) width syntax header *options* (or pad-left 2) (or pad-desc 0) footer)))

(defn print-usage
  "Prints the usage statement for *options* and the specified application."
  [program & {:keys [width]}]
  (let [width (or width *columns*)]
    (.printUsage (HelpFormatter.) (PrintWriter. *out*) width program *options*)))

(defmacro with-commandline-options
  "Evaluate body with command line options bound to name."
  [name options & body]
  `(let [~name
         (doto (Options.)
           ~@(for [[opt# long-opt# description# type# arg-name# required#] options]
               `(.addOption
                 (make-option
                  ~(if opt# (str opt#))
                  ~(if long-opt# (str long-opt#))
                  ~description#
                  ~type#
                  ~arg-name#
                  ~required#))))]
     ~@body))

(defmacro with-options
  "Evaluate body with *options* bound to options."
  [options & body] `(binding [*options* ~options] ~@body))

(defmacro with-commandline
  "Evaluate body with commandline arguments bound to their names."
  [parser args options & body]
  (let [command-line# (gensym "command-line")
        args# args
        name# (gensym "options")
        options# options]
    `(with-commandline-options ~name#
       [~@options#]
       (with-options ~name#
         (let [~command-line# (.parse (make-parser ~parser) ~name# (into-array ~(if (or (nil? args#) (empty? args#)) [""] args#)))
               ~@(argument-bindings command-line# options#)]
           ~@body)))))
