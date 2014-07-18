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
import com.citytechinc.cq.clientlibs.api.structures.graph.DependencyGraph
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

import java.util.concurrent.locks.ReentrantReadWriteLock

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
    protected final List<ResourceDependencyProvider> resourceDependencyProviderList

    @Reference( cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC, bind = "bindVariableProvider", unbind = "unbindVariableProvider", referenceInterface = VariableProvider )
    protected final List<VariableProvider> variableProviderList

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

    protected ClientLibraryRepositoryStateManager stateManager

    protected ReentrantReadWriteLock resourceDependencyProviderListReadWriteLock
    protected ReentrantReadWriteLock variableProviderListReadWriteLock


    protected DefaultClientLibraryRepository(List<ResourceDependencyProvider> resourceDependencyProviderList,
                                             List<VariableProvider> variableProviderList,
                                             ClientLibraryRepositoryStateManager stateManager,
                                             ReentrantReadWriteLock resourceDependencyProviderListReadWriteLock,
                                             ReentrantReadWriteLock variableProviderListReadWriteLock) {

        this.resourceDependencyProviderList = resourceDependencyProviderList;
        this.variableProviderList = variableProviderList;
        this.stateManager = stateManager
        this.resourceDependencyProviderListReadWriteLock = resourceDependencyProviderListReadWriteLock
        this.variableProviderListReadWriteLock = variableProviderListReadWriteLock
    }


    DefaultClientLibraryRepository() {
        this.resourceDependencyProviderList = []
        this.variableProviderList = []
        this.resourceDependencyProviderListReadWriteLock = new ReentrantReadWriteLock(false)
        this.variableProviderListReadWriteLock = new ReentrantReadWriteLock(false)
    }


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
        if(LOG.isDebugEnabled()) {
            LOG.debug("Binding ResourceDependencyProvider " + resourceDependencyProvider)
        }

        boolean resourceDependencyProviderListContains = false

        try {
            this.resourceDependencyProviderListReadWriteLock.readLock().lock()
            resourceDependencyProviderListContains = resourceDependencyProviderList.contains(resourceDependencyProvider);
        } finally {
            this.resourceDependencyProviderListReadWriteLock.readLock().unlock()
        }

        if(resourceDependencyProviderListContains) {
            LOG.error(resourceDependencyProvider + " already exists in the services Resource Dependency Provider List")
        }else {

            try {
                this.resourceDependencyProviderListReadWriteLock.writeLock().lock()
                boolean resourceDependencyProviderListAdd = this.resourceDependencyProviderList.add(resourceDependencyProvider)

                if(resourceDependencyProviderListAdd) {
                    LOG.error(resourceDependencyProvider + " already exists in the services Resource Dependency Provider List after contains check")
                }
            } finally {
                this.resourceDependencyProviderListReadWriteLock.writeLock().unlock()
            }

        }
    }

    protected void unbindDependencyProvider(ResourceDependencyProvider resourceDependencyProvider) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Unbinding ResourceDependencyProvider " + resourceDependencyProvider)
        }

        boolean resourceDependencyProviderListContains = false

        try {
            this.resourceDependencyProviderListReadWriteLock.readLock().lock()
            resourceDependencyProviderListContains = resourceDependencyProviderList.contains(resourceDependencyProvider);
        } finally {
            this.resourceDependencyProviderListReadWriteLock.readLock().unlock()
        }

        if(resourceDependencyProviderListContains) {
            LOG.error("An attempt to unbind " + resourceDependencyProvider + " was made however this dependency provider is not in the current list of known providers")
        }else {

            try {
                this.resourceDependencyProviderListReadWriteLock.writeLock().lock()
                boolean resourceDependencyProviderListRemove = resourceDependencyProviderList.remove(resourceDependencyProvider)

                if(resourceDependencyProviderListRemove) {
                    LOG.error("An attempt to unbind " + resourceDependencyProvider + " was made however this dependency provider is not in the current list of known providers after contains check")
                }
            } finally {
                this.resourceDependencyProviderListReadWriteLock.writeLock().unlock()
            }

        }
    }

    protected void bindVariableProvider(VariableProvider variableProvider) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Binding VariableProvider " + variableProvider)
        }

        boolean variableProviderListContains = false

        try {
            this.variableProviderListReadWriteLock.readLock().lock()
            variableProviderListContains = variableProviderList.contains(variableProvider)
        }finally {
            this.variableProviderListReadWriteLock.readLock().unlock()
        }

        if(variableProviderListContains) {
            LOG.error(variableProvider + " already exists in the service's Variable Provider List")
        }else {
            try {
                this.variableProviderListReadWriteLock.writeLock().lock()
                boolean variableProviderListAdd = this.variableProviderList.add(variableProvider)

                if(variableProviderListAdd) {
                    LOG.error(variableProvider + " already exists in the service's Variable Provider List after contains check")
                }
            }finally {
                this.variableProviderListReadWriteLock.writeLock().unlock()
            }
        }
    }

    protected void unbindVariableProvider(VariableProvider variableProvider) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Unbinding VariableProvider " + variableProvider)
        }

        boolean variableProviderListContains = false

        try {
            this.variableProviderListReadWriteLock.readLock().lock()
            variableProviderListContains = variableProviderList.contains(variableProvider)
        }finally {
            this.variableProviderListReadWriteLock.readLock().unlock()
        }

        if(variableProviderListContains) {
            LOG.error(variableProvider + " already exists in the service's Variable Provider List")
        }else {
            try {
                this.variableProviderListReadWriteLock.writeLock().lock()
                boolean variableProviderListRemove = this.variableProviderList.remove(variableProvider)

                if(variableProviderListRemove) {
                    LOG.error("An attempt to unbind " + variableProvider + " was made however this dependency provider is not in the current list of known providers after contains check")
                }
            }finally {
                this.variableProviderListReadWriteLock.writeLock().unlock()
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

    @Override
    public DependencyGraph<ClientLibrary> getClientLibraryDependencyGraph(Resource root) {

        return getDependencyGraph(root);

    }
/**
     *
     * TODO: Optimize this implementation so that it doesn't run replace all a billion times
     *
     * @param library
     * @return
     */
    protected String transformLibrary(Resource root, String library) {

        String retLibrary = library
        List<VariableProvider> variableProviderListCopy = null

        try {
            this.variableProviderListReadWriteLock.readLock().lock()
            variableProviderListCopy = ImmutableList.copyOf(variableProviderList)
        }finally {
            this.variableProviderListReadWriteLock.readLock().unlock()
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

    protected DependencyGraph<ClientLibrary> getDependencyGraph(Resource root) {
        List<ResourceDependencyProvider> resourceDependencyProviderListCopy = null

        try {
            this.resourceDependencyProviderListReadWriteLock.readLock().lock()
            resourceDependencyProviderListCopy = ImmutableList.copyOf(resourceDependencyProviderList)
        }finally {
            this.resourceDependencyProviderListReadWriteLock.readLock().unlock()
        }

        return stateManager.requestDependencyGraph(root, resourceDependencyProviderListCopy)
    }


    protected List<ClientLibrary> getOrderedDependencies( Resource root ) throws InvalidQueryException, RepositoryException, InvalidClientLibraryCategoryException {
        List<ResourceDependencyProvider> resourceDependencyProviderListCopy = null


        try {
            this.resourceDependencyProviderListReadWriteLock.readLock().lock()
            resourceDependencyProviderListCopy = ImmutableList.copyOf(resourceDependencyProviderList)
        }finally {
            this.resourceDependencyProviderListReadWriteLock.readLock().unlock()
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
