package com.citytechinc.cq.clientlibs.core.domain.component;

import com.citytechinc.cq.clientlibs.api.domain.component.DependentComponent;
import com.citytechinc.cq.clientlibs.core.domain.component.impl.DefaultDependentComponent;
import org.apache.sling.api.resource.Resource;

import java.util.Set;

public class Components {

    private Components() {

    }

    public static DependentComponent forResourceAndDependencies(Resource componentResource, Set<String> dependencies) {
        return new DefaultDependentComponent(componentResource, dependencies);
    }

    public static DependentComponent forResourceAndSuperDependentComponent(Resource componentResource, DependentComponent superComponent) {
        return new DefaultDependentComponent(componentResource, superComponent.getDependencies());
    }

}
