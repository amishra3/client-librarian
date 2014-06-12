package com.citytechinc.cq.clientlibs.api.events.components.factory;

import com.citytechinc.cq.clientlibs.api.domain.component.DependentComponent;
import com.citytechinc.cq.clientlibs.api.events.components.DependentComponentEvent;
import com.google.common.base.Optional;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import java.util.Map;

public interface DependentComponentEventFactory {

    public Optional<DependentComponentEvent> make(Event event, Map<String, DependentComponent> dependentComponentByPathMap, Session session) throws RepositoryException;

}

