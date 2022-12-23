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

(defn add-page-offset
  "adds OFFSET to the page numbers at the end of each line of a bookmark file.
  This is useful because often the page number in the contents of a book are
  different from the page number in the pdf file."
  [filepath offset]
  (with-open [rdr (io/reader filepath)]
    (doseq [line (line-seq rdr)]
      (let [arr (s/split line #"\s")
            num (parse-long (last arr))]
        (println (s/join " " (-> arr pop (conj (+ offset num)))))))))

(defn try-get-as-pdf [pdf-filepath]
  (let [^File pdf-file (io/as-file pdf-filepath)
        random-access-file (RandomAccessFile. pdf-file "r")
        parser (PDFParser. random-access-file)]
    (try
      (.parse parser)
      (.getPDDocument parser)
      (catch Exception _))))

(comment
  (defn is-pdf?
    "filepath leads to PDF?"
    [pdf-filepath]
    (if-let [pdf (try-get-as-pdf pdf-filepath)]
      (try
        (some? pdf)
        (finally
          (.close pdf)))
      false)))

(defn load-pdf
  "Load a given PDF only after checking if it really is a PDF"
  [pdf-filepath]
  (if-let [pdf (try-get-as-pdf pdf-filepath)]
    pdf
    (throw (IllegalArgumentException. (format "%s is not a PDF file" pdf-filepath)))))

(defn print-bookmark
  "Print bookmark tree structure. Depth-first search.

  Based on:
  https://svn.apache.org/viewvc/pdfbox/trunk/examples/src/main/java/org/apache/pdfbox/examples/pdmodel/PrintBookmarks.java?view=markup
  "

  [document bookmark current-indentation]
  (let [current (atom (.getFirstChild bookmark))]
    (while @current
      (let [title (.getTitle @current)
            pd (.getDestination @current) ;; page destination
            gta (.getAction @current) ;; go to action

            pd-page-num
            (cond (instance? PDPageDestination pd)
                  (.retrievePageNumber pd)

                  (instance? PDNamedDestination pd)
                  (let [pd (.. document
                               (getDocumentCatalog)
                               (findNamedDestinationPage pd))]
                    (when pd
                      (.retrievePageNumber pd)))

                  pd
                  (println
                   (str current-indentation
                        "Destination class: "
                        (.. @current
                            (getDestination)
                            (getClass)
                            (getSimpleName)))))

            gta-page-num
            (cond (instance? PDActionGoTo gta)
                  (cond (instance? PDPageDestination (.getDestination gta))
                        (let [pd (.getDestination gta)]
                          (.retrievePageNumber pd))

                        (instance? PDNamedDestination (.getDestination gta))
                        (when-let [pd (-> document
                                          (.getDocumentCatalog)
                                          (.findNamedDestinationPage
                                           (.getDestination gta)))]
                          (.retrievePageNumber pd))

                        :else
                        (println (str current-indentation
                                      "Destination class: "
                                      (-> gta
                                          (.getDestination)
                                          (.getClass)
                                          (.getSimpleName)))))

                  gta
                  (println current-indentation "Action class: "
                           (-> current
                               (.getAction)
                               (.getClass)
                               (.getSimpleName))))]

        (when (or pd-page-num gta-page-num)
          (println (str current-indentation title " "
                        (inc (if pd-page-num pd-page-num gta-page-num)))))

        ;; print all children
        (print-bookmark document @current (str current-indentation indent))
        (swap! current (fn [x] (.getNextSibling x)))))))

(defn print-bookmarks [pdf-filepath]
  (let [document (load-pdf pdf-filepath)
        outline (-> document (.getDocumentCatalog) (.getDocumentOutline))]
    (print-bookmark document outline "")
    ;; (println "# pages:" (.getNumberOfPages document))
    ;;
    (when document
      (.close document))))

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

(comment
  (def txt-path "resources/a.txt")
  (def pdf-path "resources/HeinzVonFoerster-UnderstandingUnderstanding.pdf")
  (def output-path "resources/test.pdf"))

(defn add-bookmarks
  "Add bookmarks to pdf.

  Based on:
  https://svn.apache.org/viewvc/pdfbox/trunk/examples/src/main/java/org/apache/pdfbox/examples/pdmodel/CreateBookmarks.java?view=markup
  "
  ([bookmark-filepath pdf-filepath]
   (add-bookmarks bookmark-filepath pdf-filepath pdf-filepath))
  ([bookmark-filepath pdf-filepath output-path]
   (add-bookmarks bookmark-filepath pdf-filepath output-path 0))
  ([bookmark-filepath pdf-filepath output-path offset]
   (println "Adding bookmarks from" bookmark-filepath "-->" pdf-filepath)
   ;; (println "Output at:" output-path)
   (let [document (load-pdf pdf-filepath)]
     (if (.isEncrypted document)
       (println "Error: Cannot add bookmarks to encrypted document.")

       (let [get-page (fn [page-num] (-> document (.getPages) (.get (dec page-num))))
             outline (PDDocumentOutline.)
             dest (PDPageFitWidthDestination.)
             error (atom false)
             set-error! (fn [msg]
                          (reset! error true)
                          (println msg))]
         (let [bookmark-stack (atom '())
               prev-num-indents (atom 0)
               line-num (atom 0)]
           (with-open [rdr (io/reader bookmark-filepath)]
             (doseq [line (line-seq rdr)
                     :while (not @error)]
               (swap! line-num inc)

               (when (not (= "" (s/trim line)))
                 ;; create a bookmark
                 (let [trimmed-line (s/trim line)
                       split-array (s/split trimmed-line #"\s")
                       num-indents (num-front-spaces line)
                       read-last-num (str->num (last split-array))
                       page-num (+ (if (number? read-last-num) read-last-num 1)
                                   offset)
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
                               (reset! bookmark-stack (list bookmark)))

                             (cond
                               (> num-indents @prev-num-indents)
                               (if (-> (- num-indents @prev-num-indents) (> 1))
                                 (set-error! (str "ERROR: Bookmark file too many indents. line: " @line-num))
                                 (do
                                   (.addLast (peek @bookmark-stack) bookmark)
                                   (swap! bookmark-stack conj bookmark)))

                               (= num-indents @prev-num-indents)
                               (do
                                 (swap! bookmark-stack pop)
                                 (.addLast (peek @bookmark-stack) bookmark)
                                 (swap! bookmark-stack conj bookmark))

                               :else
                               (if (-> (inc (- @prev-num-indents num-indents)) (> (count @bookmark-stack)))
                                 (set-error! (str "ERROR: Bookmark file too many indents. line:" @line-num))
                                 (do
                                   (dotimes [x (inc (- @prev-num-indents num-indents))]
                                     (swap! bookmark-stack pop))
                                   (.addLast (peek @bookmark-stack) bookmark)
                                   (swap! bookmark-stack conj bookmark)))))
                           (reset! prev-num-indents num-indents))))))))

         (when (not @error)
           (println "Saving PDF to" output-path)
           (-> document (.getDocumentCatalog) (.setDocumentOutline outline))
           (.save document output-path))))
     (when document
       (.close document)))))

(defn usage []
  (println (->>
            ["pbm - PDF Bookmark Maker"
             ""
             "Usage:"
             "  pbm add ${BOOKMARK_FILE} ${PDF_FILE} ${PAGE_OFFSET (optional)}"
             ;; "  pbm add ${BOOKMARK_FILE} ${PDF_FILE} ${OUTPUT_FILE}"
             "  pbm print ${PDF_FILE}"
             "  pbm help"

             ""

             "${BOOKMARK_FILE} format:"
             "Contents 3"
             "Chapter 1 - The Great Thing You Don't Know 5"
             " 1.1 - Why You've Never Heard of This 6"
             "  1.1.1 - Lies 7"
             "  1.1.2 - Conspiracies 10"
             " 1.2 - Why No One Else Has Heard of This 15"
             "Chapter 2 - Rethinking Everything 20"

             ""

             "${PAGE_OFFSET} is an optional parameter which allows you to"]
            (s/join \newline))))

(defn -main [& args]
  (let [[op arg1 arg2 arg3] args
        num-args (count args)]
    (cond (or (= num-args 0)
              (not (.contains ["help" "h" "add" "a" "print" "p"] op)))
          (usage)

          (.contains  ["help" "h"] op)
          (usage)

          (.contains ["add" "a"] op)
          (cond
            (= num-args 3)
            (add-bookmarks arg1 arg2 arg2)

            (= num-args 4)
            (add-bookmarks arg1 arg2 arg2 arg3)

            :else
            (usage))

          (.contains ["print" "p"] op)
          (if (< num-args 2)
            (usage)
            (print-bookmarks arg1))

          :else
          (usage))))
