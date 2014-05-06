# Client Librarian

[CITYTECH, Inc.](http://www.citytechinc.com)

## Overview

The Client-Librarian is a dependency management mechanism for client side libraries (css, js, less) which builds on the
concepts introduced by the AEM Client Library mechanisms.

## Features

* Declare Component and Template dependencies on Client Libraries
* Declare dependencies between Client Libraries
* Produce .css and .js files which are relevant to the components on a page
* Use global [LESS](http://lesscss.org/) variables and mixins in your LESS workflow without external compilation
* Produce multiple brands of .css and .js without unnecessary code duplication
* Provide programmatically generated or environment specific content for .css and .js

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

## Basic Usage

The Client-Librarian produces and delivers page specific CSS and JavaScript libraries via a servlet listening for
page resource requests with a selector of ```pagelib```.   Whether a CSS or JavaScript file is produced is based on the
extension of the requested URL.  For example, if your page resides at */content/tacodan/home/products/tacos*, the URL
for the CSS page library will be */content/tacodan/home/products/tacos.pagelib.css*.  Similarly the URL for the JavaScript
page library will be */content/tacodan/home/products/tacos.pagelib/js*.

### Page Library JSP Tag

A JSP tag by the name of ```pageLibrary``` is provided under the http://www.citytechinc.com/clientlibrarian/tags
namespace.  This tag is used to produce an appropriate page library URL for the page being rendered.  It is intended to
be included in the JSP of a page template.

The following options are available for the Tag

Attribute | Type                          | Required | Description
--------- | ----                          | -------- | -----------
type      | One of "css", "js", or "both" | Optional | Indicates whether you want to include the CSS or JavaScript page library, or both.  Omitting this attribute has the same effect as setting it to "both".
brand     | String                        | Optional | The Brand Identifier indicating a request for a Branded Page Library.  See [Branded Libraries](#branded-libraries) below.

### Declaring Component Dependencies on Client Libraries

At a high level, the Client Librarian builds up a Page Specific Library by inspecting the components represented on a
page, determining their Client Library dependencies, and building those dependencies into a Page Specific Library.
The Client Libraries which a given component is dependent upon are declared in the ```dependencies``` property of the
component definition node.  The value of this property is expected to be 1 to many Client Library Category Identifiers
indicating the categories which this component is dependent upon.

### Declaring Dependencies Between Libraries

Individual Client Libraries can declare dependencies on other Client Libraries in the same way that a component declares
its dependencies on Client Libraries.  The existing ```embed``` and ```dependencies``` vocabulary of the standard Client Library
functionality has been maintained by the Client Librarian to ease the transition.  Dependencies are declared by setting
the ```dependencies``` property to a value of 1 to many Client Library Categories.  If a Client Library will embed other
libraries in its rendering without depending on them, the ```embed``` property should be set to the categories of these
libraries.

### Refreshing the Client Librarian

The Client Librarian exposes some basic library statistics as well as a "Refresh" button in the **ClientLibraryRepositoryReportingAndMaintenanceMBean**
accessible via the Felix JMX Board.  The "Refresh" button is useful in situations where you are seeing unexpected results
in your rendered page specific libraries and want to make sure that said results are not caused by old data in the Librarian
itself.  Refreshing the Client Librarian clears any in memory cache of known libraries and components and forces the Librarian
to re-query the repository for Client Libraries and components.

## Intermediate Usage

### [LESS](http://lesscss.org/) Compilation

If your CSS Client Libraries include files with a ```.less``` extension, the Client Librarian will treat the library as a
LESS Library and will run a LESS compiler on the library after all of the files which make up the library have been
put together in the proper order.

The current version of the LESS compiler being used is 1.6.2.

### Strict JavaScript

Since JavaScript Libraries are put together dynamically based on the needs of the page, placing the ```"use strict"```
directive at the top of your JavaScript files is not necessarily guaranteed to cause your JavaScript to run in stict
mode (based on your dependency setup of course).  To force any Page Library created by the Librarian to be run in strict
mode, the ```strictJavascript``` OSGI configuration on the ```DefaultClientLibraryRepository``` may be set to ```true```.
Setting this configuration to ```true``` causes the ```"use strict"``` directive to be written to the top of any
JavaScript produced by the Librarian.  While the default is false so as to not break existing JavaScript it is highly
recommended that this be set to true as it helps catch potential errors early.

### Branded Libraries

Branding Client Libraries indicates to the Client Librarian the Libraries which should be considered for inclusion when
a branded library request is made.  A branded request is made by adding a single brand selector after the pagelib selector
in the URL of the Page Library.  This selector is added for you when the ```brand``` attribute of the ```pageLibrary```
JSP tag is set.  Declaring the brand of a Client Library is done by setting a ```brand``` property on the Client Library
node to 1 to many Brand Identifiers.  Doing so makes the library a "Branded Library" (as opposed to an "Unbranded Library").
During Library compilation, if a specific brand was requested, the Client Librarian will include Unbranded Libraries and
any Branded Libraries which contain the requested brand in their set of brands.  If no brand was requested, the Client Librarian
will include all Unbranded Libraries and any Branded Libraries which include the "default" brand in their set of brands.
Branded Libraries which do not include the "default" brand in their set of brands will not be included in an Unbranded
Page Library request.

### Run Mode Restrictions on Library Inclusion

A common concern is the inclusion of particular Libraries in particular environments.  This often comes up when one
needs to include some Libraries which augment the authoring experience in author mode only while excluding them from
publish.  Using the ```runModes``` property you can dictate which Sling Run Modes a given Client Library should be included
in.  This property is set on the Client Library node itself and may be set to 1 to many Sling Run Mode Identifiers.
If no runModes property is specified for a given Client Library then it is assumed that the Library should be included
in all run modes.  The run mode specified in this property may either be a single run mode such as ```author``` or a
composite run mode such as ```dev.node1.publish```.  If the property is set to a composite run mode then the Library will
only be included in environments running in all of the modes specified by the composite.
For example, a Client Library with a ```runModes``` property of ```dev.node1.publish``` will be included if the AEM
environment is running in all three run modes: dev, node1, and publish.

### Library Versioning

*Coming Soon*

## Advanced Usage


### Resource Dependency Providers

Above, the process which the Client Librarian undertakes to determine the Client Library dependencies for a given page
is described at a high level and involves inspecting the resources on a page and determining the Client Library dependencies
of the components represented by these resources.  This inspection mechanism is implemented within a ```ResourceDependencyProvider```
service.  The ```ResourceDependencyProvider``` interface exposes a single method:

```
public Set<ClientLibrary> getDependenciesForResource(Resource r)
```

During Page Library compilation, the Client Librarian will hand to the ```ResourceDependencyProvider``` service the Resource
representing the current page.  The ```ResourceDependencyProvider``` service is then responsible for returning the set
of Client Libraries which the page is dependent upon.  To extend the functionality of the Client Librarian and handle situations
where inspection of the Resources on a page is not enough to determine the Library dependencies, you can implement your own
```ResourceDependencyProvider``` services.  The Client Librarian will utilize all ```ResourceDependencyProvider``` services
existing in the OSGI environment, passing the same Resource to each and collecting a set of Client Libraries from each.
At the end of this process it will merge the sets collected from each of the services into a single set for processing
and inclusion.

### Variable Providers

Variable Providers give you a mechanism to include programmatically generated and / or environment specific information
in your JavaScript or CSS.  During Page Library compilation (and prior to LESS compilation in the case of a LESS Library)
the Client Librarian attempts to replace place holder variables found in the Page Libraries with variables provided by any of the
Variable Provider services.  A place holder variable is established in a library using the placeholder syntax ```<%variable-name%>```
surrounding your variable name with an opening and closing ```<%``` ```%>```.

Variable providing services are implementations of the ```VariableProvider``` interface.  This interface exposes a single method:

```
public Map<String, String> getVariables(Resource root)
```

During Library compilation, the Client Librarian will call this method on all Variable Provider services existing in
OSGI, passing the Resource representing the page for which the Library is being compiled and collecting bindings for
named variables.  Once bindings are collected from all known Variable Provider services, the Librarian will replace any
place holder variables whose variable name matches one of the bindings provided by the Variable Provider services.  Unmatched
place holder variables will be left un-touched.

**Note:** if multiple Variable Provider services provide bindings for the same variable name, the behavior, specifically concerning
which binding "wins," is undefined.
