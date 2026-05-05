(ns utils.money
  (:require [clojure.string :as s]))

(defn format-amount [n]
  (format "%.2f" (double n)))

(defn parse-amount
  "Parse a comma-formatted number string (e.g. \"1,02,283.68\") to a float."
  [s]
  (-> s
      s/trim
      (s/replace "," "")
      (Float/parseFloat)))
