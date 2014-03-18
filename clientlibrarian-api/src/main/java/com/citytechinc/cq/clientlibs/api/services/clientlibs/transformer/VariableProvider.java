package com.citytechinc.cq.clientlibs.api.services.clientlibs.transformer;


import org.apache.sling.api.resource.Resource;

import java.util.Map;

public interface VariableProvider {

    public Map<String, String> getVariables(Resource root);

}
