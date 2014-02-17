package com.citytechinc.cq.clientlibs.services.clientlibs.actor.messages;

import org.apache.sling.api.resource.Resource;

public class OrderedDependenciesRequestMessage {

    private final Resource resource;

    public OrderedDependenciesRequestMessage(Resource resource) {
        this.resource = resource;
    }

    public Resource getResource() {
        return resource;
    }

    public static OrderedDependenciesRequestMessage forResource(Resource resource) {
        return new OrderedDependenciesRequestMessage(resource);
    }

}
