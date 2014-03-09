package com.citytechinc.cq.clientlibs.services.clientlibs;

import com.citytechinc.cq.clientlibs.domain.library.ClientLibrary;
import org.apache.sling.api.resource.Resource;

import java.util.Set;

public interface ResourceDependencyProvider {

    public Set<ClientLibrary> getDependenciesForResource(Resource r);

}
