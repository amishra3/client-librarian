/**
 * Copyright 2014, CITYTECH, Inc.
 * All rights reserved - Do Not Redistribute
 * Confidential and Proprietary
 */
package com.citytechinc.cq.clientlibs.domain.component.impl;

import java.util.Set;

import com.citytechinc.cq.clientlibs.domain.component.DependentComponent;
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
