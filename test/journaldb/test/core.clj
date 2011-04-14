(ns journaldb.test.core
  (:use journaldb.core clojure.test clojure.contrib.pprint)
  (:require [clojure.java.io :as io]))

(defn add-user [db userid name jira]
  (change db (because (format "Access approved for user '%s' under JIRA %s" name jira)
                      `(assoc-in [:users ~userid] ~name))))

(defn add-test-users [db]
  (add-user db "malc" "Malcolm Sparks" "RTGM-123")
  (add-user db "tim" "Tim Williams" "RTGM-124")
  )

(deftest add-users-with-backing-writer
  (let [underlying (java.io.StringWriter.)
        journal (io/writer underlying)
        state (ref {})
        db (create-database journal state)]
    (add-test-users db)
    (println "State is :-")
    (pprint (get-state db))
    (println "Journal is :-")
    (doall (map println (line-seq (io/reader (java.io.StringReader. (str underlying))))))
    (is (= 2 (count (line-seq (io/reader (java.io.StringReader. (str underlying)))))))
    (is (= 2 (count (:users (get-state db)))))))

(deftest add-users-with-backing-ref
  (let [journal (ref [])
        state (ref {})
        db (create-database journal state)]
    (add-test-users db)
    (doall (map pprint @journal))
    (is (= 2 (count @journal)))
    (is (= 2 (count (:users (get-state db)))))))

(defn recover [journal db]
  (dorun (for [e journal]
           (change db e))))

(deftest recover-users-with-backing-ref
  (let [journal (ref [])
        db (create-database journal (ref {}))
        new-db (create-database (ref []) (ref {}))]
    (add-test-users db)
    (binding [*user* "Mary Rescue"]
      (recover @journal new-db)
      (is (= 2 (count (:users (get-state new-db)))))
      (add-user new-db "hankster" "Steve Hankin" "RTGM-125")
      (is (= 3 (count (:users (get-state new-db))))))
    ))

