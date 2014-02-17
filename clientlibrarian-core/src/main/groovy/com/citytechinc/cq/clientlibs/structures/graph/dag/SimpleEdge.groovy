package com.citytechinc.cq.clientlibs.structures.graph.dag

import com.citytechinc.cq.clientlibs.structures.graph.Edge

public class SimpleEdge<T> implements Edge<T> {

    private final T fromNode
    private final T toNode

    public SimpleEdge(T fromNode, T toNode) {
        fromNode = fromNode
        toNode = toNode
    }

    @Override
    T getFrom() {
        fromNode
    }

    @Override
    T getTo() {
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