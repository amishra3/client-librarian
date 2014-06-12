# Client Librarian

[CITYTECH, Inc.](http://www.citytechinc.com)

## Overview

The Client-Librarian is a dependency management mechanism for client side libraries (css, js, less) which builds on the
concepts introduced by the AEM Client Library mechanisms.

## Features

* Declare Component and Template dependencies on Client Libraries
* Declare dependencies between Client Libraries
* Produce .css and .js files which are relevant to the components on a page
* Use global LESS variables and mixins in your LESS workflow without external compilation

## Requirements

* Maven 3.x for building the project

## Getting Started

The Client-Librarian is deployed as an AEM package created by the clientlibrarian-ui module.  To create and install
the package, the following command should be executed to build the Client Librarian:

```
mvn clean install
```

After this, the target directory of the clientlibrarian-ui module will contain the installable package .zip file.
This can be installed via the Package Manager.