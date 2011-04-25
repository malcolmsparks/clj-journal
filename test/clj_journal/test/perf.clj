;; Copyright 2010 Malcolm Sparks.
;;
;; This file is part of Plugboard.
;;
;; Plugboard is free software: you can redistribute it and/or modify it under the
;; terms of the GNU Affero General Public License as published by the Free
;; Software Foundation, either version 3 of the License, or (at your option) any
;; later version.
;;
;; Plugboard is distributed in the hope that it will be useful but WITHOUT ANY
;; WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
;; A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more
;; details.
;;
;; Please see the LICENSE file for a copy of the GNU Affero General Public License.

(ns clj-journal.test.perf
  (:use clj-journal.test.core clj-journal.core clojure.test clojure.contrib.pprint)
  (:require [clojure.java.io :as io]))


(deftest perf-test
  (with-open [w (io/writer "journal-perf")]
    (let [state (ref {})
          db (create-database w state)]
      (dorun (for [i (range 500)] (add-test-users db)))
      )))
