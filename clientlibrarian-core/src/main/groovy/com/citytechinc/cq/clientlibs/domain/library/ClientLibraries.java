package com.citytechinc.cq.clientlibs.domain.library;

import com.citytechinc.cq.clientlibs.domain.library.impl.DefaultClientLibrary;
import org.apache.sling.api.resource.Resource;

import java.util.List;
import java.util.Set;

public class ClientLibraries {

    private ClientLibraries() {}

    public static ClientLibrary newDefaultClientLibrary(Set<String> categories, Resource clientLibraryResource, List<String> embeddedCategories, List<String> dependencies) {

        return new DefaultClientLibrary(categories, clientLibraryResource, embeddedCategories, dependencies);

    }
}
