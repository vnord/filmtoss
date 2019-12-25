(ns filmtoss.core
  (:require [net.cgrand.enlive-html :as html]))

(defn imdb-search-url [film-title]
  (clojure.string/replace (str "https://www.imdb.com/search/title/?title="
       film-title
       "&title_type=feature&release_date=," (today)) " " "+"))

(defn fetch-url [url]
  (html/html-resource (java.net.URL. url)))

(defn today []
  (.format (java.text.SimpleDateFormat. "yyyy-MM-dd") (new java.util.Date)))


(defn get-id-from-attr [x]
  (re-find #"\d+" (:href (:attrs x))))

(defn imdb-ids [title]
  (let
    [stuff (html/select 
             (fetch-url (imdb-search-url title)) [:h3.lister-item-header :a])]
    (map
      (fn [x] (get-id-from-attr x)) stuff)))

(defn select-people [page loc]
  (let
    [x (html/select page [[:div.credit_summary_item (html/nth-of-type loc)]])
     people (filter string? (flatten (map :content (:content (first x)))))]
    (not-empty (take-while #(not= "|" %) people))))

(defn get-roles [[s & people]]
  (list (keyword (clojure.string/lower-case
                   (clojure.string/replace s #"s?:" ""))) people))

(defn get-people [page]
  (apply hash-map (apply concat (map get-roles (remove nil?
    (map #(select-people page %) [2 3 4]))))))

(defn get-rating [page]
  (first (clojure.string/split (clojure.string/trim (first
    (map html/text (html/select page [:div.imdbRating])))) #"/")))

(defn get-year [page]
  (first (:content (first
    (html/select page [:div.title_wrapper :h1 :span :a])))))

(defn get-title [page]
  (clojure.string/replace (first (:content (first
    (html/select page [:div.titleBar :div.title_wrapper :h1])))) #" " ""))
    ; that last bit is not a regular space lol

(defn get-original-title [page]
  (first (:content (first (html/select page [:div.originalTitle])))))

(defn get-genres [page loc]
  (let [g (map clojure.string/trim (filter #(not= "|" %) (filter string?
    (flatten (map :content (:content (first (html/select page
      [[:div.see-more.inline.canwrap (html/nth-of-type loc)]]))))))))]
  (if (empty? g) nil
    (if (clojure.string/starts-with? (first g) "Genre") (rest g) nil))))

(defn get-runtime [page]
  (->> (html/select page [:time]) (map :content) rest first first
       (re-find #"\d+")))

(defn get-info [title]
  (let [id             (first (imdb-ids title))
        page           (fetch-url (str "https://www.imdb.com/title/tt" id))
        title          [:title (get-title page)]
        original-title [:original-title (get-original-title page)]
        ppl            (get-people page)
        desc           [:desc (clojure.string/trim (first (map html/text
                              (html/select page [:div.summary_text]))))]
        rating         [:rating (get-rating page)]
        year           [:year (get-year page)]
        genre          [:genre (filter string? (flatten
                                 (map #(get-genres page %) [3 4 5])))]
        runtime        [:runtime (get-runtime page)]]
    (into {}
      [[:id id] title original-title year rating ppl desc genre runtime])))
