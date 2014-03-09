/**
 * Copyright 2014, CITYTECH, Inc.
 * All rights reserved - Do Not Redistribute
 * Confidential and Proprietary
 */
package com.citytechinc.cq.clientlibs.services.clientlibs.impl

import com.citytechinc.cq.clientlibs.domain.library.ClientLibrary
import com.citytechinc.cq.clientlibs.domain.library.LibraryType
import com.citytechinc.cq.clientlibs.domain.library.exceptions.InvalidClientLibraryCategoryException
import com.citytechinc.cq.clientlibs.services.clientlibs.ClientLibraryManager
import com.citytechinc.cq.clientlibs.services.clientlibs.ClientLibraryRepository
import com.citytechinc.cq.clientlibs.services.clientlibs.ResourceDependencyProvider
import com.citytechinc.cq.clientlibs.services.clientlibs.compilers.less.LessCompiler
import com.citytechinc.cq.clientlibs.services.clientlibs.exceptions.ClientLibraryCompilationException
import com.citytechinc.cq.clientlibs.services.clientlibs.state.manager.impl.ClientLibraryRepositoryStateManager
import com.citytechinc.cq.clientlibs.services.components.DependentComponentManager
import org.apache.felix.scr.annotations.*
import org.apache.sling.api.resource.LoginException
import org.apache.sling.api.resource.Resource
import org.mozilla.javascript.RhinoException
import org.osgi.framework.Constants
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.jcr.RepositoryException
import javax.jcr.query.InvalidQueryException

/**
 *
 */
@Component( label="AEM Client Librarian Repository Service", description="" )
@Service
@Properties( [
    @Property( name = Constants.SERVICE_VENDOR, value = "CITYTECH, Inc." ) ] )
class DefaultClientLibraryRepository implements ClientLibraryRepository {


    private static final Logger LOG = LoggerFactory.getLogger(DefaultClientLibraryRepository.class)

    @Reference( cardinality = ReferenceCardinality.MANDATORY_MULTIPLE, bind = "bindDependencyProvider", unbind = "unbindDependencyProvider", referenceInterface = ResourceDependencyProvider )
    private final List<ResourceDependencyProvider> resourceDependencyProviderList = []

    @Reference
    private ClientLibraryManager clientLibraryManager

    @Reference
    private DependentComponentManager dependentComponentManager

    @Reference
    private LessCompiler lessCompiler

    //@Reference
    //private ClientLibraryEventFactory clientLibraryEventFactory

    private ClientLibraryRepositoryStateManager stateManager

    @Activate
    protected void activate( Map<String, Object> properties ) throws RepositoryException, LoginException {

        LOG.debug( "Activating Service" )

        stateManager = new ClientLibraryRepositoryStateManager( clientLibraryManager, dependentComponentManager )

        /*
         * Setup a new ClientLibraryEventListener listening for all event types on the entire repository.
         * The listener's onEvent method will be responsible for filtering out and acting on only those events
         * which we actually care about
         *
        def newEventListener = new ClientLibraryEventListener( this, clientLibraryEventFactory, getAdministrativeSession() )
        observationManager.addEventListener(
                newEventListener,
                31, "/", true, null, null, true )

        repositoryState.setEventListener( newEventListener )

        LOG.debug( "Event listener added to observation manager" )
         */
    }

    @Deactivate
    protected void deactivate() {

        LOG.debug( "Deactivating Service" )
        stateManager = null

        LOG.debug( "Deactivation Completed" )

    }

    protected void bindDependencyProvider(ResourceDependencyProvider resourceDependencyProvider) {
        if (resourceDependencyProviderList.contains(resourceDependencyProvider)) {
            LOG.error(resourceDependencyProvider + " already exists in the services Resource Dependency Provider List")
        }
        else {
            resourceDependencyProviderList.add(resourceDependencyProvider)
        }
    }

    protected void unbindDependencyProvider(ResourceDependencyProvider resourceDependencyProvider) {
        if (resourceDependencyProviderList.contains(resourceDependencyProvider)) {
            resourceDependencyProviderList.remove(resourceDependencyProvider)
        }
        else {
            LOG.error("An attempt to unbind " + resourceDependencyProvider + " was made however this dependency provider is not in the current list of known providers")
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
    public String compileClientLibrary(Resource root, LibraryType type) throws ClientLibraryCompilationException {

        try {
            List<ClientLibrary> dependencies = getOrderedDependencies( root )

            LOG.debug( "Found dependencies for " + root.getPath() + " : " + dependencies )

            if ( type == LibraryType.CSS ) {
                return compileCSSClientLibrary( root, dependencies )
            }
            else if ( type == LibraryType.JS ) {
                return compileJSClientLibrary( root, dependencies )
            }

            return null

        } catch ( InvalidClientLibraryCategoryException e ) {
            throw new ClientLibraryCompilationException( "Invalid Client Library Exception hit in attempting to build library", e )
        }
    }

    private List<ClientLibrary> getOrderedDependencies( Resource root ) throws InvalidQueryException, RepositoryException, InvalidClientLibraryCategoryException {

        return stateManager.requestOrderedDependencies( root, resourceDependencyProviderList )

    }

    private String compileJSClientLibrary( Resource root, List<ClientLibrary> dependencies ) {

        StringBuffer mergedClientLibraries = new StringBuffer();

        for (ClientLibrary curClientLibrary : dependencies) {
            if (curClientLibrary.hasJs()) {
                mergedClientLibraries.append(curClientLibrary.getJs()).append("\n");
            }
        }

        return mergedClientLibraries.toString();

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

        if (usesLess) {
            try {
                return lessCompiler.compile(mergedClientLibraries.toString());
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

        return mergedClientLibraries.toString();
    }

}
