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
package com.citytechinc.cq.clientlibs.core.services.clientlibs.impl;


import com.citytechinc.cq.clientlibs.api.constants.Types;
import com.citytechinc.cq.clientlibs.api.services.clientlibs.ResourceProviderHelper;
import com.citytechinc.cq.clientlibs.api.util.ComponentUtils;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;

import java.util.Set;

/**
 * <p>
 * The Inheriting Paragraph System is a component which behaves largely like a Paragraph System the difference
 * being that content from a parent page may be inherited to a child page so long as the path to the paragraph system
 * relative to the containing page is that same between the parent and the child page.
 * </p>
 * <p>
 * Inheritance on an individual Paragraph System may be cancelled or disabled.
 * </p>
 *
 * <ul>
 *     <li>Cancelled: When inheritance is cancelled, paragraphs from pages which are parents of the page on which
 *     inheritance was cancelled are inherited by the page on which inheritance was cancelled however pages which
 *     are children of the page on which inheritance was cancelled will not receive the inherited paragraphs from
 *     any parent pages.</li>
 *     <li>Disabled: When inheritance is disabled, paragraphs from pages which are parents of the page on which
 *     inheritance was disabled are not inherited by the page on which inheritance was disabled and child pages
 *     of the page on which inheritance was disabled will not receive the inherited paragraphs.  In this way,
 *     disabled mirrors the cancelled functionality but also stops inheritance on the page in which the property
 *     was set.</li>
 * </ul>
 */
@Component(label = "Inheriting Parsys ResourceProviderHelper")
@Service
public class IParsysResourceProviderHelper implements ResourceProviderHelper {

    public static final String IPARSYS_INHERITANCE_PROPERTY = "inheritance";
    public static final String IPARSYS_INHERITANCE_CANCEL_PROPERTY_VALUE = "cancel";
    public static final String IPARSYS_FAKE_PAR_RESOURCE_NAME = "iparsys_fake_par";

    private static final Set<String> RESOURCE_TYPES_SERVED = Sets.newHashSet(Types.IPARSYS);

    protected boolean isInheritanceDisabledForIParsys(Resource iParsysResource) {

        Resource fakePar = iParsysResource.getChild(IPARSYS_FAKE_PAR_RESOURCE_NAME);

        if (fakePar != null) {
            ValueMap fakeParValueMap = fakePar.adaptTo(ValueMap.class);

            if (IPARSYS_INHERITANCE_CANCEL_PROPERTY_VALUE.equals(fakeParValueMap.get(IPARSYS_INHERITANCE_PROPERTY, String.class))) {
                return true;
            }
        }

        return false;

    }

    protected boolean isInheritanceCancelledForIParsys(Resource iParsysResource) {

        ValueMap currentIParsysValueMap = iParsysResource.adaptTo(ValueMap.class);
        String inheritance = currentIParsysValueMap.get(IPARSYS_INHERITANCE_PROPERTY, String.class);

        if (IPARSYS_INHERITANCE_CANCEL_PROPERTY_VALUE.equals(inheritance)) {
            return true;
        }

        return false;

    }

    protected boolean isInheritanceCancelledOrDisabledForIParsys(Resource iParsysResource) {
        return isInheritanceCancelledForIParsys(iParsysResource) || isInheritanceDisabledForIParsys(iParsysResource);
    }

    @Override
    public Set<Resource> getContainedResources(Resource resource) {

        Set<Resource> containedResources = Sets.newHashSet();

        //Check whether inheritance has been disabled
        if (isInheritanceDisabledForIParsys(resource)) {
            return containedResources;
        }

        ResourceResolver resourceResolver = resource.getResourceResolver();
        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);

        Page currentPage = pageManager.getContainingPage(resource);

        if (currentPage != null) {
            String iparsysRelativePath = resource.getPath().substring(currentPage.getPath().length() + 1);
            Page parentPage = currentPage.getParent();

            while (parentPage != null) {
                Resource currentIParsysResource = resourceResolver.getResource(parentPage.getPath() + "/" + iparsysRelativePath);

                if (currentIParsysResource != null) {

                    if (isInheritanceCancelledOrDisabledForIParsys(currentIParsysResource)) {
                        return containedResources;
                    }

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
