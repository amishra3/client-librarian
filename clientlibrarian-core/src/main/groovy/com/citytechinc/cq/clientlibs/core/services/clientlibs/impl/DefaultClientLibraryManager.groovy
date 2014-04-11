package com.citytechinc.cq.clientlibs.core.services.clientlibs.impl

import com.citytechinc.cq.clientlibs.core.domain.library.ClientLibraries
import com.citytechinc.cq.clientlibs.api.domain.library.ClientLibrary
import com.citytechinc.cq.clientlibs.api.services.clientlibs.ClientLibraryManager
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Sets
import org.apache.felix.scr.annotations.Activate
import org.apache.felix.scr.annotations.Component
import org.apache.felix.scr.annotations.Deactivate
import org.apache.felix.scr.annotations.Service
import org.apache.sling.api.resource.LoginException
import org.apache.sling.api.resource.ResourceResolver
import org.apache.sling.api.resource.ResourceResolverFactory
import org.apache.sling.jcr.api.SlingRepository

import javax.jcr.RepositoryException
import javax.jcr.Session
import javax.jcr.query.Query
import javax.jcr.query.QueryManager


@Component(
        label="Default Client Library Manager",
        description="A Client Library Manager which maintains a list of all cq:ClientLibraryFolder nodes in the repository and listens for changes to those nodes as well as the addition or removal of such nodes" )
@Service
class DefaultClientLibraryManager implements ClientLibraryManager {

    private static final String ALL_CLIENT_LIBRARIES_QUERY = "SELECT * FROM \"cq:ClientLibraryFolder\""

    private Set<ClientLibrary> clientLibrarySet = Sets.newHashSet()
    private Map<String, Set<ClientLibrary>> clientLibrariesByCategoryMap = [:]

    private Boolean initialized = false

    @org.apache.felix.scr.annotations.Reference
    private ResourceResolverFactory resourceResolverFactory
    private ResourceResolver resourceResolver

    @org.apache.felix.scr.annotations.Reference
    private SlingRepository repository
    private Session session

    @Override
    Set<ClientLibrary> getAllLibraries() {

        synchronized (this) {

            refreshIfNotInitialized();
            return ImmutableSet.copyOf(clientLibrarySet);

        }

    }

    @Override
    Set<ClientLibrary> getLibrariesForCategory(String category) {

        synchronized (this) {

            refreshIfNotInitialized()
            if (clientLibrariesByCategoryMap.containsKey(category)) {
                return clientLibrariesByCategoryMap.get(category)
            }

            return Sets.newHashSet()

        }

    }

    @Override
    Map<String, Set<ClientLibrary>> getLibrariesByCategory() {

        synchronized (this) {
            refreshIfNotInitialized()
            return ImmutableMap.copyOf(clientLibrariesByCategoryMap)
        }

    }

    @Override
    Integer getClientLibraryCount() {

        synchronized (this) {
            refreshIfNotInitialized()
            return clientLibrarySet.size()
        }

    }

    void requestRefresh() {

        synchronized (this) {
            refresh()
        }

    }

    @Activate
    protected void activate( Map<String, Object> properties ) throws RepositoryException, LoginException {

    }

    @Deactivate
    protected void deactivate() {

        closeResourceResolver()
        closeSession()

    }

    protected void invalidateSelf() {

        initialized = false
        clientLibrarySet = Sets.newHashSet()
        clientLibrariesByCategoryMap = [:]

    }

    protected void refreshIfNotInitialized() {

        if (!initialized) {
            refresh()
        }

    }

    protected void refresh() {

        invalidateSelf()

        QueryManager queryManager = getAdministrativeSession().getWorkspace().getQueryManager()

        def componentsWithDependenciesQuery =
            queryManager.createQuery(ALL_CLIENT_LIBRARIES_QUERY, Query.JCR_SQL2)

        def result = componentsWithDependenciesQuery.execute()

        def resultIterator = result.rows

        while (resultIterator.hasNext()) {

            def currentRow = resultIterator.nextRow()
            def clientLibraryResource = administrativeResourceResolver.getResource(currentRow.path)

            if (clientLibraryResource != null) {
                ClientLibrary newClientLibrary = ClientLibraries.forResource(clientLibraryResource)
                clientLibrarySet.add(newClientLibrary)

                newClientLibrary.categories.each { String currentCategory ->
                    if (!clientLibrariesByCategoryMap.containsKey(currentCategory)) {
                        clientLibrariesByCategoryMap[currentCategory] = Sets.newHashSet()
                    }

                    clientLibrariesByCategoryMap[currentCategory].add(newClientLibrary)
                }
            }
        }

        initialized = true

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
