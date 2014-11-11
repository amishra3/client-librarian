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
import com.citytechinc.cq.clientlibs.api.services.clientlibs.ClientLibraryManager
import com.citytechinc.cq.clientlibs.api.services.clientlibs.cache.ClientLibraryCacheManager
import com.citytechinc.cq.clientlibs.core.domain.library.ClientLibraries
import com.citytechinc.cq.clientlibs.core.listeners.library.factory.impl.DefaultClientLibraryEventFactory
import com.citytechinc.cq.clientlibs.core.listeners.library.impl.ClientLibraryEventListener
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
import javax.jcr.observation.ObservationManager
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

    @org.apache.felix.scr.annotations.Reference
    private ClientLibraryCacheManager clientLibraryCacheManager

    private Session session

    private ClientLibraryEventListener clientLibraryEventListener

    @Override
    Set<ClientLibrary> getLibraries() {

        synchronized (this) {

            refreshIfNotInitialized()
            return ImmutableSet.copyOf(clientLibrarySet)

        }
    }

    @Override
    ClientLibrary getLibrary(String path) {

        synchronized (this) {

            refreshIfNotInitialized()
            return ImmutableSet.copyOf(clientLibrarySet).find { it.clientLibraryPath == path }

        }

    }

    @Override
    Set<ClientLibrary> getLibrariesForCategory(String category) {

        synchronized (this) {

            refreshIfNotInitialized()
            if (clientLibrariesByCategoryMap.containsKey(category)) {
                return ImmutableSet.copyOf(clientLibrariesByCategoryMap.get(category))
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

        ObservationManager observationManager = administrativeSession.workspace.observationManager
        clientLibraryEventListener = new ClientLibraryEventListener(new DefaultClientLibraryEventFactory(), this, clientLibraryCacheManager, session)
        observationManager.addEventListener(clientLibraryEventListener, 31, "/", true, null, null, true)

    }

    @Deactivate
    protected void deactivate() {

        if (clientLibraryEventListener != null) {
            administrativeSession.workspace.observationManager.removeEventListener(clientLibraryEventListener)
            clientLibraryEventListener = null;
        }

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
