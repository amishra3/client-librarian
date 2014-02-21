package com.citytechinc.cq.clientlibs.services.clientlibs.actor

import com.citytechinc.cq.clientlibs.domain.component.Components
import com.citytechinc.cq.clientlibs.domain.component.DependentComponent
import com.citytechinc.cq.clientlibs.domain.library.ClientLibraries
import com.citytechinc.cq.clientlibs.domain.library.ClientLibrary
import com.citytechinc.cq.clientlibs.domain.library.exceptions.InvalidClientLibraryCategoryException
import com.citytechinc.cq.clientlibs.services.clientlibs.actor.messages.ClientLibraryStatisticsRequestMessage
import com.citytechinc.cq.clientlibs.services.clientlibs.actor.messages.OrderedDependenciesRequestMessage
import com.citytechinc.cq.clientlibs.services.clientlibs.actor.messages.RefreshMessage
import com.citytechinc.cq.clientlibs.services.clientlibs.state.builder.ClientLibraryStateStatisticsBuilder
import com.citytechinc.cq.clientlibs.structures.graph.dag.DirectedAcyclicGraph
import com.citytechinc.cq.clientlibs.util.ComponentUtils
import com.google.common.base.Stopwatch
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.google.common.collect.Sets
import groovyx.gpars.actor.DynamicDispatchActor
import org.apache.commons.lang.StringUtils
import org.apache.sling.api.resource.Resource
import org.apache.sling.api.resource.ResourceResolver
import org.apache.sling.api.resource.ValueMap
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.jcr.RepositoryException
import javax.jcr.query.InvalidQueryException
import javax.jcr.query.Query
import javax.jcr.query.QueryManager
import java.util.concurrent.TimeUnit

class ClientLibraryRepositoryStateActor extends DynamicDispatchActor {

    private static final Logger LOG = LoggerFactory.getLogger( ClientLibraryRepositoryStateActor )

    private static final String COMPONENTS_WITH_DEPENDENCIES_QUERY = "SELECT * FROM \"cq:Component\" WHERE dependencies IS NOT NULL"
    private static final String INHERITING_COMPONENTS_WITHOUT_DEPENDENCIES_QUERY = "SELECT * FROM \"cq:Component\" WHERE dependencies IS NULL AND [sling:resourceSuperType] IS NOT NULL"
    private static final String CLIENT_LIBRARY_QUERY = "SELECT * FROM \"cq:ClientLibraryFolder\""

    private final ResourceResolver resourceResolver
    private final QueryManager queryManager

    /*
     * A mapping of component sling:resourceTypes to DependentComponent objects.
     * The DependentComponent, among other things, indicates the library categories
     * which the component is dependent on.
     *
     * It should be noted that the keys in this map are the full paths to the component
     * definitions and as such are prefixed with /apps/ or /libs/ depending on where they
     * came from.
     */
    private final Map<String, DependentComponent> componentMap

    /*
     * A list of all known Client Libraries
     */
    private final List<ClientLibrary> clientLibraries;

    /*
     * A mapping between library categories and the ClientLibraries which indicate that they
     * are direct members of said category.
     */
    private final Map<String, Set<ClientLibrary>> clientLibraryCategoryMap;

    /*
     * A mapping between paths in the repository and ClientLibrary objects.  The paths
     * specified are the paths of the cq:ClientLibraryFolder nodes representing the
     * client library object.
     */
    private final Map<String, ClientLibrary> clientLibraryPathMap;

    /*
     * A mapping between the sling:resourceType of components and the ClientLibrary
     * objects which they directly depend on.
     *
     * It should be noted that the keys in this map are the full paths to the component
     * definitions and as such are prefixed with /apps/ or /libs/ depending on where they
     * came from.
     */
    private final Map<String, Set<ClientLibrary>> componentDependencyMap;

    private Boolean initialized

    public ClientLibraryRepositoryStateActor( ResourceResolver resourceResolver, QueryManager queryManager ) {
        this.resourceResolver = resourceResolver
        this.queryManager = queryManager

        componentMap = Maps.newHashMap()
        clientLibraries = Lists.newArrayList()
        clientLibraryCategoryMap = Maps.newHashMap()
        clientLibraryPathMap = Maps.newHashMap()
        componentDependencyMap = Maps.newHashMap()

        initialized = false
    }


    void onMessage( RefreshMessage m ) {
        LOG.debug( "Received RefreshMessage" )

        initialized = false

        refresh()
    }

    void onMessage( OrderedDependenciesRequestMessage m ) {
        if ( !initialized ) {
            refresh()
        }

        reply( getOrderedDependencies( m.resource ) )
    }

    void onMessage( ClientLibraryStatisticsRequestMessage m ) {
        if ( !initialized ) {
            refresh()
        }

        def statisticsBuilder = ClientLibraryStateStatisticsBuilder.cleanBuilder

        statisticsBuilder.setClientLibraryCount( clientLibraries.size() )

        reply( statisticsBuilder.build() )

    }

    void onException( Throwable e ) {
        LOG.error( "Exception encountered in the State Actor", e )
    }

    protected void refresh() {

        LOG.debug( "Refreshing the Client Library State" )
        Stopwatch stopwatch = Stopwatch.createStarted()

        setComponents( collectComponents( queryManager, resourceResolver ) )
        setClientLibraries( collectClientLibraries( queryManager, resourceResolver ) )

        refreshComponentDependencyMap()

        initialized = true

        stopwatch.stop()
        LOG.debug( "Repository state refresh completed in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + "ms" )

    }

    /**
     * Add a list of component instances to the internal state representation.
     *
     * This method will invalidate the internal dependency map and as such, #refreshDependencyMap will need
     * to be called some time after this method
     *
     * @param components The List of components to add
     */
    protected void addComponents(List<DependentComponent> components) {

        for (DependentComponent curComponent : components) {
            componentMap.put(curComponent.getResource().getPath(), curComponent);
        }

    }

    /**
     * Clear any existing components from the internal state and adds all provided components to the
     * internal state.
     *
     * This method will invalidate the internal dependency map and as such, #refreshDependencyMap will need
     * to be called some time after this method
     *
     * @param components
     */
    protected void setComponents(List<DependentComponent> components) {

        componentMap.clear();
        addComponents(components);

    }

    protected void addClientLibraries(List<ClientLibrary> libraries) {

        for (ClientLibrary curLibrary : libraries) {
            clientLibraries.add(curLibrary);
            clientLibraryPathMap.put(curLibrary.getClientLibraryPath(), curLibrary);

            for (String curCategory : curLibrary.getCategories()) {
                if (!clientLibraryCategoryMap.containsKey(curCategory)) {
                    Set<ClientLibrary> newClientLibrarySet = Sets.newHashSet();
                    clientLibraryCategoryMap.put(curCategory, newClientLibrarySet);
                }

                clientLibraryCategoryMap.get(curCategory).add(curLibrary);
            }
        }

        invalidateDependencyMap();

    }

    protected void setClientLibraries( List<ClientLibrary> libraries ) {

        clientLibraries.clear();
        clientLibraryPathMap.clear();
        clientLibraryCategoryMap.clear();
        addClientLibraries(libraries);

    }

    protected void invalidateDependencyMap() {
        componentDependencyMap.clear();
    }

    protected void refreshComponentDependencyMap() {

        LOG.debug("Populating the Component Dependency Map");

        invalidateDependencyMap();

        for (String curComponentMapKey : componentMap.keySet()) {
            DependentComponent curDependentComponent = componentMap.get(curComponentMapKey);

            Set<ClientLibrary> componentLibraries = Sets.newHashSet();

            for (String curComponentDependency : curDependentComponent.getDependencies()) {
                componentLibraries.addAll(clientLibraryCategoryMap.get(curComponentDependency));
            }

            componentDependencyMap.put(curComponentMapKey, componentLibraries);
        }

    }

    protected Integer getClientLibraryCount() {
        return clientLibraries.size();
    }

    protected Set<ClientLibrary> getLibrariesUnderPath(String path) {

        Set<ClientLibrary> librariesUnderPath = Sets.newHashSet();

        for (String curPath : clientLibraryPathMap.keySet()) {
            ClientLibrary curClientLibrary = clientLibraryPathMap.get(curPath);

            if (curPath.startsWith(path)) {
                librariesUnderPath.add(curClientLibrary);
            }
        }

        return librariesUnderPath;

    }

    protected boolean isClientLibraryFolderPath(String path) {
        return clientLibraryPathMap.containsKey(path);
    }

    /**
     *
     * @param path
     * @return true if the path specified identifies a component type, false otherwise.
     */
    protected boolean isComponentPath(String path) {

        return componentMap.containsKey(path);

    }

    /**
     *
     * @param path
     * @return True if the file is a Library Include file.  Library Include files are css.txt or js.txt files
     *         in the context of a client library
     */
    protected boolean isLibraryIncludeFilePath(String path) {

        for (ClientLibrary curLibrary : clientLibraries) {
            if (curLibrary.getLibraryIncludeFilePaths().contains(path)) {
                return true;
            }
        }

        return false;

    }

    /**
     *
     * @param path
     * @return True if the file is a Library Resource file.  A library resource file is a .css, .js, or .less asset
     *         which is included in the client library
     */
    protected boolean isLibraryResourceFilePath(String path) {

        for (ClientLibrary curLibrary : clientLibraries) {
            if (curLibrary.getClientLibraryResourcePaths().contains(path)) {
                return true;
            }
        }

        return false;

    }

    protected String getQualifiedPathToOverridableComponentDefinition(String resourceType, String[] searchPaths) {

        if (resourceType.startsWith("/")) {
            return resourceType;
        }
        for (String curSearchPath : searchPaths) {
            if (componentMap.containsKey(curSearchPath + resourceType)) {
                return curSearchPath + resourceType;
            }
        }

        return null;

    }

    /**
     * <p>
     * Given a resource, perform the following algorithm:
     * </p>
     *
     * <ul>
     *     <li>Construct a Set of all sling:resourceTypes represented by the resource and any children of the resource</li>
     *     <li>Qualify the resource type paths - each resource type path is qualified by iterating through the ResourceResolver's
     *         search paths, prepending each to the relative resource type path, stopping as soon as a concrete resource definition is found.</li>
     *     <li>Determine the set of qualified component paths for which library dependencies are known and use these as starting points for a graph search.</li>
     *     <li>Starting from these starting points, build a dependency graph.
     *         Embedded libraries are added as though the embedded library is dependent on the embedding library.</li>
     *     <li>Order the dependency graph.</li>
     * </ul>
     *
     * <p>
     * The results of the ordering are returned.
     * </p>
     *
     * @param root
     * @return An ordered list of Client Libraries build using the algorithm stipulated in the description
     * @throws javax.jcr.query.InvalidQueryException
     * @throws javax.jcr.RepositoryException
     */
    protected List<ClientLibrary> getOrderedDependencies(Resource root) throws InvalidQueryException, RepositoryException, InvalidClientLibraryCategoryException {

        Set<String> componentTypes = ComponentUtils.getNestedComponentTypes(root);

        Set<ClientLibrary> startingPoints = Sets.newHashSet();

        String[] searchPaths = root.getResourceResolver().getSearchPath();

        for (String curComponentType : componentTypes) {
            String fullyQualifiedComponentType = getQualifiedPathToOverridableComponentDefinition(curComponentType, searchPaths);

            if (StringUtils.isNotEmpty(fullyQualifiedComponentType)) {
                if (componentDependencyMap.containsKey(fullyQualifiedComponentType)) {
                    LOG.debug("Found component type " + fullyQualifiedComponentType + " with dependencies " + componentDependencyMap.get(fullyQualifiedComponentType));
                    startingPoints.addAll(componentDependencyMap.get(fullyQualifiedComponentType));
                }
            }
        }

        DirectedAcyclicGraph<ClientLibrary> dependencyGraph = new DirectedAcyclicGraph<ClientLibrary>();

        List<ClientLibrary> startingPointList = Lists.newArrayList(startingPoints);
        Set<ClientLibrary> visitedLibraries = Sets.newHashSet();

        while (!startingPointList.isEmpty()) {
            ClientLibrary curClientLibrary = startingPointList.remove(startingPointList.size() - 1);

            if (!visitedLibraries.contains(curClientLibrary)) {
                visitedLibraries.add(curClientLibrary)

                if (!dependencyGraph.contains(curClientLibrary)) {

                    LOG.debug( "Adding " + curClientLibrary + " to the dependency graph" );

                    dependencyGraph.addNode(curClientLibrary);

                }

                /*
                 * Add all embedded libraries as dependents of the current library
                 *
                 * embedded library - depends on -> client library
                 */
                for (String curEmbeddedLibraryCategory : curClientLibrary.getEmbeddedCategories()) {
                    if (!clientLibraryCategoryMap.containsKey(curEmbeddedLibraryCategory)) {
                        throw new InvalidClientLibraryCategoryException("Embedded library category " + curEmbeddedLibraryCategory + " was not found in the list of known libraries");
                    }
                    for (ClientLibrary curEmbeddedLibrary : clientLibraryCategoryMap.get(curEmbeddedLibraryCategory)) {
                        LOG.debug( "While processing embeds : found edge " + curEmbeddedLibrary + " -> " + curClientLibrary );
                        /*
                         * If this embedded library is not in the graph that means it has not been
                         * individually considered yet.  If it is, then it was previously considered or is
                         * already in the starting point list.
                         */
                        if (!visitedLibraries.contains(curEmbeddedLibrary)) {
                            startingPointList.add(curEmbeddedLibrary);
                        }
                        dependencyGraph.addEdge(curEmbeddedLibrary, curClientLibrary);
                    }
                }

                /*
                 * Add all dependencies of the current library
                 */
                for (String curDependencyLibraryCategory : curClientLibrary.getDependencies()) {
                    if (StringUtils.isNotBlank(curDependencyLibraryCategory)) {
                        if (!clientLibraryCategoryMap.containsKey(curDependencyLibraryCategory)) {
                            throw new InvalidClientLibraryCategoryException("Dependency library category " + curDependencyLibraryCategory + " was not found in the list of known libraries");
                        }
                        for (ClientLibrary curDependencyLibrary : clientLibraryCategoryMap.get(curDependencyLibraryCategory)) {
                            LOG.debug( "While processing dependencies : found edge " + curClientLibrary + " -> " + curDependencyLibrary );
                            /*
                             * See comment under the embedded library loop
                             */
                            if (!visitedLibraries.contains(curDependencyLibrary)) {
                                startingPointList.add(curDependencyLibrary);
                            }
                            dependencyGraph.addEdge(curClientLibrary, curDependencyLibrary);
                        }
                    }
                    else {
                        LOG.warn("Empty or blank library dependency found in client library " + curClientLibrary.toString());
                    }
                }

            }


        }

        return dependencyGraph.order( true );

    }

    protected void decommission() {
        clientLibraries.clear();
        componentDependencyMap.clear();
        clientLibraryCategoryMap.clear();
        componentMap.clear();
        clientLibraryPathMap.clear();
    }

    /**
     *
     * Construct a list of DependentComponent objects
     *
     * TODO: Determine what to do about inherited components and reference components since they don't live in the content tree of the page itself
     *
     *
     * @param queryManager
     * @param resourceResolver
     * @throws javax.jcr.RepositoryException
     */
    private static List<DependentComponent> collectComponents(QueryManager queryManager, ResourceResolver resourceResolver) throws InvalidQueryException, RepositoryException {

        LOG.debug("Collecting component definitions");

        Stopwatch stopwatch = Stopwatch.createStarted()

        def retList = []
        def resourceTypeToComponentMap = [:]

        def componentsWithDependenciesQuery =
            queryManager.createQuery(COMPONENTS_WITH_DEPENDENCIES_QUERY, Query.JCR_SQL2)

        def result = componentsWithDependenciesQuery.execute()

        def resultIterator = result.getRows()

        while (resultIterator.hasNext()) {

            def curRow = resultIterator.nextRow()

            LOG.debug( "Found component with dependency at path " + curRow.path )

            def componentResource = resourceResolver.getResource( curRow.path )
            def componentValueMap = componentResource.adaptTo( ValueMap.class )
            def componentDependencies = componentValueMap.get( "dependencies", new String[0] )

            def categories = Sets.newHashSet()

            categories.addAll( componentDependencies )

            def newComponent = Components.forResourceAndDependencies( componentResource, categories )
            retList.add( newComponent )
            resourceTypeToComponentMap[ newComponent.resourceType ] = newComponent

        }

        LOG.debug( "Querying for Inherited Components without Dependencies" )

        def inheritedComponentsWithoutDependenciesQuery =
            queryManager.createQuery(INHERITING_COMPONENTS_WITHOUT_DEPENDENCIES_QUERY, Query.JCR_SQL2)

        def inheritedResults = inheritedComponentsWithoutDependenciesQuery.execute()

        def inheritedResultIterator = inheritedResults.getRows()

        def numAdded = 0
        def componentsToCheck = []

        LOG.debug( "Handling component inheritance" )
        /*
         * The first pass over the query result handles the concern of getting the result into the format of a
         * List of Resource objects while also adding any found components to the returnable DependentComponent List
         */
        while ( inheritedResultIterator.hasNext() ) {

            def curRow = inheritedResultIterator.nextRow()

            def componentResource = resourceResolver.getResource( curRow.getPath() )

            if ( resourceTypeToComponentMap.containsKey( componentResource.getResourceSuperType() ) ) {

                LOG.debug( "Component at path " + componentResource.path + " found to have super type " + componentResource.resourceSuperType + " which has dependencies" )
                def newComponent = Components.forResourceAndDependencies( componentResource, resourceTypeToComponentMap[ componentResource.resourceSuperType ].dependencies )
                retList.add( newComponent )
                resourceTypeToComponentMap[ newComponent.resourceType ] = newComponent
                numAdded++

            }
            else {

                componentsToCheck.add( componentResource )

            }
        }

        /*
         * As long as we have added at least one DependentComponent to the List of known DependentComponents in the prior
         * iteration we loop over the List of components Resources to check to see if more can be added as a result of the
         * prior iteration
         */
        while ( numAdded > 0 && !componentsToCheck.isEmpty() ) {

            numAdded = 0

            componentsToCheck.each( { Resource curComponentResource ->

                if ( resourceTypeToComponentMap.containsKey( curComponentResource.resourceSuperType ) ) {

                    LOG.debug( "Component at path " + curComponentResource.path + " found to have super type " + curComponentResource.resourceSuperType + " which has dependencies" )

                    def newComponent = Components.forResourceAndDependencies( curComponentResource, resourceTypeToComponentMap[ curComponentResource.resourceSuperType ].dependencies )
                    retList.add( newComponent )
                    resourceTypeToComponentMap[ curComponentResource.resourceType ] = newComponent
                    numAdded ++

                }

            } )

        }


        stopwatch.stop()
        LOG.debug("Component collection completed in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + "ms" )

        return retList

    }

    /**
     * Construct a list of known Client Libraries across the entire repository
     *
     * @param queryManager
     * @param resourceResolver
     * @return
     * @throws InvalidQueryException
     * @throws RepositoryException
     */
    private static List<ClientLibrary> collectClientLibraries(QueryManager queryManager, ResourceResolver resourceResolver) throws InvalidQueryException, RepositoryException {

        LOG.debug("Collecting Client Library definitions");

        Stopwatch stopwatch = Stopwatch.createStarted()

        def retList = []

        def clientLibraryQuery =
            queryManager.createQuery(CLIENT_LIBRARY_QUERY, Query.JCR_SQL2)

        def result = clientLibraryQuery.execute()

        def resultIterator = result.getRows()

        while (resultIterator.hasNext()) {

            def curRow = resultIterator.nextRow()

            def clientLibraryResource = resourceResolver.getResource(curRow.getPath())
            def clientLibraryValueMap = clientLibraryResource.adaptTo(ValueMap.class)
            def clientLibraryCategories = clientLibraryValueMap.get("categories", new String[0])
            def clientLibraryEmbeds = clientLibraryValueMap.get("embed", new String[0])
            def clientLibraryDependencies = clientLibraryValueMap.get("dependencies", new String[0])

            LOG.debug( "Constructing Client Library for Client Library Resource " + clientLibraryResource.path )

            def curClientLibrary = ClientLibraries.newDefaultClientLibrary(
                    Sets.newHashSet( Arrays.asList( clientLibraryCategories ) ),
                    clientLibraryResource,
                    Arrays.asList(clientLibraryEmbeds),
                    Arrays.asList( clientLibraryDependencies ) )

            retList.add(curClientLibrary)

        }

        stopwatch.stop()

        LOG.debug( "Client Library collection completed in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + "ms" )

        return retList

    }
}
