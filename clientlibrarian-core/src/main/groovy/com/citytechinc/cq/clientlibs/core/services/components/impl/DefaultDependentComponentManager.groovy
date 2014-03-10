package com.citytechinc.cq.clientlibs.core.services.components.impl

import com.citytechinc.cq.clientlibs.core.domain.component.Components
import com.citytechinc.cq.clientlibs.api.domain.component.DependentComponent
import com.citytechinc.cq.clientlibs.api.services.components.DependentComponentManager
import com.google.common.base.Optional
import com.google.common.collect.ImmutableMap
import org.apache.felix.scr.annotations.Activate
import org.apache.felix.scr.annotations.Component
import org.apache.felix.scr.annotations.Deactivate
import org.apache.felix.scr.annotations.Service
import org.apache.sling.api.resource.LoginException
import org.apache.sling.api.resource.Resource
import org.apache.sling.api.resource.ResourceResolver
import org.apache.sling.api.resource.ResourceResolverFactory
import org.apache.sling.api.resource.ValueMap
import org.apache.sling.jcr.api.SlingRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.jcr.RepositoryException
import javax.jcr.Session
import javax.jcr.query.Query
import javax.jcr.query.QueryManager

@Component(
        label="Default Dependent Component Manager",
        description="A Dependent Component Manager which maintains a list of all cq:Component nodes in the repository and listens for updates to those nodes and the addition or removal of such nodes" )
@Service
class DefaultDependentComponentManager implements DependentComponentManager {

    private static final String ALL_COMPONENTS_QUERY = "SELECT * FROM \"cq:Component\""

    private static final Logger LOG = LoggerFactory.getLogger(DefaultDependentComponentManager)

    private Set<DependentComponent> dependentComponentSet = [] as Set<DependentComponent>
    private Map<String, DependentComponent> dependentComponentsByComponentPath = [:]
    private Map<String, Set<DependentComponent>> dependentComponentsByClientLibraryCategory = [:]

    private Boolean initialized = false

    @org.apache.felix.scr.annotations.Reference
    private ResourceResolverFactory resourceResolverFactory
    private ResourceResolver resourceResolver

    @org.apache.felix.scr.annotations.Reference
    private SlingRepository repository
    private Session session

    @Override
    Optional<DependentComponent> getDependentComponentForResource(Resource r) {
        return getDependentComponentForResourceType(r.resourceType)
    }

    @Override
    Optional<DependentComponent> getDependentComponentForResourceType(String resourceType) {

        synchronized (this) {

            refreshIfNotInitialized()
            return lookupDependentComponentForResourceType(resourceType)

        }

    }

    @Override
    Set<DependentComponent> getComponentsDependentOnLibraryCategory(String category) {

        synchronized (this) {

            refreshIfNotInitialized()
            if (dependentComponentsByClientLibraryCategory.containsKey(category)) {
                return dependentComponentsByClientLibraryCategory.get(category)
            }

            return [] as Set<DependentComponent>
        }

    }

    @Override
    Map<String, DependentComponent> getComponentsByPath() {

        synchronized (this) {

            refreshIfNotInitialized()
            return ImmutableMap.copyOf(dependentComponentsByComponentPath)

        }

    }

    @Override
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

    protected void refreshIfNotInitialized() {
        if (!initialized) {
            refresh()
        }
    }

    protected void invalidateSelf() {

        initialized = false
        dependentComponentSet.clear()
        dependentComponentsByComponentPath.clear()
        dependentComponentsByClientLibraryCategory.clear()

    }

    protected Optional<DependentComponent> lookupDependentComponentForResourceType(String resourceType) {

        for (String currentSearchPath : administrativeResourceResolver.searchPath) {
            def componentPath = currentSearchPath + "/" + resourceType
            if (dependentComponentsByComponentPath.containsKey(componentPath)) {
                return Optional.fromNullable(dependentComponentsByComponentPath.get(componentPath))
            }
        }

        return Optional.absent()

    }

    protected void refresh() {

        invalidateSelf()

        QueryManager queryManager = getAdministrativeSession().getWorkspace().getQueryManager()

        def componentsWithDependenciesQuery =
            queryManager.createQuery(ALL_COMPONENTS_QUERY, Query.JCR_SQL2)

        def result = componentsWithDependenciesQuery.execute()

        def resultIterator = result.rows

        def resourcesForComponentsWithoutDependencies = []

        while (resultIterator.hasNext()) {
            def currentResult = resultIterator.nextRow()

            def componentResource = administrativeResourceResolver.getResource( currentResult.path )
            def componentValueMap = componentResource.adaptTo( ValueMap.class )

            /*
             * If the component definition has a dependency property - store it in our dependent component set
             */
            if (componentValueMap.containsKey("dependencies")) {
                def componentDependencies = componentValueMap.get("dependencies", new String[0]) as Set<String>

                def newDependentComponent = Components.forResourceAndDependencies(componentResource, componentDependencies)
                dependentComponentSet.add(newDependentComponent)
                dependentComponentsByComponentPath.put(componentResource.getPath(), newDependentComponent)
            }
            else {
                resourcesForComponentsWithoutDependencies.add( componentResource )
            }
        }

        /*
         * Iterate through the component definitions which did not have dependencies and see if their
         * super types have dependencies
         */
        def componentsAddedInPreviousIteration = true
        def componentsToInspect = resourcesForComponentsWithoutDependencies

        while (componentsAddedInPreviousIteration) {
            componentsAddedInPreviousIteration = false

            def nextComponentsToInspect = []

            componentsToInspect.each { Resource currentComponentResource ->
                if (currentComponentResource.resourceSuperType != null) {
                    def superDependentComponentOptional = lookupDependentComponentForResourceType(currentComponentResource.resourceSuperType)

                    if (superDependentComponentOptional.isPresent()) {
                        def newDependentComponent = Components.forResourceAndSuperDependentComponent(currentComponentResource, superDependentComponentOptional.get())
                        dependentComponentSet.add(newDependentComponent)
                        dependentComponentsByComponentPath.put(currentComponentResource.getPath(), newDependentComponent)
                        componentsAddedInPreviousIteration = true
                    }
                    else {
                        nextComponentsToInspect.add(currentComponentResource)
                    }
                }
            }

            componentsToInspect = nextComponentsToInspect
        }

        dependentComponentSet.each { DependentComponent currentDependentComponent ->
            currentDependentComponent.dependencies.each { String currentLibraryCategory ->
                if (!dependentComponentsByClientLibraryCategory.containsKey(currentLibraryCategory)) {
                     dependentComponentsByClientLibraryCategory[currentLibraryCategory] = [] as Set<DependentComponent>
                }

                dependentComponentsByClientLibraryCategory[currentLibraryCategory].add(currentDependentComponent)
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
