package com.citytechinc.cq.clientlibs.api.structures.graph;

import java.util.List;

public interface DependencyGraph<T> {

    public void addNode(T node);

    public void addEdge(T fromNode, T toNode);

    public Boolean contains(T node);

    public Integer getNodeCount();

    public Integer getEdgeCount();

    public List<Edge<T>> getOutgoingEdgesFromNode(T node);

    public List<T> getOrdering();

    public List<T> getReverseOrdering();

}
