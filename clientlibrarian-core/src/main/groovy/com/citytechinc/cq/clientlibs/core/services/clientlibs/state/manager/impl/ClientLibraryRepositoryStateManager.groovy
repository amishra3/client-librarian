package com.citytechinc.cq.clientlibs.core.services.clientlibs.state.manager.impl

import com.citytechinc.cq.clientlibs.api.domain.library.ClientLibrary
import com.citytechinc.cq.clientlibs.api.domain.library.exceptions.InvalidClientLibraryCategoryException
import com.citytechinc.cq.clientlibs.api.services.clientlibs.ClientLibraryManager
import com.citytechinc.cq.clientlibs.api.services.clientlibs.ResourceDependencyProvider
import com.citytechinc.cq.clientlibs.api.services.clientlibs.state.ClientLibraryStateStatistics
import com.citytechinc.cq.clientlibs.api.structures.graph.DependencyGraph
import com.citytechinc.cq.clientlibs.core.services.clientlibs.state.builder.ClientLibraryStateStatisticsBuilder
import com.citytechinc.cq.clientlibs.api.services.components.DependentComponentManager
import com.citytechinc.cq.clientlibs.core.structures.graph.dag.DirectedAcyclicGraph
import com.google.common.collect.Lists
import com.google.common.collect.Sets
import org.apache.commons.lang.StringUtils
import org.apache.sling.api.resource.Resource
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ClientLibraryRepositoryStateManager {

    private static final Logger LOG = LoggerFactory.getLogger( ClientLibraryRepositoryStateManager )

    private DependentComponentManager dependentComponentManager
    private ClientLibraryManager clientLibraryManager

    public ClientLibraryRepositoryStateManager(ClientLibraryManager clientLibraryManager, DependentComponentManager dependentComponentManager) {
        this.dependentComponentManager = dependentComponentManager
        this.clientLibraryManager = clientLibraryManager
    }

    void requestRefresh() {
        LOG.debug("Received Refresh Request")

        synchronized (this) {
            dependentComponentManager.requestRefresh()
            clientLibraryManager.requestRefresh()
        }
    }

    public DependencyGraph<ClientLibrary> requestDependencyGraph(Resource r, List<ResourceDependencyProvider> resourceDependencyProviderList) {

        LOG.debug("Received Dependency Graph Request")

        synchronized (this) {

            return getDependencyGraph(r, resourceDependencyProviderList);

        }

    }

    public List<ClientLibrary> requestOrderedDependencies(Resource r, List<ResourceDependencyProvider> resourceDependencyProviderList) {
        LOG.debug("Received Ordered Dependencies Request")

        synchronized (this) {

            return getOrderedDependencies(r, resourceDependencyProviderList)

        }
    }

    public ClientLibraryStateStatistics requestStateStatistics() {
        LOG.debug("Received State Statistics Request")

        synchronized (this) {

            def statisticsBuilder = ClientLibraryStateStatisticsBuilder.cleanBuilder

            statisticsBuilder.setClientLibraryCount(clientLibraryManager.getClientLibraryCount())

            return statisticsBuilder.build()

        }

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
     * </ul>
     *
     * <p>
     * The resulting dependecy graph is returned.
     * </p>
     *
     * @param root  Page to begin search for dependencies.
     * @return A dependency graph of Client Libraries built using the algorithm stipulated in the description
     */
    protected DirectedAcyclicGraph<ClientLibrary> getDependencyGraph(Resource root, List<ResourceDependencyProvider> resourceDependencyProviderList) throws InvalidClientLibraryCategoryException {

        Set<ClientLibrary> startingPoints = Sets.newHashSet()

        //Iterate through all known dependency provider services adding the dependencies provided by each for the Resource in question
        resourceDependencyProviderList.each { ResourceDependencyProvider currentResourceDependencyProvider ->
            startingPoints.addAll(currentResourceDependencyProvider.getDependenciesForResource(root))
        }

        DirectedAcyclicGraph<ClientLibrary> dependencyGraph = new DirectedAcyclicGraph<ClientLibrary>()

        Map<String, Set<ClientLibrary>> clientLibrariesByCategoryMap = clientLibraryManager.getLibrariesByCategory()
        List<ClientLibrary> startingPointList = Lists.newArrayList(startingPoints);
        //TODO: See if we can clean up this visitor implementation
        Set<ClientLibrary> visitedLibraries = Sets.newHashSet()

        Set<String> containedCategories = Sets.newHashSet()
        Set<ClientLibrary> librariesWithConditionalDependencies = Sets.newHashSet()

        while (!startingPointList.isEmpty()) {

            ClientLibrary curClientLibrary = startingPointList.remove(startingPointList.size() - 1)

            //Only process the Client Library if it has not yet been visited.
            if (!visitedLibraries.contains(curClientLibrary)) {
                visitedLibraries.add(curClientLibrary)

                //If this library has Conditional Dependencies add it to the set to be processed later
                if (!curClientLibrary.conditionalDependencies.empty) {
                    librariesWithConditionalDependencies.add(curClientLibrary)
                }

                if (!dependencyGraph.contains(curClientLibrary)) {

                    LOG.debug( "Adding " + curClientLibrary + " to the dependency graph" );

                    dependencyGraph.addNode(curClientLibrary);

                }

                /*
                 * Add all embedded libraries as dependents of the current library
                 *
                 * embedded library - depends on -> client library
                 */
                for (String currentEmbeddedLibraryCategory : curClientLibrary.getEmbeddedCategories()) {

                    if (!clientLibrariesByCategoryMap.containsKey(currentEmbeddedLibraryCategory)) {
                        throw new InvalidClientLibraryCategoryException("Client Library " + curClientLibrary.clientLibraryPath + " embeds category " + currentEmbeddedLibraryCategory + " however no Client Library answers to that name")
                    }

                    Set<ClientLibrary> currentEmbeddedLibraries = clientLibrariesByCategoryMap.get(currentEmbeddedLibraryCategory)

                    for (ClientLibrary currentEmbeddedLibrary : currentEmbeddedLibraries) {
                        LOG.debug( "While processing embeds : found edge " + currentEmbeddedLibrary.clientLibraryPath + " -> " + curClientLibrary.clientLibraryPath );
                        /*
                         * If this embedded library is not in the set of visited libraries that means it has not been
                         * individually considered yet.  If it is, then it was previously considered or is
                         * already in the starting point list.
                         */
                        if (!visitedLibraries.contains(currentEmbeddedLibrary)) {
                            startingPointList.add(currentEmbeddedLibrary);
                        }
                        dependencyGraph.addEdge(currentEmbeddedLibrary, curClientLibrary);
                    }
                }

                /*
                 * Add all dependencies of the current library
                 */
                for (String curDependencyLibraryCategory : curClientLibrary.getDependencies()) {
                    if (StringUtils.isNotBlank(curDependencyLibraryCategory)) {
                        if (!clientLibrariesByCategoryMap.containsKey(curDependencyLibraryCategory)) {
                            throw new InvalidClientLibraryCategoryException("Client Library " + curClientLibrary.clientLibraryPath + " depends on category " + curDependencyLibraryCategory + " however no Client Library answers to that name")
                        }
                        for (ClientLibrary curDependencyLibrary : clientLibrariesByCategoryMap.get(curDependencyLibraryCategory)) {
                            LOG.debug( "While processing dependencies : found edge " + curClientLibrary.clientLibraryPath + " -> " + curDependencyLibrary.clientLibraryPath );
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

        /*
        * Handling of Conditional Dependencies
        *
        * Iterate through the set of ClientLibraries which appear in the graph and have conditional dependencies.
        * For each, iterate over the categories which they indicate to be conditional dependencies.  For each category,
        * iterate over the libraries in the category looking to see whether each is already in the graph.  For each
        * such library add an edge between the library with the conditional dependency and the library which is that
        * dependency.
        */
        librariesWithConditionalDependencies.each( { ClientLibrary currentLibraryWithConditionalDependency ->

            currentLibraryWithConditionalDependency.conditionalDependencies.each( { String currentConditionalDependencyCategory ->

                clientLibrariesByCategoryMap.get(currentConditionalDependencyCategory).each( { ClientLibrary currentLibraryInConditionalDependency ->

                    if (dependencyGraph.contains(currentLibraryInConditionalDependency)) {
                        dependencyGraph.addEdge(currentLibraryWithConditionalDependency, currentLibraryInConditionalDependency)
                    }

                } )

            } )

        } )

        return dependencyGraph;

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
     */
    protected List<ClientLibrary> getOrderedDependencies(Resource root, List<ResourceDependencyProvider> resourceDependencyProviderList) throws InvalidClientLibraryCategoryException {

        return getDependencyGraph(root, resourceDependencyProviderList).order( true );

    }

}
