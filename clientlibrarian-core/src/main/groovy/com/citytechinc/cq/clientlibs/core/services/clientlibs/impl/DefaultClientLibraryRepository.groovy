/**
 * Copyright 2014, CITYTECH, Inc.
 * All rights reserved - Do Not Redistribute
 * Confidential and Proprietary
 */
package com.citytechinc.cq.clientlibs.core.services.clientlibs.impl

import com.citytechinc.cq.clientlibs.api.domain.library.ClientLibrary
import com.citytechinc.cq.clientlibs.api.domain.library.LibraryType
import com.citytechinc.cq.clientlibs.api.domain.library.exceptions.InvalidClientLibraryCategoryException
import com.citytechinc.cq.clientlibs.api.services.clientlibs.ClientLibraryManager
import com.citytechinc.cq.clientlibs.api.services.clientlibs.ClientLibraryRepository
import com.citytechinc.cq.clientlibs.api.services.clientlibs.ResourceDependencyProvider
import com.citytechinc.cq.clientlibs.api.services.clientlibs.compilers.less.LessCompiler
import com.citytechinc.cq.clientlibs.api.services.clientlibs.exceptions.ClientLibraryCompilationException
import com.citytechinc.cq.clientlibs.api.services.clientlibs.transformer.VariableProvider
import com.citytechinc.cq.clientlibs.core.services.clientlibs.state.manager.impl.ClientLibraryRepositoryStateManager
import com.citytechinc.cq.clientlibs.api.services.components.DependentComponentManager
import com.google.common.base.Optional
import com.google.common.collect.ImmutableList
import org.apache.commons.lang.StringUtils
import org.apache.felix.scr.annotations.*
import org.apache.sling.api.resource.LoginException
import org.apache.sling.api.resource.Resource
import org.apache.sling.settings.SlingSettingsService
import org.mozilla.javascript.RhinoException
import org.osgi.framework.Constants
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.jcr.RepositoryException
import javax.jcr.query.InvalidQueryException

import org.apache.sling.commons.osgi.PropertiesUtil

/**
 *
 */
@Component( label="AEM Client Librarian Repository Service", description="" )
@Service
@Properties( [
    @Property( name = Constants.SERVICE_VENDOR, value = "CITYTECH, Inc." ) ] )
class DefaultClientLibraryRepository implements ClientLibraryRepository {


    private static final Logger LOG = LoggerFactory.getLogger(DefaultClientLibraryRepository.class)

    @Reference( cardinality = ReferenceCardinality.MANDATORY_MULTIPLE, policy = ReferencePolicy.DYNAMIC, bind = "bindDependencyProvider", unbind = "unbindDependencyProvider", referenceInterface = ResourceDependencyProvider )
    private final List<ResourceDependencyProvider> resourceDependencyProviderList = []

    @Reference( cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC, bind = "bindVariableProvider", unbind = "unbindVariableProvider", referenceInterface = VariableProvider )
    private final List<VariableProvider> variableProviderList = []

    @Reference
    private ClientLibraryManager clientLibraryManager

    @Reference
    private DependentComponentManager dependentComponentManager

    @Reference
    private LessCompiler lessCompiler

    @Reference
    SlingSettingsService slingSettingsService

    @Property(label = "Strict Javascript", boolValue = false, description = "When set to true rendered JavaScript page libraries will start with a 'strict' directive")
    private static final String STRICT_JAVASCRIPT = "strictJavascript"
    private Boolean strictJavascript

    private ClientLibraryRepositoryStateManager stateManager

    @Activate
    protected void activate( Map<String, Object> properties ) throws RepositoryException, LoginException {

        LOG.debug( "Activating Service" )

        stateManager = new ClientLibraryRepositoryStateManager( clientLibraryManager, dependentComponentManager )

        strictJavascript = PropertiesUtil.toBoolean(properties.get(STRICT_JAVASCRIPT), false)

    }

    @Modified
    protected void modified( Map<String, Object> properties ) {

        strictJavascript = PropertiesUtil.toBoolean(properties.get(STRICT_JAVASCRIPT), false)

    }

    @Deactivate
    protected void deactivate() {

        LOG.debug( "Deactivating Service" )
        stateManager = null

        LOG.debug( "Deactivation Completed" )

    }

    protected void bindDependencyProvider(ResourceDependencyProvider resourceDependencyProvider) {

        LOG.debug("Binding ResourceDependencyProvider " + resourceDependencyProvider)

        synchronized (resourceDependencyProviderList) {
            if (resourceDependencyProviderList.contains(resourceDependencyProvider)) {
                LOG.error(resourceDependencyProvider + " already exists in the services Resource Dependency Provider List")
            }
            else {
                resourceDependencyProviderList.add(resourceDependencyProvider)
            }
        }
    }

    protected void unbindDependencyProvider(ResourceDependencyProvider resourceDependencyProvider) {

        LOG.debug("Unbinding ResourceDependencyProvider " + resourceDependencyProvider)

        synchronized (resourceDependencyProviderList) {
            if (resourceDependencyProviderList.contains(resourceDependencyProvider)) {
                resourceDependencyProviderList.remove(resourceDependencyProvider)
            }
            else {
                LOG.error("An attempt to unbind " + resourceDependencyProvider + " was made however this dependency provider is not in the current list of known providers")
            }
        }
    }

    protected void bindVariableProvider(VariableProvider variableProvider) {

        LOG.debug("Binding VariableProvider " + variableProvider)

        synchronized (variableProviderList) {
            if (variableProviderList.contains(variableProvider)) {
                LOG.error(variableProvider + " already exists in the service's Variable Provider List")
            }
            else {
                variableProviderList.add(variableProvider)
            }
        }

    }

    protected void unbindVariableProvider(VariableProvider variableProvider) {

        LOG.debug("Unbinding VariableProvider " + variableProvider)

        synchronized (variableProviderList) {
            if (variableProviderList.contains(variableProvider)) {
                variableProviderList.remove(variableProvider)
            }
            else {
                LOG.error("An attempt to unbind " + variableProvider + " was made however this dependency provider is not in the current list of known providers")
            }
        }

    }

    @Override
    public Integer getClientLibraryCount() {
        return stateManager.requestStateStatistics().clientLibraryCount
    }

    @Override
    public void refresh() throws RepositoryException, LoginException {
        stateManager.requestRefresh();
    }

    @Override
    public String compileClientLibrary(Resource root, LibraryType type, Optional<String> brand) throws ClientLibraryCompilationException {

        try {
            List<ClientLibrary> dependencies = getOrderedDependencies( root )

            Set<String> currentRunModes = slingSettingsService.runModes

            LOG.debug( "Found dependencies for " + root.getPath() + " : " + dependencies )

            /*
             * Filter the included libraries based on run mode
             */
            List<ClientLibrary> filteredDependencies = dependencies.findAll { ClientLibrary currentDependency ->

                return currentDependency.isIncludedForRunModes(currentRunModes) &&
                        currentDependency.isIncludedForBrand(brand)

            }

            LOG.debug( "Filtered dependencies for " + root.getPath() + " : " + filteredDependencies )

            if ( type == LibraryType.CSS ) {
                String compiledCssLibrary = compileCSSClientLibrary( root, filteredDependencies )

                return compiledCssLibrary
            }
            else if ( type == LibraryType.JS ) {
                String compiledJsLibrary = compileJSClientLibrary( root, filteredDependencies )

                return compiledJsLibrary

            }

            return null

        } catch ( InvalidClientLibraryCategoryException e ) {
            throw new ClientLibraryCompilationException( "Invalid Client Library Exception hit in attempting to build library", e )
        }
    }

    /**
     *
     * TODO: Optimize this implementation so that it doesn't run replace all a billion times
     *
     * @param library
     * @return
     */
    private String transformLibrary(Resource root, String library) {

        String retLibrary = library
        List<VariableProvider> variableProviderListCopy = null

        synchronized (variableProviderList) {
            variableProviderListCopy = ImmutableList.copyOf(variableProviderList)
        }

        Map<String, String> variables = [:]

        variableProviderListCopy.each {
            variables.putAll(it.getVariables(root))
        }

        variables.each { k, v ->
            retLibrary = StringUtils.replace(retLibrary, "<%" + k + "%>", v)
        }

        return retLibrary

    }

    private List<ClientLibrary> getOrderedDependencies( Resource root ) throws InvalidQueryException, RepositoryException, InvalidClientLibraryCategoryException {

        List<ResourceDependencyProvider> resourceDependencyProviderListCopy = null

        synchronized (resourceDependencyProviderList) {
            resourceDependencyProviderListCopy = ImmutableList.copyOf(resourceDependencyProviderList)
        }

        return stateManager.requestOrderedDependencies( root, resourceDependencyProviderListCopy )

    }

    private String compileJSClientLibrary( Resource root, List<ClientLibrary> dependencies ) {

        StringBuffer mergedClientLibraries = new StringBuffer();

        if (strictJavascript) {
            mergedClientLibraries.append("\"use strict\";").append("\n")
        }

        for (ClientLibrary curClientLibrary : dependencies) {
            if (curClientLibrary.hasJs()) {
                mergedClientLibraries.append(curClientLibrary.getJs()).append("\n");
            }
        }

        String transformedJsLibrary = transformLibrary(root, mergedClientLibraries.toString())

        return transformedJsLibrary;

    }

    private String compileCSSClientLibrary( Resource root, List<ClientLibrary> dependencies ) throws ClientLibraryCompilationException {

        boolean usesLess = false;
        boolean usesSass = false;

        StringBuffer mergedClientLibraries = new StringBuffer();

        for (ClientLibrary curClientLibrary : dependencies) {
            if (curClientLibrary.hasCss()) {
                mergedClientLibraries.append(curClientLibrary.getCss()).append("\n");

                if (curClientLibrary.hasLess()) {
                    usesLess = true;
                }

                if (curClientLibrary.hasSass()) {
                    usesSass = true;
                }

                if (usesLess && usesSass) {
                    throw new ClientLibraryCompilationException("CSS Library can only use one compilation language");
                }
            }
        }

        String transformedCssLibrary = transformLibrary(root, mergedClientLibraries.toString())

        if (usesLess) {
            try {
                return lessCompiler.compile(transformedCssLibrary);
            } catch (RhinoException e) {
                StringBuffer errorMessageBuffer = new StringBuffer( "Rhino Exception encountered while compiling CSS Library \n" )
                errorMessageBuffer.append( e.details() ).append( "\n" )
                errorMessageBuffer.append( "at line " ).append( e.lineNumber() ).append( "\n" )
                errorMessageBuffer.append( e.lineSource() ).append( "\n" )
                errorMessageBuffer.append( e.getScriptStackTrace() )

                LOG.error( errorMessageBuffer.toString() )
                throw new ClientLibraryCompilationException( "Rhino exception encountered while compiling CSS Library", e )
            } catch (IOException e) {
                LOG.error( "Error encountered during LESS compilation", e );
                throw new ClientLibraryCompilationException( "Exception encountered during LESS compilation", e );
            }
        }

        return transformedCssLibrary;
    }

}
