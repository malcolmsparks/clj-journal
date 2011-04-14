(ns journaldb.core
  (:require [clojure.java.io :as io]
            [clj-time.core :as time]
            [clj-time.format :as tf])
  (:use clojure.contrib.pprint))

(defprotocol Journal
  (log [this e]))

(extend-protocol Journal
  nil
  (log [_ _] nil)

  clojure.lang.PersistentVector
  (log [this e] (conj this e))

  java.io.BufferedWriter
  (log [this e]
       (.write this (str e))
       (.write this "\n")
       (.flush this)
       )

  clojure.lang.IRef
  (log [this e] (alter this conj e))
  )

(defprotocol State
  (update [this f])
  (get-map [this]))

(declare this-binding)

(extend-protocol State
  nil
  (update [this f] this)
  (get-map [this] nil)

  clojure.lang.IRef
  (update [this f] (binding [this-binding this]
                     (eval `(alter this-binding ~@f))))
  (get-map [this] @this))

(defprotocol Changeable
  (change [_ event])
  (get-journal [this])
  (get-state [this]))

(defrecord Database [journal state]
  Changeable
  (change [_ event] (dosync
                     (log journal (assoc event :when (tf/unparse (tf/formatters :basic-date-time) (time/now))))
                     (update state (:what event))))
  (get-journal [this] journal)
  (get-state [this] (get-map state)))

(defn create-database [journal state]
  (Database. journal state))

(defn because [why what]
  {:what what
   :why why})
