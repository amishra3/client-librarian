package com.citytechinc.cq.clientlibs.core.services.clientlibs.impl;


import com.citytechinc.cq.clientlibs.api.constants.Types;
import com.citytechinc.cq.clientlibs.api.services.clientlibs.ResourceProviderHelper;
import com.citytechinc.cq.clientlibs.api.util.ComponentUtils;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.google.common.collect.Sets;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;

import java.util.Set;

public class IParsysResourceProviderHelper implements ResourceProviderHelper {

    public static final String IPARSYS_INHERITANCE_PROPERTY = "inheritance";
    public static final String IPARSYS_INHERITANCE_CANCEL_PROPERTY_VALUE = "cancel";

    private static final Set<String> RESOURCE_TYPES_SERVED = Sets.newHashSet(Types.IPARSYS);

    @Override
    public Set<Resource> getContainedResources(Resource resource) {
        Set<Resource> containedResources = Sets.newHashSet();

        ResourceResolver resourceResolver = resource.getResourceResolver();
        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);

        Page currentPage = pageManager.getContainingPage(resource);

        if (currentPage != null) {
            String iparsysRelativePath = resource.getPath().substring(currentPage.getPath().length() + 1);
            Page parentPage = currentPage.getParent();

            while (parentPage != null) {
                Resource currentIParsysResource = resourceResolver.getResource(parentPage.getPath() + "/" + iparsysRelativePath);

                if (currentIParsysResource != null) {
                    containedResources.addAll(ComponentUtils.flattenResourceTree(currentIParsysResource, false));
                }

                parentPage = parentPage.getParent();
            }

        }

        return containedResources;
    }

    @Override
    public Set<String> getResourceTypesServed() {
        return RESOURCE_TYPES_SERVED;
    }

}
