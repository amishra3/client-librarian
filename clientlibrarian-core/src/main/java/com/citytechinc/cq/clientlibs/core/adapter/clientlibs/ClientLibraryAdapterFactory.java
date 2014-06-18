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
