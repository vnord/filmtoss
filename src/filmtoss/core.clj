(ns filmtoss.core
  (:require [net.cgrand.enlive-html :as html])
  (:require [clojure.string :as string]))

(defn imdb-search-url [film-title]
  (string/replace (str "https://www.imdb.com/search/title/?title="
       film-title
       "&title_type=feature&release_date=," (today)) " " "+"))

(defn fetch-url [url]
  (html/html-resource (java.net.URL. url)))

(defn today []
  (.format (java.text.SimpleDateFormat. "yyyy-MM-dd") (new java.util.Date)))

(defn get-id-from-attr [x]
  (re-find #"\d+" (:href (:attrs x))))

(defn imdb-ids [title]
  (->> [:h3.lister-item-header :a]
       (html/select (fetch-url (imdb-search-url title)))
       (map #(get-id-from-attr %))))

(defn select-people [page loc]
  (->> (html/select page [[:div.credit_summary_item (html/nth-of-type loc)]])
       first
       :content
       (map :content)
       flatten
       (filter string?)
       (take-while #(not= "|" %))
       not-empty))

(defn get-roles [[s & people]]
  (list (keyword (string/lower-case (string/replace s #"s?:" ""))) people))

(defn get-people [page]
  (apply hash-map (apply concat (map get-roles (remove nil?
    (map #(select-people page %) [2 3 4]))))))

(defn get-rating [page]
  (first (string/split (string/trim (first
    (map html/text (html/select page [:div.imdbRating])))) #"/")))

(defn get-year [page]
  (first (:content (first
    (html/select page [:div.title_wrapper :h1 :span :a])))))

(defn get-title [page]
  (string/replace (first (:content (first
    (html/select page [:div.titleBar :div.title_wrapper :h1])))) #" " ""))
    ; that last bit is not a regular space lol

(defn get-original-title [page]
  (first (:content (first (html/select page [:div.originalTitle])))))

(defn get-genres [page loc]
  (let [g (map string/trim (filter #(not= "|" %) (filter string?
    (flatten (map :content (:content (first (html/select page
      [[:div.see-more.inline.canwrap (html/nth-of-type loc)]]))))))))]
  (if (empty? g) nil
    (if (string/starts-with? (first g) "Genre") (rest g) nil))))

(defn get-runtime [page]
  (->> (html/select page [:time]) (map :content) rest first first
       (re-find #"\d+")))

(defn get-info [title]
  (let [id             (first (imdb-ids title))
        page           (fetch-url (str "https://www.imdb.com/title/tt" id))
        title          [:title (get-title page)]
        original-title [:original-title (get-original-title page)]
        ppl            (get-people page)
        desc           [:desc (string/trim (first (map html/text
                              (html/select page [:div.summary_text]))))]
        rating         [:rating (get-rating page)]
        year           [:year (get-year page)]
        genre          [:genre (some identity
                                 (map #(get-genres page %) [3 4 5]))]
        runtime        [:runtime (get-runtime page)]]
    (into {}
      [[:id id] title original-title year rating ppl desc genre runtime])))
