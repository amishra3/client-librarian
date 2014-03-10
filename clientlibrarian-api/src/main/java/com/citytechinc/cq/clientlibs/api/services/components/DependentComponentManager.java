package com.citytechinc.cq.clientlibs.api.services.components;

import com.citytechinc.cq.clientlibs.api.domain.component.DependentComponent;
import com.google.common.base.Optional;
import org.apache.sling.api.resource.Resource;

import java.util.Map;
import java.util.Set;

public interface DependentComponentManager {

    public Optional<DependentComponent> getDependentComponentForResource(Resource r);

    public Optional<DependentComponent> getDependentComponentForResourceType(String resourceType);

    public Set<DependentComponent> getComponentsDependentOnLibraryCategory(String category);

    public Map<String, DependentComponent> getComponentsByPath();

    public void requestRefresh();

}
