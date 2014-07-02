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
package com.citytechinc.cq.clientlibs.core.adapter.clientlibs;

import com.citytechinc.cq.clientlibs.api.domain.library.ClientLibrary;
import com.citytechinc.cq.clientlibs.core.domain.library.ClientLibraries;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.osgi.framework.Constants;

@Component
@Service
@Properties({
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "Client Librarian Client Library Adapter Factory"),
        @Property(name = "adaptables", value = {
                "org.apache.sling.api.resource.Resource"
        }),
        @Property(name = "adapters", value = {
                "com.citytechinc.cq.clientlibs.api.domain.library.ClientLibrary"
        })
})
public class ClientLibraryAdapterFactory implements AdapterFactory {

    @Override
    public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {

        if (adaptable instanceof Resource) {
            return getAdapter((Resource) adaptable, type);
        }

        return null;
    }

    private <AdapterType> AdapterType getAdapter(Resource adaptable, Class<AdapterType> type) {

        if (ClientLibrary.class == type) {
            return (AdapterType) ClientLibraries.forResource(adaptable);
        }

        return null;

    }

}
