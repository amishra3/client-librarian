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
package com.citytechinc.cq.clientlibs.core.structures.graph.dag

import com.citytechinc.cq.clientlibs.api.domain.library.ClientLibrary
import com.citytechinc.cq.clientlibs.api.structures.graph.DependencyGraph
import com.citytechinc.cq.clientlibs.api.structures.graph.Edge
import com.citytechinc.cq.clientlibs.api.structures.graph.exceptions.InvalidGraphException

/**
 * http://en.wikipedia.org/wiki/Dependency_graph
 *
 * @param <T>
 */
class DirectedAcyclicGraph<T> implements DependencyGraph<T> {

    private Set<T> nodes
    private Set edges
    private Map nodeEdges = [:]

    public DirectedAcyclicGraph() {
        nodes = new HashSet<T>()
        edges = new HashSet<T>()
    }

    public Boolean contains(T n) {
        return nodes.contains(n)
    }

    public void addNode(T n) {
        if (!nodes.contains(n)) {
            nodes.add(n)
        }
    }

    /**
     * Adds an edge from the first parameter to the second parameter.
     * In the example of a dependency graph this should be read to mean
     * 'add a dependency such that from depends on to'
     *
     * @param from
     * @param to
     */
    public void addEdge(T from, T to) {
        def newEdge = new SimpleEdge<T>(from, to);
        if (!edges.contains(newEdge)) {
            edges.add(newEdge)
            if ( !nodeEdges.containsKey(from) ) {
                nodeEdges.put( from, [] )
            }
            nodeEdges.get( from ).add( newEdge )
        }
        if (!nodes.contains(from)) {
            nodes.add(from)
        }
        if (!nodes.contains(to)) {
            nodes.add(to)
        }
    }

    public Integer getNodeCount() {
        return nodes.size()
    }

    public Integer getEdgeCount() {
        return edges.size()
    }

    public List<Edge> getOutgoingEdgesFromNode(T node) {
        if ( nodeEdges.get( node ) ) {
            nodeEdges.get( node )


            return nodeEdges.get( node )
        }

        return []
    }

    public List<T> getOrdering() {
        return order( false )
    }

    public List<T> getReverseOrdering() {
        return order( true )
    }

    @Override
    public Set<T> getNodes() {
        return nodes;
    }

    @Override
    public Set<Edge<T>> getEdges() {
        return edges;
    }

    /**
     * A DFS implementation of a Topological sort on the Graph.
     *
     * http://en.wikipedia.org/wiki/Topological_sorting#Algorithms
     *
     * @return An ordered list of nodes in the graph
     */
    private List<T> order(Boolean reversed = false) {

        List<T> retList = new ArrayList<T>()

        /*
         * Here we sort the set of Nodes prior to transforming them into visitable nodes.  This ensures that
         * the ordering of unrelated nodes in the returned list of nodes is deterministic.  Previously, if two
         * nodes had no direct or indirect edge between them, their ordering in the returned list of nodes
         * was random.  Sorting the nodes prior to visiting them ensures unrelated nodes are visited in a
         * consistent order and thus appear in a consistent order in the produced list.
         */
        Map<T, VisitableNode> visitableNodes = nodes.sort().collectEntries { [ ( it ) : new VisitableNode( payload:it ) ] }

        def visit
        visit = {

            //If we find a node which we're currently visiting then we hit a cycle
            if ( it.visiting ) {
                throw new InvalidGraphException( "Graph is not acyclic" )
            }

            //If we already visited the node, ignore it
            if ( it.visited ) {
                return
            }

            //Visit the node
            def payload = it.visit()

            //Visit all of the nodes outgoing edges
            getOutgoingEdgesFromNode( payload ).each { visit( visitableNodes.get( it[ "to" ] ) ) }

            //Add the node to the ordered list of nodes
            retList.push( payload )

            //Leave the node we were visiting
            it.leave()

        }

        visitableNodes.values().each visit

        /*
         * The DFS algorithm ends up producing a reversed sort so to provide
         * a true graph ordering the list needs to be reversed.
         */
        if (reversed) {
            return retList
        }

        return retList.reverse()

    }
}
