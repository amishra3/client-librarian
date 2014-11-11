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
package com.citytechinc.cq.clientlibs.core.listeners.components.impl;

import com.citytechinc.cq.clientlibs.api.domain.component.DependentComponent;
import com.citytechinc.cq.clientlibs.api.events.components.DependentComponentEvent;
import com.citytechinc.cq.clientlibs.api.events.components.factory.DependentComponentEventFactory;
import com.citytechinc.cq.clientlibs.api.services.clientlibs.cache.ClientLibraryCacheManager;
import com.citytechinc.cq.clientlibs.api.services.clientlibs.exceptions.ClientLibraryCachingException;
import com.citytechinc.cq.clientlibs.api.services.components.DependentComponentManager;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class DependentComponentEventListener implements EventListener {

    private static final Logger LOG = LoggerFactory.getLogger(DependentComponentEventListener.class);

    private final DependentComponentEventFactory dependentComponentEventFactory;
    private final DependentComponentManager dependentComponentManager;
    private final ClientLibraryCacheManager clientLibraryCacheManager;
    private final Session session;

    public DependentComponentEventListener(DependentComponentEventFactory dependentComponentEventFactory, DependentComponentManager dependentComponentManager, ClientLibraryCacheManager clientLibraryCacheManager, Session session) {
        this.dependentComponentEventFactory = dependentComponentEventFactory;
        this.dependentComponentManager = dependentComponentManager;
        this.clientLibraryCacheManager = clientLibraryCacheManager;
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
            } catch(PathNotFoundException e ) {
                LOG.debug("Path Not Found Exception encountered while processing event", e);
            } catch(RepositoryException e) {
                LOG.error("Repository Exception encountered while processing event", e);
            }

        }

        if (dependentComponentEventList.size() > 0) {
            try {
                clientLibraryCacheManager.clearCache();
            } catch (ClientLibraryCachingException e) {
                LOG.error("Exception encountered attempting to clear the cache", e);
            }
            dependentComponentManager.requestRefresh();
        }

        stopwatch.stop();
        LOG.debug("Client Library event handling completed in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + "ms. Resulted in " + dependentComponentEventList.size() + " events");

    }

}
