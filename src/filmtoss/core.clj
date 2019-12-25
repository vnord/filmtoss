(ns filmtoss.core
  (:require [net.cgrand.enlive-html :as html]))

(defn imdb-search-url [film-title]
  (clojure.string/replace (str "https://www.imdb.com/search/title/?title="
       film-title
       "&title_type=feature&lang=en_us") " " "+"))

(defn fetch-url [url]
  (html/html-resource (java.net.URL. url)))

(defn get-id-from-attr [x]
  (re-find #"\d+" (:href (:attrs x))))

(defn imdb-titles [title]
  (let
    [stuff (html/select 
             (fetch-url (imdb-search-url title)) [:h3.lister-item-header :a])]
    (map
      (fn [x] {:id (get-id-from-attr x)
               :title (html/text x)})
    stuff)))

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

(defn get-info [title]
  (let [hit  (first (imdb-titles title))
        id   (:id hit)
        page (fetch-url (str "https://www.imdb.com/title/tt" id))
        ppl  (get-people page)
        desc [:desc (clojure.string/trim (first (map html/text
               (html/select page [:div.summary_text]))))]]
    (into {} [hit ppl desc])))
