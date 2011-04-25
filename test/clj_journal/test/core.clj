;; Copyright 2010 Malcolm Sparks.
;;
;; This file is part of clj-journal.
;;
;; clj-journal is free software: you can redistribute it and/or modify it under the
;; terms of the GNU Affero General Public License as published by the Free
;; Software Foundation, either version 3 of the License, or (at your option) any
;; later version.
;;
;; clj-journal is distributed in the hope that it will be useful but WITHOUT ANY
;; WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
;; A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more
;; details.
;;
;; Please see the LICENSE file for a copy of the GNU Affero General Public License.

(ns clj-journal.test.core
  (:use clj-journal.core clojure.test clojure.contrib.pprint)
  (:require [clojure.java.io :as io]))

(defn add-user [db userid name jira]
  (change db
          (-> {
               :how `(fn [~(symbol 'state) ~(symbol 'data)]
                       (assoc-in ~(symbol 'state) [:users (:userid ~(symbol 'data))] (:name ~(symbol 'data))))
               :with {:userid userid :name name}}
              (because ["Access approved for user '%s' under JIRA %s" :userid :name]))))

;; TODO: Order journal entry maps such that all the specific bits
;; (when, with) appear at the front, leaving the rest at the back for
;; maximum 'compressibility'.

;; TODO: Test gzip compression over a lot of entries.

(defn add-test-users [db]
  (add-user db "alice" "Alice Smith" "CLJN-123")
  (add-user db "betty" "Betty Brown" "CLJN-124"))

(deftest add-users-with-backing-writer
  (let [underlying (java.io.StringWriter.)
        journal (io/writer underlying)
        state (ref {})
        db (create-database journal state)]
    (add-test-users db)
    (println "State is :-")
    (pprint @(:state db))
    (println "Journal is :-")
    (doall (map println (line-seq (io/reader (java.io.StringReader. (str underlying))))))
    (is (= 2 (count (line-seq (io/reader (java.io.StringReader. (str underlying)))))))
    (is (= 2 (count (:users @(:state db)))))))

(deftest add-users-with-backing-ref
  (let [journal (ref [])
        state (ref {})
        db (create-database journal state)]
    (add-test-users db)
    (doall (map pprint @journal))
    (is (= 2 (count @journal)))
    (is (= 2 (count (:users @(:state db)))))))

(deftest recover-users-with-backing-ref
  (let [journal (ref [])
        db (create-database journal (ref {}))]
    ;; Bob adds some users
    (add-test-users db)
    ;; Oh dear, we just rebound our database and lost it!
    (let [db (create-database (ref []) (ref {}))]
      ;; Never mind, we still have the journal, let's recover.
      (binding [*user* "Mary Rescue"]
        (recover-db-from-journal @journal db)
        (is (= 2 (count (:users @(:state db)))))
        (add-user db "hankster" "Steve Hankin" "RTGM-125")
        (is (= 3 (count (:users @(:state db)))))
        (is (= "Bob Admin" (:by (first @(:journal db)))))
        (is (= "Mary Rescue" (:by (last @(:journal db)))))))))

(deftest recover-users-with-backing-file
  (let [state (ref {})
        db (create-database (io/writer "journal") state)]
    (add-test-users db)
    (let [db (create-database (ref []) (ref {}))]
      ;; Never mind, we still have the journal, let's recover.
      (binding [*user* "Mary Rescue"]
        (recover-db-from-journal (io/reader "journal") db)
        (is (= 2 (count (:users @(:state db)))))
        (add-user db "hankster" "Steve Hankin" "RTGM-125")
        (is (= 3 (count (:users @(:state db)))))
        (is (= "Bob Admin" (:by (first @(:journal db)))))
        (is (= "Mary Rescue" (:by (last @(:journal db)))))))))

;; TODO: Log rotation stuff
