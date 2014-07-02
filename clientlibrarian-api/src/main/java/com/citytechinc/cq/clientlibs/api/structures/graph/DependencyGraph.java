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
package com.citytechinc.cq.clientlibs.api.structures.graph;

import java.util.List;
import java.util.Set;

public interface DependencyGraph<T> {

    public void addNode(T node);

    public void addEdge(T fromNode, T toNode);

    public Boolean contains(T node);

    public Integer getNodeCount();

    public Integer getEdgeCount();

    public List<Edge<T>> getOutgoingEdgesFromNode(T node);

    public List<T> getOrdering();

    public List<T> getReverseOrdering();

    public Set<T> getNodes();

    public Set<Edge<T>> getEdges();

}
