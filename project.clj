(defproject pdf-bookmark-maker "0.1.0-SNAPSHOT"
  :description "Command line tool to add bookmarks to pdfs"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.apache.pdfbox/pdfbox "2.0.19"]]
  :repl-options {:init-ns pdf-bookmark-maker.core})
