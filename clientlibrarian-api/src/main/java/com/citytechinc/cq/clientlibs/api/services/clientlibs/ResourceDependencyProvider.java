package com.citytechinc.cq.clientlibs.api.services.clientlibs;

import com.citytechinc.cq.clientlibs.api.domain.library.ClientLibrary;
import org.apache.sling.api.resource.Resource;

import java.util.Set;

public interface ResourceDependencyProvider {

    public Set<ClientLibrary> getDependenciesForResource(Resource r);

}
