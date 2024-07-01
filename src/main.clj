#!/usr/bin/env bb

(ns main
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.tools.cli :refer [parse-opts]]))

(def transcription-provider "https://tactiq-apps-prod.tactiq.io/transcript")

(def cli-options
  [["-l" "--lang LANG" "Language code (default: en)"
    :default "en"]
   ["-h" "--help"]])

; TODO: add a usage info

(defn validate-url [url]
  (or (re-matches #"https?://(?:www\.)?youtube\.com/watch\?v=[\w-]+" url)
      (re-matches #"https?://youtu\.be/[\w-]+" url)))

(defn transcribe [url lang]
  (let [response (http/post transcription-provider
                            {:headers {"Content-Type" "application/json"}
                             :body (json/generate-string
                                     {:videoUrl url
                                      :langCode lang})})]
    (json/parse-string (:body response) true)))

(defn plain-text
  [{title    :title
    captions :captions}]
  (->> captions
       (sort-by #(Float/parseFloat (:start %)))
       (map (fn [{start-ts :start
                  text     :text}]
              (format "[%s] %s" start-ts text)))
       (str/join "\n")))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options)      (println summary)
      errors               (do (println errors) (System/exit 1))
      ; TODO: refactor out validate-args
      (empty? arguments)   (do (println "Please provide a YouTube URL as a positional argument") (System/exit 1))
      (not (validate-url (first arguments))) (do (println "Must be a valid YouTube video URL") (System/exit 1))

      :else (-> (first arguments)
                (transcribe (:lang options))
                (plain-text)
                (println)))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
