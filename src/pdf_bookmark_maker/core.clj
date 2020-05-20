(ns pdf-bookmark-maker.core
  (:import
   java.io.File
   java.io.IOException
   org.apache.pdfbox.io.RandomAccessFile
   org.apache.pdfbox.pdfparser.PDFParser
   org.apache.pdfbox.pdmodel.PDDocument
   org.apache.pdfbox.pdmodel.PDPage
   org.apache.pdfbox.pdmodel.PageMode
   org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo
   org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
   org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDNamedDestination
   org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitWidthDestination
   org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline
   org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem)
  (:require [clojure.java.io :as io]
            [clojure.string :as s])
  (:gen-class))

(def indent-num-spaces 1)
(def indent (s/join (repeat indent-num-spaces " ")))

(defn try-get-as-pdf [pdf-file-path]
  (let [^File pdf-file (io/as-file pdf-file-path)
        random-access-file (RandomAccessFile. pdf-file "r")
        parser (PDFParser. random-access-file)]
    (try
      (.parse parser)
      (.getPDDocument parser)
      (catch Exception _))))

(defn is-pdf?
  "Confirm that the PDF supplied is really a PDF"
  [pdf-file-path]
  (if-let [pdf (try-get-as-pdf pdf-file-path)]
    (try
      (not (nil? pdf))
      (finally
        (.close pdf)))
    false))

(defn load-pdf
  "Load a given PDF only after checking if it really is a PDF"
  [pdf-file-path]
  (if-let [pdf (try-get-as-pdf pdf-file-path)]
    pdf
    (throw (IllegalArgumentException. (format "%s is not a PDF file" pdf-file-path)))))

(defn print-bookmark [document bookmark indentation]
  "Print bookmark tree structure, by traversing recursively"
  (let [current (atom (.getFirstChild bookmark))]
    (while @current
      (print (s/join [indentation (.getTitle @current)]))
      (let [dest (.getDestination @current)
            action (.getAction @current)]
        (cond (instance? PDPageDestination dest)
              (println (s/join [" " (inc (.retrievePageNumber dest))]))

              (instance? PDNamedDestination dest)
              (let [pd (-> (.getDocumentCatalog document) (.findNamedDestinationPage dest))]
                (when pd
                  (println (s/join " " (inc (.retrievePageNumber pd))))))

              ;; dest
              ;; (println (s/join  " " (-> @current
              ;;                           (.getDestination)
              ;;                           (.getClass)
              ;;                           (.getSimpleName))))
              )

        (cond (instance? PDActionGoTo action)
              (cond (instance? PDPageDestination (.getDestination action))
                    (let [pd (.getDestination action)]
                      (println (s/join [" " (inc (.retrievePageNumber pd))])))

                    (instance? PDNamedDestination (.getDestination action))
                    (if-let [pd (-> document (.getDocumentCatalog)
                                    (.findNamedDestinationPage (.getDestination action)))]
                      (println (s/join [" " (inc (.retrievePageNumber pd))])))

                    ;; :else
                    ;; (println (s/join [" " (-> action (.getDestination) (.getClass) (.getSimpleName))]))
                    )

              ;; action
              ;; (println indentation "Action class: " (-> current
              ;;                                           (.getAction)
              ;;                                           (.getClass)
              ;;                                           (.getSimpleName)))
              )
        (print-bookmark document @current (s/join [indentation indent]))
        (swap! current (fn [x] (.getNextSibling x)))))))

(defn print-bookmarks [pdf-file-path]
  (let [document (load-pdf pdf-file-path)
        outline (-> document (.getDocumentCatalog) (.getDocumentOutline))]
    (print-bookmark document outline "")
    ;; (println "# pages:" (.getNumberOfPages document))
    ))

(defn num-front-spaces [str]
  (loop [i 0
         c (nth str i)
         cnt 0]
    (if (= c \space)
      (if (= (inc i) (count str))
        (inc cnt)
        (recur (inc i) (nth str (inc i)) (inc cnt)))
      cnt)))

(defn str->num [str]
  (try (read-string str)
       (catch Exception _)))

(def txt-path "resources/a.txt")
(def pdf-path "resources/HeinzVonFoerster-UnderstandingUnderstanding.pdf")
(def output-path "resources/test.pdf")

(defn pop-atom! [a]
  (swap! a pop))

(defn set-atom! [a val]
  (swap! a (fn [x] val)))

(defn conj-atom! [a val]
  (swap! a (fn [x] (conj x val))))

(defn add-bookmarks [text-file-path pdf-file-path output-path]
  (println "Start adding bookmarks from" text-file-path "to" pdf-file-path "Output at:" output-path)
  (let [document (load-pdf pdf-file-path)]
    (if (.isEncrypted document)
      (println "Error: Cannot add bookmarks to encrypted document.")

      (let [get-page (fn [page-num] (-> document (.getPages) (.get (dec page-num))))
            outline (PDDocumentOutline.)
            dest (PDPageFitWidthDestination.)
            error (atom false)
            set-error! (fn [msg]
                         (set-atom! error true)
                         (println msg))]
        (with-open [rdr (io/reader text-file-path)]
          (let [bookmark-stack (atom '())
                prev-num-indents (atom 0)
                line-num (atom 0)]
            (with-open [rdr (io/reader text-file-path)]
              (doseq [line (line-seq rdr)
                      :while (not @error)]
                (swap! line-num inc)

                (when (not (= "" (s/trim line)))
                  ;; create a bookmark
                  (let [trimmed-line (s/trim line)
                        split-array (s/split trimmed-line #"\s")
                        num-indents (num-front-spaces line)
                        read-last-num (str->num (last split-array))
                        page-num (if (number? read-last-num) read-last-num 1)
                        bookmark (PDOutlineItem.)
                        dest (PDPageFitWidthDestination.)
                        title (-> trimmed-line
                                  (subs 0 (max 0 (- (count trimmed-line)
                                                    (if (number? read-last-num)
                                                      (inc (count (last split-array)))
                                                      0)))))]
                    (cond (= title "")
                          (set-error! (str "ERROR: Bookmark file invalid title. line: " @line-num))

                          (or (-> page-num (< 1))
                              (-> page-num (> (.getNumberOfPages document))))
                          (set-error! (str "ERROR: Bookmark file page number out of range. line: " @line-num))

                          :else
                          (do
                            ;; (println title ", page num:" page-num ", num indents:" num-indents)
                            (.setPage dest (get-page page-num))
                            (.setDestination bookmark dest)
                            (.setTitle bookmark title)

                            (if (= num-indents 0)
                              (do
                                (.addLast outline bookmark)
                                (set-atom! bookmark-stack (list bookmark)))

                              (cond
                                (> num-indents @prev-num-indents)
                                (if (-> (- num-indents @prev-num-indents) (> 1))
                                  (set-error! (str "ERROR: Bookmark file too many indents. line: " @line-num))
                                  (do
                                    (.addLast (peek @bookmark-stack) bookmark)
                                    (conj-atom! bookmark-stack bookmark)))

                                (= num-indents @prev-num-indents)
                                (do
                                  (pop-atom! bookmark-stack)
                                  (.addLast (peek @bookmark-stack) bookmark)
                                  (conj-atom! bookmark-stack bookmark))

                                :else
                                (if (-> (inc (- @prev-num-indents num-indents)) (> (count @bookmark-stack)))
                                  (set-error! (str "ERROR: Bookmark file too many indents. line:" @line-num))
                                  (do
                                    (dotimes [x (inc (- @prev-num-indents num-indents))]
                                      (pop-atom! bookmark-stack))
                                    (.addLast (peek @bookmark-stack) bookmark)
                                    (conj-atom! bookmark-stack bookmark)))))
                            (set-atom! prev-num-indents num-indents))))))))
          (when (not @error)
            (println "Saving PDF to" output-path)
            (-> document (.getDocumentCatalog) (.setDocumentOutline outline))
            (.save document output-path)))))
    (when document
      (.close document))))

(defn usage []
  (println (->>
            ["pbm - PDF Bookmark Maker"
             ""
             "Usage:"
             "  pbm add ${BOOKMARK_FILE} ${PDF_FILE} ${OUTPUT_FILE}"
             "  pbm print ${PDF_FILE}"
             "  pbm help"]
            (s/join \newline))))

(defn -main [& args]
  (cond (or (= (count args) 0)
            (not (.contains ["help" "h" "add" "a" "print" "p"] (nth args 0))))
        (usage)

        (.contains  ["help" "h"] (nth args 0))
        (usage)

        (.contains ["add" "a"] (nth args 0))
        (if (< (count args) 4)
          (usage)
          (add-bookmarks (nth args 1) (nth args 2) (nth args 3)))

        (.contains ["print" "p"] (nth args 0))
        (if (< (count args) 2)
          (usage)
          (print-bookmarks (nth args 1)))

        :else
        (usage)))
