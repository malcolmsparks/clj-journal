(ns journaldb.test.core
  (:use journaldb.core clojure.test clojure.contrib.pprint)
  (:require [clojure.java.io :as io]))

(defn add-user [db userid name]
  (change db (because (format "Access approved for user '%s'" name)
                      `(assoc-in [:users ~userid] ~name)
                      )))

;; TODO: Test with in-memory journal as (ref []) rather than StringWriter.

(deftest add-users
  (let [underlying (java.io.StringWriter.)
        journal (io/writer underlying)
        state (ref {})
        db (create-database journal state)]
    (add-user db "malc" "Malcolm Sparks")
    (add-user db "tim" "Tim Williams")
    (add-user db "hankster" "Steve Hankin")
    (println "State is :-")
    (pprint (get-state db))
    (println "Journal is :-")
    (doall (map println (line-seq (io/reader (java.io.StringReader. (str underlying))))))
    (is (= 9 (count (line-seq (io/reader (java.io.StringReader. (str underlying)))))))
    (is (= 3 (count (:users (get-state db)))))))
