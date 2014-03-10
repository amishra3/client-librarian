package com.citytechinc.cq.clientlibs.core.domain.library;

import com.citytechinc.cq.clientlibs.api.domain.library.ClientLibrary;
import com.citytechinc.cq.clientlibs.core.domain.library.impl.DefaultClientLibrary;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import java.util.List;
import java.util.Set;

public class ClientLibraries {

    private ClientLibraries() {}

    public static ClientLibrary forResource(Resource clientLibraryResource) {

        ValueMap clientLibraryValueMap = clientLibraryResource.adaptTo(ValueMap.class);
        Set<String> clientLibraryCategories = Sets.newHashSet(clientLibraryValueMap.get("categories", new String[0]));
        List<String> clientLibraryEmbeds = Lists.newArrayList(clientLibraryValueMap.get("embed", new String[0]));
        List<String> clientLibraryDependencies = Lists.newArrayList(clientLibraryValueMap.get("dependencies", new String[0]));

        return new DefaultClientLibrary(clientLibraryCategories, clientLibraryResource, clientLibraryEmbeds, clientLibraryDependencies);

    }

    public static ClientLibrary newDefaultClientLibrary(Set<String> categories, Resource clientLibraryResource, List<String> embeddedCategories, List<String> dependencies) {

        return new DefaultClientLibrary(categories, clientLibraryResource, embeddedCategories, dependencies);

    }
}
