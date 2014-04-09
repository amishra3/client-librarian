package com.citytechinc.cq.clientlibs.core.listeners.components.impl;

import com.citytechinc.cq.clientlibs.api.domain.component.DependentComponent;
import com.citytechinc.cq.clientlibs.api.events.components.DependentComponentEvent;
import com.citytechinc.cq.clientlibs.api.events.components.factory.DependentComponentEventFactory;
import com.citytechinc.cq.clientlibs.api.services.components.DependentComponentManager;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import java.util.List;
import java.util.Set;

public class ClientLibraryComponentListener implements EventListener {

    private static final Logger LOG = LoggerFactory.getLogger(ClientLibraryComponentListener.class);

    private final DependentComponentEventFactory dependentComponentEventFactory;
    private final DependentComponentManager dependentComponentManager;
    private final Session session;

    public ClientLibraryComponentListener(DependentComponentEventFactory dependentComponentEventFactory, DependentComponentManager dependentComponentManager, Session session) {
        this.dependentComponentEventFactory = dependentComponentEventFactory;
        this.dependentComponentManager = dependentComponentManager;
        this.session = session;
    }

    @Override
    public void onEvent(EventIterator events) {

        Stopwatch stopwatch = Stopwatch.createStarted();

        List<DependentComponentEvent> dependentComponentEventList = Lists.newArrayList();

        Set<DependentComponent> dependentComponents = dependentComponentManager.getComponents();

        while (events.hasNext()) {
            Event currentEvent = events.nextEvent();

            try {
                Optional<DependentComponentEvent> eventOptional = dependentComponentEventFactory.make(currentEvent, dependentComponentManager.getComponentsByPath(), session);

                if (eventOptional.isPresent()) {
                    dependentComponentEventList.add(eventOptional.get());
                }
            } catch (RepositoryException e) {
                LOG.error("Repository Exception encountered while processing event", e);
            }

        }

        if (dependentComponentEventList.size() > 0) {
            dependentComponentManager.requestRefresh();
        }

    }

}
