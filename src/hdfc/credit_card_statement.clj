(ns hdfc.credit-card-statement
  (:require [clojure.string :as s]
            [utils.money :as money]
            [config :refer [conf]]
            [utils.sundry :refer :all]
            [utils.date :refer :all]
            [clojure.pprint :refer [print-table]]))

(defn description->tag [description]
  (let [selected-tag (->> (get-in @conf [:bank-statement :tags])
                          (filter (fn [[tag descs]]
                                    (some #(s/includes? (s/upper-case description) %) descs)))
                          ffirst)]
    (or selected-tag :untagged)))

(defn parse-row [[_tx-type _name date description amount debit-credit _rewards]]
  (let [credit? (= (s/trim (or debit-credit "")) "Cr")
        desc    (s/trim description)]
    {:date        (-> date s/trim (subs 0 10) parse-ddmmyyyy)
     :amount      (money/parse-amount amount)
     :description desc
     :credit?     credit?
     :tag         (description->tag desc)}))

(defn parse-csv [lines new-fmt?]
  (let [header-idx (first (keep-indexed #(when (s/starts-with? %2 "Transaction type~") %1) lines))
        delimiter  (if new-fmt? #"~\|~" #"~")]
    (->> (drop (inc header-idx) lines)
         (take-while #(or (s/starts-with? % "Domestic")
                          (s/starts-with? % "International")))
         (map #(s/split % delimiter))
         (map (fn [[tx-type name date desc a b c]]
                (if new-fmt?
                  [tx-type name date desc a b c]
                  [tx-type name date desc b c a]))))))

(defn parse-total-due [lines new-fmt?]
  (let [delimiter (if new-fmt? #"~\|~" #"~")
        line      (->> lines (filter #(s/starts-with? % "Total Amount Due~")) first)]
    (-> line (s/split delimiter) second money/parse-amount)))

(defn gen-statement [credits debits total-due]
  (let [[from to] (min-max-dates (concat credits debits))]
    {:from            from
     :to              to
     :total-credits   (reduce + 0.0 (map :amount credits))
     :total-debits    (reduce + 0.0 (map :amount debits))
     :total-due       total-due
     :debit-breakdown (->> (group-by :tag debits)
                           (map (fn [[tag expenses]]
                                  [tag (reduce + 0.0 (map :amount expenses))]))
                           (into {}))}))

(defn process [filename]
  (let [lines                       (-> filename slurp s/split-lines)
        header-idx                  (first (keep-indexed #(when (s/starts-with? %2 "Transaction type~") %1) lines))
        new-fmt?                    (s/starts-with? (nth lines header-idx) "Transaction type~|~")
        rows                        (parse-csv lines new-fmt?)
        tx-maps                     (map parse-row rows)
        {credits true debits false} (group-by :credit? tx-maps)]
    (gen-statement (or credits []) (or debits []) (parse-total-due lines new-fmt?))))

(defn format-statement [data]
  (let [summary   {:period        (str (:from data) " to " (:to data))
                   :total-credits (money/format-amount (:total-credits data))
                   :total-debits  (money/format-amount (:total-debits data))
                   :total-due     (money/format-amount (:total-due data))}
        breakdown (for [[category amount] (:debit-breakdown data)]
                    {:category (name category) :amount (money/format-amount amount)})]
    (println)
    (print "==> Summary")
    (print-table (keys summary) [summary])
    (println)
    (print "==> Expenditure Breakdown")
    (print-table (keys (first breakdown)) breakdown)))
