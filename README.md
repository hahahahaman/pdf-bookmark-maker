# pdf-bookmark-maker

Clojure and the PDFBox Java library to do stuff with PDF files.

## Usage

### Print bookmarks

```
pbm print ${PDF}
```

### Add bookmarks

```
pbm add ${BOOKMARK_FILE} ${PDF_FILE} ${PAGE_OFFSET (optional)}
```

Bookmark files are formatted text files. A bookmark line appears in
this format:

```
${INDENT} ${TEXT} ${PAGE}
```

The `${PAGE_OFFSET}` variable will add a bookmark to pdf file at `${PAGE +
PAGE_OFFSET}`.

If `${PAGE}` is empty then the bookmark link will default to the first
page or the same page as the parent bookmark.

`${PAGE_OFFSET}` defaults to 0.

#### Example File

```
Contents 14

1. A 20
 1.1 A1 30
 1.2 A2 31
2. B 40
 2.1 B2 43
 
Index 50
```

## Apache PDFBox 

https://pdfbox.apache.org/

This tool uses the PDFBox java library. The code is based on the examples provided by the library.

### Examples of how to use the library

https://svn.apache.org/viewvc/pdfbox/trunk/examples/src/main/java/org/apache/pdfbox/examples/

#### Create Bookmarks

https://svn.apache.org/viewvc/pdfbox/trunk/examples/src/main/java/org/apache/pdfbox/examples/pdmodel/CreateBookmarks.java?view=markup

#### Print Bookmarks

https://svn.apache.org/viewvc/pdfbox/trunk/examples/src/main/java/org/apache/pdfbox/examples/pdmodel/PrintBookmarks.java?view=markup

## Alternatives

Other tools that may help:

https://www.pdflabs.com/blog/export-and-import-pdf-bookmarks/

https://commons.wikimedia.org/wiki/Help:Creating_an_outline_for_PDF_and_DjVu

## make a standalone executable with lein-binplus

https://github.com/BrunoBonacci/lein-binplus

To Install lein-binplus in your ~/.lein/profiles.clj just add the dependency like:

``` clojure

{:user
 {:plugins
  [[lein-binplus "0.6.6"]]}}

```

Then inside this project's directory:

`lein bin`


### alternatively create an uberjar 

create an uberjar in the target/ folder:

`lein uberjar`

I copy the uberjar into ~/bin/ and create a shell file pbm.sh:

``` sh

java -jar pbm-uber.jar "$1" "$2" "$3"

```

`chmod +x ~/bin/pbm.sh`


## License

Copyright © 2020 Edward Ye

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
