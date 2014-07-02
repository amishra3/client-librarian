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
package com.citytechinc.cq.clientlibs.core.listeners.components.factory.impl;

import com.citytechinc.cq.clientlibs.api.constants.Properties;
import com.citytechinc.cq.clientlibs.api.domain.component.DependentComponent;
import com.citytechinc.cq.clientlibs.api.events.components.DependentComponentEvent;
import com.citytechinc.cq.clientlibs.api.events.components.factory.DependentComponentEventFactory;
import com.citytechinc.cq.clientlibs.core.events.components.impl.*;
import com.google.common.base.Optional;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import java.util.Map;

public class DefaultDependentComponentEventFactory implements DependentComponentEventFactory {

    @Override
    public Optional<DependentComponentEvent> make(Event event, Map<String, DependentComponent> dependentComponentByPathMap, Session session) throws RepositoryException {

        switch (event.getType()) {
            case Event.NODE_ADDED:
                return Optional.fromNullable(makeForNodeAddedEvent(event, session.getNode(event.getPath())));
            case Event.NODE_MOVED:
                return Optional.fromNullable(makeForNodeMovedEvent(event, session.getNode(event.getPath())));
            case Event.NODE_REMOVED:
                return Optional.fromNullable(makeForNodeRemovedEvent(event, dependentComponentByPathMap));
            case Event.PROPERTY_ADDED:
                return Optional.fromNullable(makeForPropertyAddedEvent(event, session.getProperty(event.getPath())));
            case Event.PROPERTY_CHANGED:
                return Optional.fromNullable(makeForPropertyChangedEvent(event, session.getProperty(event.getPath())));
            case Event.PROPERTY_REMOVED:
                return Optional.fromNullable(makeForPropertyRemovedEvent(event, dependentComponentByPathMap));
            case Event.PERSIST:
                return Optional.of(makeForPersistEvent());
            default:
                return Optional.absent();
        }

    }

    protected DependentComponentEvent makeForNodeAddedEvent(Event event, Node eventNode) throws RepositoryException {

        if (isNodeADependentComponent(eventNode)) {
            return new NewDependentComponentEvent();
        }

        return null;

    }

    /**
     * For now - this method is a placeholder which always returns null.  A Property Add event for the "dependencies"
     * property is issued along with the Node Add event for the component node, so we can catch the Component Addition
     * in the Property Add event.  Otherwise we would catch the addition twice.
     *
     * @param event
     * @param eventNode
     * @return
     * @throws RepositoryException
     */
    protected DependentComponentEvent makeForNodeMovedEvent(Event event, Node eventNode) throws RepositoryException {

        return null;

    }

    protected DependentComponentEvent makeForNodeRemovedEvent(Event event, Map<String, DependentComponent> dependentComponentByPathMap) throws RepositoryException {

        if (dependentComponentByPathMap.containsKey(event.getPath())) {
            return new RemovedDependentComponentEvent();
        }

        return null;

    }

    protected DependentComponentEvent makeForPropertyAddedEvent(Event event, Property property) throws RepositoryException {

        if (Properties.CLIENT_LIBRARY_DEPENDENCIES.equals(property.getName())) {
            Node parentNode = property.getParent();
            if (isNodeADependentComponent(parentNode)) {
                return new NewDependentComponentEvent();
            }
        }

        return null;

    }

    protected DependentComponentEvent makeForPropertyChangedEvent(Event event, Property property) throws RepositoryException {

        if (Properties.CLIENT_LIBRARY_DEPENDENCIES.equals(property.getName())) {
            Node parentNode = property.getParent();
            if (isNodeADependentComponent(parentNode)) {
                return new ModifiedDependentComponentEvent();
            }
        }

        return null;

    }

    protected DependentComponentEvent makeForPropertyRemovedEvent(Event event, Map<String, DependentComponent> dependentComponentByPathMap) throws RepositoryException {
        String propertyName = event.getPath().substring(event.getPath().lastIndexOf("/") + 1);

        if (Properties.CLIENT_LIBRARY_DEPENDENCIES.equals(propertyName)) {
            String nodePath = event.getPath().substring(0, event.getPath().lastIndexOf("/"));
            if (dependentComponentByPathMap.containsKey(nodePath)) {
                return new RemovedDependentComponentEvent();
            }
        }

        return null;

    }

    protected DependentComponentEvent makeForPersistEvent() {
        return new PersistEvent();
    }


    protected Boolean isNodeADependentComponent(Node node) throws RepositoryException {
        return node.isNodeType("cq:Component") && node.hasProperty(Properties.CLIENT_LIBRARY_DEPENDENCIES);
    }

}
