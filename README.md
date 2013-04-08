# COMMANDLINE-CLJ [![Build Status](https://travis-ci.org/r0man/commandline-clj.png)](https://travis-ci.org/r0man/commandline-clj)

A Clojure wrapper around the Apache Commons CLI command line parsing
library. See: http://commons.apache.org/cli/index.html

## Installation

Via Clojars: http://clojars.org/commandline-clj

## Usage

    (use 'commandline.core)

    (with-commandline [arguments ["-a" "--all" "-A" "--almost-all" "-b" "--escape" "--block-size" "10" "-c" "FILE"]]
      [[a all "do not hide entries starting with ."]
       [A almost-all "do not list implied . and .."]
       [b escape "print octal escapes for nongraphic characters"]
       [nil block-size "use SIZE-byte blocks" :integer "SIZE"]
       [B ignore-backups "do not list implied entried ending with ~"]
       [c nil (str "with -lt: sort by, and show, ctime (time of last modification of file status information)\n"
                   "with -l:  show ctime and sort by name otherwise: sort by ctime")]
       [C nil "list entries by columns"]]
      (assert (= ["FILE"] arguments))
      (print-help "ls"))

    ;=> usage: ls
    ;=>   -A,--almost-all         do not list implied . and ..
    ;=>   -a,--all                do not hide entries starting with .
    ;=>   -b,--escape             print octal escapes for nongraphic characters
    ;=>   -B,--ignore-backups     do not list implied entried ending with ~
    ;=>      --block-size <SIZE>  use SIZE-byte blocks
    ;=>   -c                      with -lt: sort by, and show, ctime (time of last modification of file status information)
    ;=>                           with -l:  show ctime and sort by name otherwise: sort by ctime
    ;=>                           ctime and sort by name otherwise: sort by ctime
    ;=>   -C                      list entries by columns

## License

Copyright (C) 2011-2013 Roman Scherer

Distributed under the Eclipse Public License, the same as Clojure.
