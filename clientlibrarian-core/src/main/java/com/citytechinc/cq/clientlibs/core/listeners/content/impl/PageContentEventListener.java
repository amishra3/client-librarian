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
package com.citytechinc.cq.clientlibs.core.listeners.content.impl;

import com.citytechinc.cq.clientlibs.api.services.clientlibs.cache.ClientLibraryCacheManager;
import com.citytechinc.cq.clientlibs.api.services.clientlibs.exceptions.ClientLibraryCachingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

/**
 * The default PageContentEventListener manages invalidation of the cached client libraries for a page
 * every time the page content changes.
 */
public class PageContentEventListener implements EventListener {

    private static final Logger LOG = LoggerFactory.getLogger(PageContentEventListener.class);

    private final ClientLibraryCacheManager clientLibraryCacheManager;

    public PageContentEventListener(ClientLibraryCacheManager clientLibraryCacheManager) {
        this.clientLibraryCacheManager = clientLibraryCacheManager;
    }

    @Override
    public void onEvent(EventIterator eventIterator) {
        while (eventIterator.hasNext()) {
            try {
                Event currentEvent = eventIterator.nextEvent();

                if (currentEvent.getType() == Event.PROPERTY_ADDED || currentEvent.getType() == Event.PROPERTY_CHANGED || currentEvent.getType() == Event.PROPERTY_REMOVED) {
                    clientLibraryCacheManager.invalidateCache(currentEvent.getPath().substring(0, currentEvent.getPath().lastIndexOf("/")));
                }
                else if (currentEvent.getType() == Event.NODE_REMOVED) {
                    clientLibraryCacheManager.invalidateCache(currentEvent.getPath());
                }
                else if (currentEvent.getType() == Event.NODE_MOVED) {
                    if(currentEvent.getInfo().containsKey("srcAbsPath")) {
                        clientLibraryCacheManager.invalidateCache((String) currentEvent.getInfo().get("srcAbsPath"));
                    }
                    clientLibraryCacheManager.invalidateCache(currentEvent.getPath());
                }
            } catch (RepositoryException e) {
                LOG.error("Repository Exception", e);
            } catch (ClientLibraryCachingException e) {
                LOG.error("Client Library Caching Exception encountered while processing page content events", e);
            }
        }
    }

}
