(ns journaldb.core
  (:require [clojure.java.io :as io]
            [clj-time.core :as time])
  (:use clojure.contrib.pprint))

;; Start with an empty journal

(defprotocol Journal
  (add-entry [this e]))

(extend-protocol Journal
  nil
  (add-entry [_ _] nil)

  clojure.lang.PersistentVector
  (add-entry [this e] (conj this e))

  java.io.BufferedWriter
  (add-entry [this e] (binding [*out* this]
                        (println e)))

  clojure.lang.IRef
  (add-entry [this e] (alter this conj e))
  )

(defprotocol State
  (update [this f])
  (get-map [this]))

(declare s)

(extend-protocol State
  nil
  (update [this f] this)
  (get-map [this] nil)

  clojure.lang.IPersistentMap
  (update [this f] (f this))
  (get-map [this] this)

  clojure.lang.IRef
  (update [this f] (binding [s this] (eval (cons 'alter (cons 's f)))))
  (get-map [this] @this))

(defprotocol Changeable
  (change [_ event])
  (get-journal [this])
  (get-state [this]))

(defrecord Database [journal state]
  Changeable
  (change [_ event] (dosync
                     (add-entry journal (assoc event :when (time/now)))
                     (update state (:what event))))
  (get-journal [this] journal)
  (get-state [this] (get-map state)))

(defn create-database [journal state]
  (Database. journal state))


