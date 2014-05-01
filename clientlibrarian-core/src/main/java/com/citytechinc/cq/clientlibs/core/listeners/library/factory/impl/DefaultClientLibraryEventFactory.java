package com.citytechinc.cq.clientlibs.core.listeners.library.factory.impl;

import com.citytechinc.cq.clientlibs.api.constants.Properties;
import com.citytechinc.cq.clientlibs.api.constants.Types;
import com.citytechinc.cq.clientlibs.api.domain.library.ClientLibrary;
import com.citytechinc.cq.clientlibs.api.events.library.ClientLibraryEvent;
import com.citytechinc.cq.clientlibs.api.events.library.factory.ClientLibraryEventFactory;
import com.citytechinc.cq.clientlibs.core.events.library.impl.*;
import com.google.common.base.Optional;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import java.util.Map;

public class DefaultClientLibraryEventFactory implements ClientLibraryEventFactory {


    @Override
    public Optional<ClientLibraryEvent> make(Event event, Map<String, ClientLibrary> clientLibrariesByPathMap, Session session) throws RepositoryException {

        switch (event.getType()) {
            case Event.NODE_ADDED:
                return Optional.fromNullable(makeForNodeAddedEvent(event, session.getNode(event.getPath())));
            case Event.NODE_MOVED:
                return Optional.fromNullable(makeForNodeMovedEvent(event, session.getNode(event.getPath())));
            case Event.NODE_REMOVED:
                return Optional.fromNullable(makeForNodeRemovedEvent(event, clientLibrariesByPathMap));
            case Event.PROPERTY_ADDED:
                return Optional.fromNullable(makeForPropertyAddedEvent(event, session.getProperty(event.getPath())));
            case Event.PROPERTY_CHANGED:
                return Optional.fromNullable(makeForPropertyChangedEvent(event, session.getProperty(event.getPath())));
            case Event.PROPERTY_REMOVED:
                return Optional.fromNullable(makeForPropertyRemovedEvent(event, clientLibrariesByPathMap));
            case Event.PERSIST:
                return Optional.of(makeForPersistEvent());
            default:
                return Optional.absent();
        }

    }

    /**
     * Elicits events for the addition of css.txt and js.txt files.  Currently we ignore the addition of client library
     * nodes themselves as these additions should be picked up in our tracking of the addition of the categories
     * property of the library node.
     *
     * @param event
     * @param node
     * @return
     * @throws RepositoryException
     */
    private ClientLibraryEvent makeForNodeAddedEvent(Event event, Node node) throws RepositoryException {

        if (isNodeACssLibraryInclusionFile(node)) {
            return new CssLibraryAdditionEvent();
        }

        if (isNodeAJsLibraryInclusionFile(node)) {
            return new JsLibraryAdditionEvent();
        }

        return null;

    }

    //TODO: Implement
    private ClientLibraryEvent makeForNodeMovedEvent(Event event, Node node) {
        return null;
    }

    private ClientLibraryEvent makeForNodeRemovedEvent(Event event, Map<String, ClientLibrary> clientLibrariesByPathMap) throws RepositoryException {

        String eventPath = event.getPath();

        if (eventPath.endsWith("/" + ClientLibrary.CSS_FILE) || eventPath.endsWith("/" + ClientLibrary.JS_FILE)) {
            if (clientLibrariesByPathMap.containsKey(eventPath.substring(0, eventPath.lastIndexOf("/")))) {
                if (eventPath.endsWith("/" + ClientLibrary.CSS_FILE)) {
                    return new CssLibraryRemovalEvent();
                }
                else {
                    return new JsLibraryRemovalEvent();
                }
            }
        }

        if (clientLibrariesByPathMap.containsKey(eventPath)) {
            return new ClientLibraryRemovalEvent();
        }

        return null;

    }

    private ClientLibraryEvent makeForPropertyAddedEvent(Event event, Property property) throws RepositoryException {

        if (property == null) {
            return null;
        }

        Node propertyParent = property.getParent();

        if (isNodeClientLibraryFolder(propertyParent)) {

            if (Properties.CLIENT_LIBRARY_CATEGORIES.equals(property.getName())) {
                return new NewClientLibraryEvent();
            }

            if (Properties.CLIENT_LIBRARY_DEPENDENCIES.equals(property.getName())) {
                return new ClientLibraryDependencyModificationEvent();
            }

            if (Properties.CLIENT_LIBRARY_EMBED.equals(property.getName())) {
                return new ClientLibraryEmbedsModificationEvent();
            }

            if (Properties.CLIENT_LIBRARY_RUN_MODES.equals(property.getName())) {
                return new ClientLibraryRunModesModificationEvent();
            }

        }

        return null;

    }

    private ClientLibraryEvent makeForPropertyChangedEvent(Event event, Property property) throws RepositoryException {

        if (property == null) {
            return null;
        }

        Node propertyParent = property.getParent();

        /*
         * For Client Library property updates, check for category, dependencies, embed, and runmodes changes
         */
        if (isNodeClientLibraryFolder(propertyParent)) {

            if (Properties.CLIENT_LIBRARY_CATEGORIES.equals(property.getName())) {
                return new ClientLibraryCategoriesModificationEvent();
            }

            if (Properties.CLIENT_LIBRARY_DEPENDENCIES.equals(property.getName())) {
                return new ClientLibraryDependencyModificationEvent();
            }

            if (Properties.CLIENT_LIBRARY_EMBED.equals(property.getName())) {
                return new ClientLibraryEmbedsModificationEvent();
            }

            if (Properties.CLIENT_LIBRARY_RUN_MODES.equals(property.getName())) {
                return new ClientLibraryRunModesModificationEvent();
            }

        }

        /*
         * Check for updates to the content of css.txt and js.txt files
         */
        if (property.getName().equals("jcr:data")) {

            if (propertyParent.getName().equals("jcr:content")) {
                propertyParent = propertyParent.getParent();
            }

            if (ClientLibrary.CSS_FILE.equals(propertyParent.getName()) || ClientLibrary.JS_FILE.equals(propertyParent.getName())) {
                if (isNodeClientLibraryFolder(propertyParent.getParent())) {
                    if (ClientLibrary.CSS_FILE.equals(propertyParent.getName())) {
                        return new CssLibraryModificationEvent();
                    }
                    else {
                        return new JsLibraryModificationEvent();
                    }
                }
            }
        }

        return null;

    }

    private ClientLibraryEvent makeForPropertyRemovedEvent(Event event, Map<String, ClientLibrary> clientLibrariesByPathMap) throws RepositoryException {

        if (clientLibrariesByPathMap.containsKey(event.getPath().substring(0, event.getPath().lastIndexOf("/")))) {

            if (event.getPath().endsWith(Properties.CLIENT_LIBRARY_DEPENDENCIES)) {
                return new ClientLibraryDependencyModificationEvent();
            }

            if (event.getPath().endsWith(Properties.CLIENT_LIBRARY_EMBED)) {
                return new ClientLibraryEmbedsModificationEvent();
            }

            if (event.getPath().endsWith(Properties.CLIENT_LIBRARY_RUN_MODES)) {
                return new ClientLibraryRunModesModificationEvent();
            }

            if (event.getPath().endsWith(Properties.CLIENT_LIBRARY_CATEGORIES)) {
                return new ClientLibraryRemovalEvent();
            }

        }

        return null;

    }

    private ClientLibraryEvent makeForPersistEvent() {
        return new PersistEvent();
    }

    private Boolean isNodeClientLibraryFolder(Node node) throws RepositoryException {
        return node.isNodeType(Types.CQ_CLIENT_LIBRARY_FOLDER);
    }

    private Boolean isNodeACssLibraryInclusionFile(Node node) throws RepositoryException {

        if (node.isNodeType(Types.NT_FILE) && (node.getName().equals(ClientLibrary.CSS_FILE))) {
            return isNodeClientLibraryFolder(node.getParent());

        }

        return false;

    }

    private boolean isNodeAJsLibraryInclusionFile( Node node ) throws RepositoryException {

        if (node.isNodeType(Types.NT_FILE) && (node.getName().equals(ClientLibrary.JS_FILE))) {
            return isNodeClientLibraryFolder(node.getParent());

        }

        return false;

    }

}
