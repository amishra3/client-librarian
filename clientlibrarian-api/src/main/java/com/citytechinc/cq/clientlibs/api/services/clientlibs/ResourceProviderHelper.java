package com.citytechinc.cq.clientlibs.api.services.clientlibs;

import org.apache.sling.api.resource.Resource;

import java.util.Set;

/**
 * Implementations of the ResourceProviderHelper assist in the gathering Resources which are intended to be treated
 * as children at some depth of a given Resource.
 *
 * Currently implementations of the ResourceProviderHelper interface assist the DefaultResourceDependencyProvider
 * in its collection of dependencies for a given Resource.
 */
public interface ResourceProviderHelper {

    public Set<Resource> getContainedResources(Resource resource);

    public Set<String> getResourceTypesServed();

}
