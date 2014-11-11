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
package com.citytechinc.cq.clientlibs.core.listeners.library.impl;

import com.citytechinc.cq.clientlibs.api.domain.library.ClientLibrary;
import com.citytechinc.cq.clientlibs.api.events.library.ClientLibraryEvent;
import com.citytechinc.cq.clientlibs.api.events.library.factory.ClientLibraryEventFactory;
import com.citytechinc.cq.clientlibs.api.services.clientlibs.ClientLibraryManager;
import com.citytechinc.cq.clientlibs.api.services.clientlibs.cache.ClientLibraryCacheManager;
import com.citytechinc.cq.clientlibs.api.services.clientlibs.exceptions.ClientLibraryCachingException;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClientLibraryEventListener implements EventListener {

    private static final Logger LOG = LoggerFactory.getLogger(ClientLibraryEventListener.class);

    private final ClientLibraryEventFactory clientLibraryEventFactory;
    private final ClientLibraryManager clientLibraryManager;
    private final ClientLibraryCacheManager clientLibraryCacheManager;
    private final Session session;

    public ClientLibraryEventListener(ClientLibraryEventFactory clientLibraryEventFactory, ClientLibraryManager clientLibraryManager, ClientLibraryCacheManager clientLibraryCacheManager, Session session) {
        this.clientLibraryEventFactory = clientLibraryEventFactory;
        this.clientLibraryManager = clientLibraryManager;
        this.clientLibraryCacheManager = clientLibraryCacheManager;
        this.session = session;
    }

    @Override
    public void onEvent(EventIterator events) {

        List<ClientLibraryEvent> clientLibraryEventList = Lists.newArrayList();

        Set<ClientLibrary> clientLibrarySetSnapshot = clientLibraryManager.getLibraries();
        Map<String, ClientLibrary> clientLibraryByPathMap = Maps.newHashMap();

        for (ClientLibrary currentClientLibrary : clientLibrarySetSnapshot) {
            clientLibraryByPathMap.put(currentClientLibrary.getClientLibraryPath(), currentClientLibrary);
        }

        while (events.hasNext()) {
            Event currentEvent = events.nextEvent();

            try {

                Optional<ClientLibraryEvent> clientLibraryEventOptional =
                        clientLibraryEventFactory.make(currentEvent, clientLibraryByPathMap, session);

                if (clientLibraryEventOptional.isPresent()) {
                    clientLibraryEventList.add(clientLibraryEventOptional.get());
                }

            } catch (PathNotFoundException e) {
                LOG.debug("Path Not Found Exception encountered while processing potential Client Library events", e);
            }
            catch (RepositoryException e) {
                LOG.error("Repository Exception encountered while processing potential Client Library events", e);
            }


        }

        if (clientLibraryEventList.size() > 0) {
            try {
                clientLibraryCacheManager.clearCache();
            } catch (ClientLibraryCachingException e) {
                LOG.error("Exception encountered clearing client library cache", e);
            }
            clientLibraryManager.requestRefresh();
        }

    }

}
