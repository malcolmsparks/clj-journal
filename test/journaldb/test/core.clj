(ns journaldb.test.core
  (:use journaldb.core clojure.test clojure.contrib.pprint)
  (:require [clojure.java.io :as io]))

;; Add something to the database, via the journal

(def nothing {:what (fn [db] db)
              :why "Dolce fa nienta"})

(defn because [why what]
  {:what what
   :why why})

(defn add-user [db userid name]
  (change db (because (format "Adding user '%s'" userid)
                      `(assoc-in [:users ~userid] ~name)
                      )))

(deftest add-users
  (let [underlying (java.io.StringWriter.)
        journal (io/writer underlying)
        state (ref {})
        db (create-database journal state)]
    (add-user db "malc" "Malcolm Sparks")
    (add-user db "tim" "Tim Williams")
    (add-user db "hankster" "Steve Hankin")
    (pprint (get-state db))
    (doall (map println (line-seq (io/reader (java.io.StringReader. (str underlying))))))
    (is (= 3 (count (line-seq (io/reader (java.io.StringReader. (str underlying)))))))
    (is (= 3 (count (:users (get-state db)))))))



