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