(ns utils.date)

(defn parse-ddmmyyyy
  [date]
  (java.time.LocalDate/parse
   date
   (java.time.format.DateTimeFormatter/ofPattern "dd/MM/yyyy")))

(defn parse-ddmmyy
  [date]
  (java.time.LocalDate/parse
   date
   (java.time.format.DateTimeFormatter/ofPattern "dd/MM/yy")))

(defn max-date
  [dates]
  (reduce (fn [a b] (if (.isAfter b a) b a)) dates))

(defn min-date
  [dates]
  (reduce (fn [a b] (if (.isBefore b a) b a)) dates))

(defn min-max-dates
  "For a list of maps containing a :date key,
  Return a tuple of [min_date, max_date] as strings."
  [row-maps]
  (->> row-maps
       (map :date)
       ((juxt min-date max-date))
       (map str)))
