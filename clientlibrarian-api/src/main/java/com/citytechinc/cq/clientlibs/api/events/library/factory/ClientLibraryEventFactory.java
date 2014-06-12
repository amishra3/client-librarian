package com.citytechinc.cq.clientlibs.api.events.library.factory;

import com.citytechinc.cq.clientlibs.api.domain.library.ClientLibrary;
import com.citytechinc.cq.clientlibs.api.events.library.ClientLibraryEvent;
import com.google.common.base.Optional;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import java.util.Map;

public interface ClientLibraryEventFactory {

    public Optional<ClientLibraryEvent> make(Event event, Map<String, ClientLibrary> clientLibrariesByPathMap, Session session) throws RepositoryException;

}
