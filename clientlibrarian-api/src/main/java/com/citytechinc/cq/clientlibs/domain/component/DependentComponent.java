package com.citytechinc.cq.clientlibs.domain.component;

import com.day.cq.wcm.api.components.Component;
import org.apache.sling.api.resource.Resource;

import java.util.Set;

public interface DependentComponent {

    public Resource getResource();

    public Set<String> getDependencies();

    public Component getComponent();

    public String getResourceType();

    public String getResourceSuperType();

}
