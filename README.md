# pdf-bookmark-maker

Command line tool to create bookmarks for PDFs.

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

### make a standalone executable with lein-binplus

Install lein-binplus

To install it in your ~/.lein/profiles.clj just add the dependency like:

``` clojure

{:user
 {:plugins
  [[lein-binplus "0.6.6"]]}}

```

`lein bin`

https://github.com/BrunoBonacci/lein-binplus

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
