/**
 * Copyright 2014, CITYTECH, Inc.
 * All rights reserved - Do Not Redistribute
 * Confidential and Proprietary
 */
package com.citytechinc.cq.clientlibs.core.domain.library.impl

import com.citytechinc.cq.clientlibs.api.domain.library.ClientLibrary
import com.citytechinc.cq.clientlibs.api.domain.sling.runmode.SlingRunModeGroup
import com.google.common.base.Optional
import com.google.common.collect.Sets
import org.apache.sling.api.resource.Resource
import org.apache.sling.api.resource.ResourceResolver
import org.apache.sling.api.resource.ValueMap

import org.apache.sling.api.resource.ResourceUtil

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.jcr.Node
import javax.jcr.Property

import java.util.regex.Matcher


/**
 *
 * Represents a Client Library which may contain JS and/or CSS.  Client Libraries are defined in
 * the JCR Repository as nodes of type cq:ClientLibraryFolder. These nodes are expected to contain
 * a css.txt and/or js.txt file indicating what CSS and JS files to include in the library
 * respectively.  Further the library may define a set of embedded libraries and a set of libraries
 * upon which the library is dependent.
 *
 * A Client Library belongs to one or more Library Categories.  Dependencies and Embedded Libraries
 * are referenced by category.
 *
 */
class DefaultClientLibrary implements ClientLibrary {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultClientLibrary.class)

    private Set<String> categories
    private Resource clientLibraryResource
    private List<String> embeddedCategories
    private List<String> dependencies

    private boolean isCssLibrary
    private boolean isJsLibrary
    private boolean isLessLibrary
    private boolean isSassLibrary

    private long cssLastModified
    private long jsLastModified

    private ResourceResolver resourceResolver

    private Set<String> jsResourcePaths
    private Set<String> cssResourcePaths
    private Optional<String> jsIncludeFilePathOptional
    private Optional<String> cssIncludeFilePathOptional

    private Set<SlingRunModeGroup> runModeGroups

    def DefaultClientLibrary( Set<String> categories, Resource clientLibraryResource, List<String> embeddedCategories, List<String> dependencies, Set<SlingRunModeGroup> runModeGroups ) {
        this.categories = categories
        this.clientLibraryResource = clientLibraryResource
        this.embeddedCategories = embeddedCategories
        this.dependencies = dependencies

        this.runModeGroups = runModeGroups

        cssResourcePaths = Sets.newLinkedHashSet()
        jsResourcePaths = Sets.newLinkedHashSet()

        this.resourceResolver = clientLibraryResource.getResourceResolver()

        jsIncludeFilePathOptional = Optional.absent()
        cssIncludeFilePathOptional = Optional.absent()

        isJsLibrary = false
        isCssLibrary = false

        determineCssInclusions()
        determineJsInclusions()
    }

    private void determineCssInclusions() {

        LOG.debug( "Determining CSS Inclusions" )

        Optional<Resource> inclusionFileResourceOptional = getInclusionFileOptional( CSS_FILE )

        if ( !inclusionFileResourceOptional.isPresent() ) {
            isCssLibrary = false
            return
        }

        LOG.debug( "Library contains CSS" )

        isCssLibrary = true
        cssIncludeFilePathOptional = Optional.of( inclusionFileResourceOptional.get().getPath() )

        Set<String> includedResourcePaths = getInclusionFileListForResource( inclusionFileResourceOptional.get() )

        includedResourcePaths.each( {

            if ( it.endsWith( ".less" ) ) {
                isLessLibrary = true
            }
            else if ( it.endsWith( ".sass" ) ) {
                isSassLibrary = true
            }

            cssResourcePaths.add( it )

        } )

    }

    private void determineJsInclusions() {

        LOG.debug( "Determining JS Inclusions" )

        Optional<Resource> inclusionFileResourceOptional = getInclusionFileOptional( JS_FILE )

        if ( !inclusionFileResourceOptional.isPresent() ) {
            isJsLibrary = false
            return
        }

        isJsLibrary = true
        jsIncludeFilePathOptional = Optional.of( inclusionFileResourceOptional.get().getPath() )

        Set<String> includedResourcePaths = getInclusionFileListForResource( inclusionFileResourceOptional.get() )

        jsResourcePaths.addAll( includedResourcePaths )

    }

    private Optional<Resource> getInclusionFileOptional( String filePath ) {

        LOG.debug( "Looking up inclusion file " + filePath )

        Resource fileResource = clientLibraryResource.getChild( filePath )

        if ( !fileResource || ResourceUtil.isNonExistingResource( fileResource ) ) {
            LOG.debug( "No inclusion file of type " + filePath + " found for library" )
            return Optional.absent()
        }

        return Optional.of( fileResource )

    }

    private Set<String> getInclusionFileListForResource( Resource resource ) {

        LOG.debug( "Determining the inclusion file set for " + resource.path )

        Set<String> retSet = Sets.newLinkedHashSet()

        def base = ""

        getReaderForBinaryResource( resource ).eachLine { line ->

            LOG.debug( "Processing line " + line )

            switch( line ) {
                /*
                 * Declaration of a base
                 */
                case ~/^#base=(.*)/:
                    base = Matcher.lastMatcher[ 0 ][ 1 ].trim()
                    if ( !base.endsWith( "/" ) ) {
                        base = base + "/"
                    }
                    break

            /*
             * Absolute or relative path to a file
             *
             * This regular expression looks for a string with the following properties
             *
             *   -The string may or may not start with a "/" character
             *   -The second character in cases where the string starts with a "/" or the first
             *    character in cases where it does not should exist and should be any character
             *    other than a white space or a "/" character
             *   -Following this may be any number of non space characters
             *   -Following this may be any number of space characters
             */
                case ~/^(\/?)([^\/\s][^\s]*)\s*$/:
                    def isAbsolutePath = Matcher.lastMatcher[ 0 ][ 1 ].trim()
                    def curIncludedFilePath = Matcher.lastMatcher[ 0 ][ 2 ].trim()

                    if ( isAbsolutePath ) {
                        retSet.add( "/" + curIncludedFilePath )
                    }
                    else {
                        retSet.add( clientLibraryResource.getPath() + "/" + base + curIncludedFilePath )
                    }

                    break

            }
        }

        return retSet

    }

    private String mergeCssFiles() {

        return mergeFiles( cssResourcePaths, "/* ", " */" )

    }

    private String mergeJsFiles() {

        return mergeFiles( jsResourcePaths, "//" )

    }

    private String mergeFiles( Set<String> fileSet, String commentPrefix, String commentPostfix = "" ) {

        StringBuffer mergedFileData = new StringBuffer()

        fileSet.each {

            mergedFileData.append( commentPrefix ).append( it ).append( commentPostfix ).append( "\n\n" )

            mergedFileData.append(
                    getDataForFileResource( resourceResolver.getResource( it ) ) ).append( "\n" )

        }

        return mergedFileData.toString()

    }

    private static String getDataForFileResource( Resource resource ) {
        Reader fileReader = getReaderForBinaryResource( resource )

        StringBuffer currentFileData = new StringBuffer()

        if ( fileReader ) {
            fileReader.eachLine { line ->
                currentFileData.append( line + "\n" )
            }

            return currentFileData.toString()
        }

        return null
    }

    private static Reader getReaderForBinaryResource( Resource resource ) {

        if ( !resource || ResourceUtil.isNonExistingResource( resource ) ) {
            LOG.debug("Null or non existant resource passed to get reader method" )
            return null
        }

        Node node = resource.adaptTo( Node.class )

        try {
            Property dataProperty = node.getProperty( "jcr:content/jcr:data" )

            return new InputStreamReader( dataProperty.getBinary().getStream(), UTF_8_ENCODING )

        }
        catch ( e ) {

            LOG.error( "Error encountered attempting to create a reader for resource " + resource.getPath(), e )
            return null

        }

    }

    private long getModifiedTimestampForFileResource( Resource fileResource ) {


    }

    private static long getLastModifiedTimestampForResource( Resource resource ) {

        ValueMap resourceValueMap = resource.adaptTo(ValueMap.class)

        resourceValueMap.get("jcr:lastModified", 0L)

    }





    /**
     *
     * @return A String representation of the merged set of CSS files contained in this client library.
     *         This String includes the self-contained files and as such does not contain embeds or dependencies.
     */
    public String getCss() {
        return mergeCssFiles()
    }

    /**
     *
     * @return A String representation of the merged set of JS files contained in this client library.
     *         This String includes the self-contained files and as such does not contain embeds or dependencies.
     */
    public String getJs() {
        return mergeJsFiles()
    }

    public boolean hasCss() {
        return isCssLibrary
    }

    public boolean hasJs() {
        return isJsLibrary
    }

    public boolean hasLess() {
        return isLessLibrary
    }

    public boolean hasSass() {
        return isSassLibrary
    }

    public String getClientLibraryPath() {
        return clientLibraryResource.getPath()
    }

    public Set<String> getJsResourcePaths() {
        return jsResourcePaths
    }

    public Set<String> getCssResourcePaths() {
        return cssResourcePaths
    }

    public Set<String> getClientLibraryResourcePaths() {

        Set<String> retSet = Sets.newLinkedHashSet( )

        retSet.addAll( jsResourcePaths )
        retSet.addAll( cssResourcePaths )

        return retSet

    }

    public Set<String> getLibraryIncludeFilePaths() {
        Set<String> retSet = Sets.newHashSet()

        if ( cssIncludeFilePathOptional.isPresent() ) {
            retSet.add( cssIncludeFilePathOptional.get() )
        }
        if ( jsIncludeFilePathOptional.isPresent() ) {
            retSet.add( jsIncludeFilePathOptional.get() )
        }

        return retSet
    }

    public Set<String> getCategories() {
        return categories
    }

    public List<String> getEmbeddedCategories() {
        embeddedCategories
    }

    @Override
    Set<SlingRunModeGroup> getRunModeGroups() {
        return runModeGroups
    }

    @Override
    Boolean isIncludedForRunModes(Set<String> runModes) {

        return getRunModeGroups().isEmpty() || getRunModeGroups().any {
            it.matches(runModes)
        }

    }

    public List<String> getDependencies() {
        dependencies
    }

    @Override
    public String toString() {
        def stringBuilder = new StringBuilder()

        stringBuilder << "Client Library ["
        stringBuilder << categories
        stringBuilder << "]"

        return stringBuilder.toString()
    }
}


