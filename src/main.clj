#!/usr/bin/env bb

(ns main
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.tools.cli :refer [parse-opts]]))

(def cli-options
  [["-u" "--url URL" "YouTube video URL"
    :validate [(fn [url]
                 (or (re-matches #"https?://(?:www\.)?youtube\.com/watch\?v=[\w-]+" url)
                     (re-matches #"https?://youtu\.be/[\w-]+" url)))
               "Must be a valid YouTube video URL"]]
   ["-l" "--lang LANG" "Language code (default: en)"
    :default "en"]
   ["-h" "--help"]])

(defn transcribe [url lang]
  (let [response (http/post "https://tactiq-apps-prod.tactiq.io/transcript"
                            {:headers {"Content-Type" "application/json"}
                             :body (json/generate-string
                                     {:videoUrl url
                                      :langCode lang})})]
    (println (json/parse-string (:body response) true))))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options)      (println summary)
      errors               (do (println errors) (System/exit 1))
      (not (:url options)) (do (println "Please provide a YouTube URL with -u or --url") (System/exit 1))

      :else (transcribe (:url options) (:lang options)))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
