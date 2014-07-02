/**
 * Copyright 2014 CITYTECH, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.citytechinc.cq.clientlibs.api.domain.library;

import com.citytechinc.cq.clientlibs.api.domain.sling.runmode.SlingRunModeGroup;
import com.google.common.base.Optional;

import java.util.List;
import java.util.Set;

public interface ClientLibrary {

    public static final String CSS_FILE = "css.txt";
    public static final String JS_FILE = "js.txt";
    public static final String UTF_8_ENCODING = "utf-8";

    /**
     *
     * @return A String representation of the merged set of CSS files contained in this client library.
     *         This String includes the self-contained files and as such does not contain embeds or dependencies.
     */
    public String getCss();

    /**
     *
     * @return A String representation of the merged set of JS files contained in this client library.
     *         This String includes the self-contained files and as such does not contain embeds or dependencies.
     */
    public String getJs();

    public boolean hasCss();

    public boolean hasJs();

    public boolean hasLess();

    public boolean hasSass();

    /**
     *
     * @return The path to the cq:ClientLibraryFolder resource represented by the Client Library object
     */
    public String getClientLibraryPath();


    public Set<String> getJsResourcePaths();

    public Set<String> getCssResourcePaths();

    /**
     *
     * @return A set containing the paths to all of the library resources (css, js, less files) which
     *         make up the library
     */
    public Set<String> getClientLibraryResourcePaths();

    /**
     *
     * @return The paths to the library's include files, css.txt and js.txt
     */
    public Set<String> getLibraryIncludeFilePaths();

    /**
     *
     * @return The set of category names which this Client Library responds to
     */
    public Set<String> getCategories();

    /**
     *
     * @return The set of library categories upon which this library is dependent.
     */
    public List<String> getDependencies();

    /**
     * Provides the list of Conditional Dependencies declared for this Library.
     *
     * <p>
     *     A Conditional Dependency indicates that, while the library does not necessarily depend on the
     *     conditional dependency, when both the library and its Conditional Dependency are included in the same
     *     compiled library, any conditional dependencies need to be ordered prior to the conditionally dependent library.
     * </p>
     * <p>
     *     Consider the following practical example.
     *     <ul>
     *         <li>Library A contains file a.css which defines a color for the `p.confidential` selector</li>
     *         <li>Library B contains file b.css which also defines a color for the `p.confidential` selector</li>
     *         <li>Component A depends on Library A</li>
     *         <li>Component B depends on Library B</li>
     *         <li>Page A contains an instance of Component A and an instance of Component B</li>
     *     </ul>
     *     Since Page A contains an instance of a component which depends on Library A and an instance of a component
     *     which depends on Library B, both libraries will be included in the Page's page library. Given that Library A
     *     and Library B have no dependencies on each other, it should not matter in which order they are included.
     *     However, since Library A and Library B both modify the same attribute of the same selector, this indeterminate
     *     ordering may lead to differing results based on the pseudo-random ordering which would be produced.
     *     Ideally the libraries are organized in such a way that such situations do not arise, however this is not
     *     always possible, especially when working with front end frameworks or old libraries which one does not have
     *     the time to refactor.
     * </p>
     * <p>
     *     In such a case, Conditional Dependencies may be used to declare an appropriate ordering between libraries
     *     which is to be enforced in cases where both libraries are to be included based on other criteria without
     *     producing a direct dependency between the libraries.  In the example above, let us say that we want Library B's
     *     color declaration to take precedence in cases where both Library A and Library B will be on a page.  To do this
     *     we would declare that Library B has a Conditional Dependency on Library A.  This would cause Library A to be
     *     rendered prior to Library B in cases where they are both in the page library.  In cases where only one of the two
     *     is in the page library, this Conditional Dependency has no effect on ordering.  The Conditional Dependency never
     *     has any effect on library inclusion.
     * </p>
     * @return
     */
    public List<String> getConditionalDependencies();

    /**
     *
     * @return The set of library categories which this client library embeds.
     */
    public List<String> getEmbeddedCategories();

    /**
     *
     * Establishes the run modes in which this library should be included.  An empty list indicates that
     * it should be included in all run modes.  This method represents the runModes property of the client
     * library folder.
     *
     * @return The list of run modes in which this library should be included
     */
    public Set<SlingRunModeGroup> getRunModeGroups();

    /**
     *
     * @param runModes
     * @return True if the library should be included based on the run modes provided, false otherwise
     */
    public Boolean isIncludedForRunModes(Set<String> runModes);

    /**
     * Determines whether the library should be included based on the provided brand identifier.  A library
     * is included if it either does not belong to any particular brand or if it belongs to the brand requested.
     * In the case that no brand is requested, the behavior is the same as requesting the special "default" brand.
     *
     * @param brand The requested brand
     * @return True if the library should be included based on the brand requested, false otherwise
     */
    public Boolean isIncludedForBrand(Optional<String> brand);
}
