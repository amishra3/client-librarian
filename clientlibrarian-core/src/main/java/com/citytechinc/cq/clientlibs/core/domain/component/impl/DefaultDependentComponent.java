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
package com.citytechinc.cq.clientlibs.core.domain.component.impl;

import java.util.Set;

import com.citytechinc.cq.clientlibs.api.constants.Properties;
import com.citytechinc.cq.clientlibs.api.domain.component.DependentComponent;
import com.citytechinc.cq.clientlibs.api.domain.component.EmbeddedComponent;
import com.citytechinc.cq.clientlibs.core.domain.component.EmbeddedComponents;
import com.day.cq.wcm.api.components.Component;
import com.google.common.collect.Sets;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultDependentComponent implements DependentComponent {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultDependentComponent.class);

    private final Set<String> dependencies;
    private final Resource componentResource;
    private final Component component;
    private final Set<EmbeddedComponent> embeddedComponents;

    public DefaultDependentComponent(Resource componentResource, Set<String> dependencies) {
        this.dependencies = dependencies;
        this.componentResource = componentResource;
        this.component = componentResource.adaptTo(Component.class);

        this.embeddedComponents = Sets.newHashSet();

        ValueMap componentValueMap = componentResource.adaptTo(ValueMap.class);
        String[] embeddedComponentDescriptors = componentValueMap.get(Properties.CLIENT_LIBRARY_EMBED, new String[0]);

        for (String currentEmbeddedComponentDescriptor : embeddedComponentDescriptors) {
            String[] embeddedComponentDescriptorParts = currentEmbeddedComponentDescriptor.split(":");

            if (embeddedComponentDescriptorParts.length == 2) {
                embeddedComponents.add(EmbeddedComponents.forRelativePathAndResourceType(embeddedComponentDescriptorParts[0], embeddedComponentDescriptorParts[1]));
            }
            else {
                LOG.error("Component descriptor " + currentEmbeddedComponentDescriptor + " within component " + componentResource.getPath() + " is malformed");
            }
        }
    }

    public Resource getResource() {
        return componentResource;
    }

    public Set<String> getDependencies() {
        return dependencies;
    }

    public Component getComponent() {
        return component;
    }

    public String getResourceType() {
        return component.getResourceType();
    }

    public String getResourceSuperType() {
        return componentResource.getResourceSuperType();
    }

    @Override
    public Set<EmbeddedComponent> getEmbeddedComponents() {
        return embeddedComponents;
    }

}
