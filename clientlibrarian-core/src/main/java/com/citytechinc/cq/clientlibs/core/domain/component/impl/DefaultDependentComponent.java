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

import com.citytechinc.cq.clientlibs.api.domain.component.DependentComponent;
import com.day.cq.wcm.api.components.Component;
import org.apache.sling.api.resource.Resource;

public class DefaultDependentComponent implements DependentComponent {

    private final Set<String> dependencies;
    private final Resource componentResource;
    private final Component component;

    public DefaultDependentComponent(Resource componentResource, Set<String> dependencies) {
        this.dependencies = dependencies;
        this.componentResource = componentResource;
        this.component = componentResource.adaptTo(Component.class);
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

}
