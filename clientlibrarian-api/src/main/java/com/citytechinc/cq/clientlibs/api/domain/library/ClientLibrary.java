package com.citytechinc.cq.clientlibs.api.domain.library;

import com.citytechinc.cq.clientlibs.api.domain.sling.runmode.SlingRunModeGroup;

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
}
