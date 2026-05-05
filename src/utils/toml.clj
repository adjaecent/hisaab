(ns utils.toml
  "Bare-bones TOML parser in no-dependency clojure. Handles a smaller subset:

  - Whitespace
  - Newlines
  - Comments
  - Simple Keys
  - Top-level Key-Value pairs
  - Value: Basic Strings (non-multi-line)
  - Value: Integers
  - Value: Floats
  - Value: Booleans
  - Value: Arrays (nestable, but without k-v pairs)
  - Standard Tables

  Full grammar: https://github.com/toml-lang/toml/blob/v0.5.0/toml.abnf"
  (:require [utils.sundry :refer [conjkw]]
            [clojure.math :refer [pow]]))

(def ^:private pos (atom 0))

(defn- curr [s] (get s @pos))
(defn- adv! [] (swap! pos inc) nil)
(defn- end? [s] (>= @pos (count s)))

(defn- skip-whitespace! [s]
  (while (and (not (end? s))
              (Character/isWhitespace (curr s)))
    (adv!)))

(defn- skip-comment! [s]
  (while (and (not (end? s))
              (not (= (curr s) \newline)))
    (adv!)))

(defn- skip-meta! [s]
  (loop []
    (skip-whitespace! s)
    (when (= (curr s) \#)
      (skip-comment! s)
      (recur))))

(defn- parse-basic-string [s]
  (adv!)
  (loop [cur-str nil]
    (if (end? s)
      cur-str
      (let [last-pos (curr s)]
        (adv!)
        (if (not (= last-pos \"))
          (recur (str cur-str last-pos))
          cur-str)))))

(defn- parse-number [s]
  (letfn [(count-digits [n]
            (if (zero? n)
              1
              (->> (abs n)
                   (iterate #(quot % 10))
                   (take-while pos?)
                   count)))]
    (loop [num       0
           mantissa  0
           saw-float false]
      (let [last-pos (curr s)]
        (if (and (not (end? s))
                 (Character/isDigit last-pos))
          (let [digit (Character/digit last-pos 10)]
            (adv!)
            (if saw-float
              (recur num (+ (* 10.0 mantissa) digit) saw-float)
              (recur (+ (* 10.0 num) digit) mantissa saw-float)))
          (if (= \. last-pos)
            (do (adv!) (recur num mantissa true))
            (if saw-float
              (+ num (/ mantissa (pow 10 (count-digits mantissa))))
              num)))))))

(defn- parse-bool [s]
  (->>
   (loop [cur-str nil]
     (let [last-pos (curr s)
           cur-word (str cur-str last-pos)]
       (if (end? s)
         cur-word
         (do
           (adv!)
           (if (not (= last-pos \e))
             (recur cur-word)
             cur-word)))))
   (get {"true" true "false" false})))

(declare parse-array)

(defn- parse-value [s]
  (skip-meta! s)
  (case (curr s)
    (\t \f) (parse-bool s)
    \"      (parse-basic-string s)
    \[      (parse-array s)
    (parse-number s)))

(defn- parse-array [s]
  (adv!)
  (loop [arr []]
    (skip-meta! s)
    (cond
      (end? s)        arr
      (= (curr s) \]) (do (adv!) arr)
      :else
      (let [val (parse-value s)]
        (skip-meta! s)
        (when (= (curr s) \,) (adv!))
        (recur (conj arr val))))))

(defn- parse-keyval [s]
  (loop [key  nil
         path []]
    (let [cur-path (conjkw path (keyword key))]
      (if (end? s)
        cur-path
        (let [last-pos (curr s)]
          (adv!)
          (case last-pos
            (\. \space \tab) (recur nil cur-path)
            \=               (do (skip-whitespace! s)
                                 [cur-path (parse-value s)])
            (recur (str key last-pos) path)))))))

(defn- parse-standard-table [s]
  (adv!)
  (loop [key  nil
         path []]
    (let [cur-path (conjkw path (keyword key))]
      (if (end? s)
        cur-path
        (let [last-pos (curr s)]
          (adv!)
          (case last-pos
            \. (recur nil cur-path)
            \] cur-path
            (recur (str key last-pos) path)))))))

(defn parse
  "Parse a TOML string into a Clojure map."
  [s]
  (reset! pos 0)
  (loop [data {}
         path []]
    (skip-meta! s)
    (if (end? s)
      data
      (if (= \[ (curr s))
        (recur data (parse-standard-table s))
        (let [[kv-path value] (parse-keyval s)]
          (recur (assoc-in data (concat path kv-path) value) path))))))
