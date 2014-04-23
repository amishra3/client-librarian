package com.citytechinc.cq.clientlibs.core.structures.graph.dag

import com.citytechinc.cq.clientlibs.api.structures.graph.Edge

public class SimpleEdge<T> implements Edge<T> {

    private final T fromNode
    private final T toNode

    public SimpleEdge(T fromNode, T toNode) {
        this.fromNode = fromNode
        this.toNode = toNode
    }

    @Override
    public T getFrom() {
        fromNode
    }

    @Override
    public T getTo() {
        toNode
    }

    @Override
    public boolean equals(Object edge) {
        if (edge.class.isInstance(Edge.class)) {
            Edge<T> castEdge = (Edge) edge
            return castEdge.from.equals(from) && castEdge.to.equals(to)
        }

        return false
    }

}