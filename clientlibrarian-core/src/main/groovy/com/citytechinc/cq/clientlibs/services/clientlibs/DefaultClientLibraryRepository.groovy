/**
 * Copyright 2014, CITYTECH, Inc.
 * All rights reserved - Do Not Redistribute
 * Confidential and Proprietary
 */
package com.citytechinc.cq.clientlibs.services.clientlibs

import com.citytechinc.cq.clientlibs.domain.library.ClientLibrary
import com.citytechinc.cq.clientlibs.domain.library.LibraryType
import com.citytechinc.cq.clientlibs.domain.library.exceptions.InvalidClientLibraryCategoryException
import com.citytechinc.cq.clientlibs.services.clientlibs.actor.ClientLibraryRepositoryStateActor
import com.citytechinc.cq.clientlibs.services.clientlibs.actor.messages.ClientLibraryStatisticsRequestMessage
import com.citytechinc.cq.clientlibs.services.clientlibs.actor.messages.OrderedDependenciesRequestMessage
import com.citytechinc.cq.clientlibs.services.clientlibs.actor.messages.RefreshMessage
import com.citytechinc.cq.clientlibs.services.clientlibs.compilers.less.LessCompiler
import com.citytechinc.cq.clientlibs.services.clientlibs.exceptions.ClientLibraryCompilationException
import com.citytechinc.cq.clientlibs.services.clientlibs.state.ClientLibraryStateStatistics
import groovyx.gpars.actor.Actor
import org.apache.felix.scr.annotations.*
import org.apache.sling.api.resource.LoginException
import org.apache.sling.api.resource.Resource
import org.apache.sling.api.resource.ResourceResolver
import org.apache.sling.api.resource.ResourceResolverFactory
import org.apache.sling.jcr.api.SlingRepository
import org.mozilla.javascript.RhinoException
import org.osgi.framework.Constants
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.jcr.RepositoryException
import javax.jcr.Session
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

    @Reference
    private LessCompiler lessCompiler

    //@Reference
    //private ClientLibraryEventFactory clientLibraryEventFactory

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private SlingRepository repository;

    private ResourceResolver resourceResolver;

    private Session session;

    private ClientLibraryRepositoryStateActor stateActor

    @Activate
    protected void activate( Map<String, Object> properties ) throws RepositoryException, LoginException {

        LOG.debug( "Activating Service" )

        stateActor = new ClientLibraryRepositoryStateActor( getAdministrativeResourceResolver(), getAdministrativeSession().getWorkspace().getQueryManager() )

        LOG.debug( "Starting State Actor" )

        stateActor.start()

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
        LOG.debug( "Stopping the State Actor" )
        stateActor.stop()

        LOG.debug( "Nulling out the State Actor reference" )
        stateActor = null

        LOG.debug( "Closing the Resource Resolver and Administrative Session" )
        closeResourceResolver()
        closeSession()

        LOG.debug( "Deactivation Completed" )

    }

    @Override
    public Integer getClientLibraryCount() {
        ClientLibraryStateStatistics statistics = stateActor.sendAndWait( ClientLibraryStatisticsRequestMessage.instance() )
        return statistics.clientLibraryCount
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
        } catch ( InvalidQueryException e ) {
            throw new ClientLibraryCompilationException( "Query error hit in attempting to build library", e )
        } catch ( RepositoryException e ) {
            throw new ClientLibraryCompilationException( "Repository error hit in attempting to build library", e )
        }
    }

    @Override
    public void refresh() throws RepositoryException, LoginException {
        stateActor.send( RefreshMessage.instance )
    }

    @Override
    public Actor getActor() {
        return stateActor
    }

    private List<ClientLibrary> getOrderedDependencies(Resource root) throws InvalidQueryException, RepositoryException, InvalidClientLibraryCategoryException {

        return stateActor.sendAndWait( OrderedDependenciesRequestMessage.forResource( root ) );

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

    /**
     * Close the administrative resource resolver.  This method should be called by the <code>@Deactivate</code> method
     * of the implementing class if the <code>getAdministrativeResourceResolver()</code> method was used at any time.
     *
     * Copied from the now defunct AbstractSlingService
     */
    protected final void closeResourceResolver() {
        if (resourceResolver != null) {
            resourceResolver.close();
        }
    }

    /**
     * Close the administrative session.  This method should be called by the <code>@Deactivate</code> method of the
     * implementing class if the <code>getAdministrativeSession()</code> method was used at any time.
     *
     * Copied from the now defunct AbstractSlingService
     */
    protected final void closeSession() {
        if (session != null) {
            session.logout();
        }
    }

    /**
     * Get an administrative resource resolver.
     *
     * Copied from the now defunct AbstractSlingService
     *
     * @return resource resolver
     * @throws LoginException if error occurs during authentication
     */
    protected final ResourceResolver getAdministrativeResourceResolver() throws LoginException {
        if (resourceResolver == null) {
            resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
        }

        return resourceResolver;
    }

    /**
     * Get an administrative JCR session.
     *
     * Copied from the now defunct AbstractSlingService
     *
     * @return session
     * @throws RepositoryException if error occurs during authentication
     */
    protected final Session getAdministrativeSession() throws RepositoryException {
        if (session == null) {
            session = repository.loginAdministrative(null);
        }

        return session;
    }


}
