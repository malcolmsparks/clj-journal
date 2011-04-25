(ns journaldb.core
  (:require [clojure.java.io :as io]
            [clj-time.core :as time]
            [clj-time.format :as tf])
  (:use clojure.contrib.pprint))

(defprotocol Journal
  (log [this e])
  (log-entries [this]))

(extend-protocol Journal
  nil
  (log [_ _] nil)
  (log-entries [_] nil)

  clojure.lang.PersistentVector
  (log-entries [this] this)

  java.io.BufferedWriter
  (log [this e]
       (.write this (str e))
       (.write this "\n")
       (.flush this))

  java.io.BufferedReader
  (log-entries [this] (map read-string (line-seq this)))

  clojure.lang.IRef
  (log [this e] (alter this conj e)))

(defprotocol State
  (update [this f data]))

(declare this-binding)

(def *user* "Bob Admin")

(extend-protocol State
  nil
  (update [this f data] this)

  clojure.lang.IRef
  (update [this f data] (binding [this-binding this]
                          (alter this-binding (eval f) data))))

(defprotocol Changeable
  (change [_ event]))

(defrecord Database [journal state]
  Changeable
  (change [_ event] (dosync
                     (log journal
                          (merge {:by *user*
                                  :when (tf/unparse (tf/formatters :basic-date-time) (time/now))}
                                 event))
                     (update state (get event :how) (get event :with)))))

(defn create-database [journal state]
  (Database. journal state))

(defn because [m why]
  (merge m {:why why}))

(defn recover-db-from-journal [journal db]
  (dorun (for [e (log-entries journal)]
           (change db e))))

