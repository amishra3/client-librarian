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
package com.citytechinc.cq.clientlibs.core.domain.library;

import com.citytechinc.cq.clientlibs.api.constants.Properties;
import com.citytechinc.cq.clientlibs.api.domain.library.ClientLibrary;
import com.citytechinc.cq.clientlibs.api.domain.sling.runmode.SlingRunModeGroup;
import com.citytechinc.cq.clientlibs.core.domain.library.impl.DefaultClientLibrary;
import com.citytechinc.cq.clientlibs.core.domain.sling.runmode.SlingRunModeGroups;
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
        Set<String> clientLibraryCategories = Sets.newHashSet(clientLibraryValueMap.get(Properties.CLIENT_LIBRARY_CATEGORIES, new String[0]));
        List<String> clientLibraryEmbeds = Lists.newArrayList(clientLibraryValueMap.get(Properties.CLIENT_LIBRARY_EMBED, new String[0]));
        List<String> clientLibraryDependencies = Lists.newArrayList(clientLibraryValueMap.get(Properties.CLIENT_LIBRARY_DEPENDENCIES, new String[0]));
        List<String> clientLibraryConditionalDependencies = Lists.newArrayList(clientLibraryValueMap.get(Properties.CLIENT_LIBRARY_CONDITIONAL_DEPENDENCIES, new String[0]));
        Set<String> clientLibrarySlingRunModes = Sets.newHashSet(clientLibraryValueMap.get(Properties.CLIENT_LIBRARY_RUN_MODES, new String[0]));
        Set<String> clientLibraryBrands = Sets.newHashSet(clientLibraryValueMap.get(Properties.CLIENT_LIBRARY_BRANDS, new String[0]));

        Set<SlingRunModeGroup> slingRunModeGroupSet = Sets.newHashSet();

        for (String currentCompositeRunMode : clientLibrarySlingRunModes) {
            slingRunModeGroupSet.add(SlingRunModeGroups.forCompositeRunMode(currentCompositeRunMode));
        }

        return new DefaultClientLibrary(clientLibraryCategories, clientLibraryResource, clientLibraryEmbeds, clientLibraryDependencies, clientLibraryConditionalDependencies, slingRunModeGroupSet, clientLibraryBrands);

    }

    public static ClientLibrary newDefaultClientLibrary(Set<String> categories, Resource clientLibraryResource, List<String> embeddedCategories, List<String> dependencies, List<String> conditionalDependencies, Set<SlingRunModeGroup> slingRunModeGroups, Set<String> brands) {

        return new DefaultClientLibrary(categories, clientLibraryResource, embeddedCategories, dependencies, conditionalDependencies, slingRunModeGroups, brands);

    }
}
