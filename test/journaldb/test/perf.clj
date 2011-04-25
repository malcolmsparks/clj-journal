(ns journaldb.test.perf
  (:use journaldb.test.core journaldb.core clojure.test clojure.contrib.pprint)
  (:require [clojure.java.io :as io]))


(deftest perf-test
  (with-open [w (io/writer "/home/malcolm/tmp/journal3")]
    (let [state (ref {})
          db (create-database w state)]
      (dorun (for [i (range 500)] (add-test-users db)))
      )))
