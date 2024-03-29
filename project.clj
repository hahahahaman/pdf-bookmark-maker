(defproject pdf-bookmark-maker "1.0"
  :description "Command line tool to add bookmarks to pdfs"
  :url ""
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.apache.pdfbox/pdfbox "2.0.19"]]
  :repl-options {:init-ns pdf-bookmark-maker.core}
  :main pdf-bookmark-maker.core
  :profiles {:uberjar {:aot [pdf-bookmark-maker.core]}}
  :jar-name "pbm.jar"
  :uberjar-name "pbm-uber.jar")
