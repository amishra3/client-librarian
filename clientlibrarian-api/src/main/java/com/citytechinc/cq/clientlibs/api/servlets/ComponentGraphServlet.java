package com.citytechinc.cq.clientlibs.api.servlets;

import com.citytechinc.cq.clientlibs.api.domain.component.DependentComponent;
import com.citytechinc.cq.clientlibs.api.services.components.DependentComponentManager;
import com.google.common.net.MediaType;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

@SlingServlet(
        name = "Component Graph Servlet",
        description = "Provides a JSON representation of component client library dependencies.",
        paths = "/bin/clientlibrarian/graphs/component",
        extensions = "json",
        methods = "GET"
)
public class ComponentGraphServlet extends SlingSafeMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentGraphServlet.class);

// TODO : implement functionality for these params
    private static final String REQ_PARAM_RESOLVE_DEPENDENCIES = "resolve";
    private static final String REQ_PARAM_COMPONENT_RESOURCE_TYPE = "resourceType";
    private static final String REQ_PARAM_CATEGORY = "category";
    
    private static final String RESP_KEY_STATUS = "status";
    private static final String RESP_KEY_COMPONENTS = "components";

    private static final String RESP_KEY_COMPONENT_PATH = "path";
    private static final String RESP_KEY_COMPONENT_TITLE = "title";
    private static final String RESP_KEY_COMPONENT_DESCRIPTION = "description";
    private static final String RESP_KEY_COMPONENT_RESOURCE_TYPE = "resourceType";
    private static final String RESP_KEY_COMPONENT_RESOURCE_SUPER_TYPE = "resourceSuperType";
    private static final String RESP_KEY_COMPONENT_DEPENDENCIES = "dependencies";

    private static final String STATUS_SUCCESS = "success";

    @Reference
    private DependentComponentManager dependentComponentManager;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {

        // get request parameters
        String paramResourceType = request.getParameter(REQ_PARAM_COMPONENT_RESOURCE_TYPE);
        String paramCategory = request.getParameter(REQ_PARAM_CATEGORY);

        // flags for parameters
        boolean resourceTypeGiven = StringUtils.isNotBlank(paramResourceType);
        boolean categoryGiven = StringUtils.isNotBlank(paramCategory);

        // set up response stuff
        JSONObject jsonResponse = new JSONObject();
        int statusCode = 400;

        // set up status message array
        JSONArray jsonStatusMessages = new JSONArray();

        // initialize components json
        JSONArray jsonComponents = new JSONArray();

        // figure out where the collection of components is coming from
        Collection<DependentComponent> components =
                categoryGiven ?
                        dependentComponentManager.getComponentsDependentOnLibraryCategory(paramCategory)
                            : dependentComponentManager.getComponentsByPath().values();

        // category is given, use manager function to only get components from given category
        for(DependentComponent component : components) {

            try {

                // check resource type if it was given as a parameter, add component JSON to response collection
                if (!resourceTypeGiven || StringUtils.equals(paramResourceType, component.getResourceType())) {

                    jsonComponents.put(buildJsonComponent(component));

                }

            } catch (JSONException e) {

                // problem occurred writing out JSON, log and return an error message
                String errMsg = "Could not write out component at " + component.getComponent().getPath() + ".";
                LOGGER.error(errMsg, e);
                jsonStatusMessages.put(errMsg);

            }

        }

        try {

            // add objects to response JSON
            jsonResponse.put(RESP_KEY_STATUS, jsonStatusMessages.length() > 0 ? jsonStatusMessages : STATUS_SUCCESS);
            jsonResponse.put(RESP_KEY_COMPONENTS, jsonComponents);

        } catch (JSONException e) {

            // could not write out JSON for some reason, error out with a 500
            LOGGER.error("An error occurred.", e);
            statusCode = 500;

        }


        // write response
        response.setStatus(statusCode);
        response.setContentType(MediaType.JSON_UTF_8.toString());
        response.getWriter().write(jsonResponse.toString());

    }

    /**
     * Create a {@link org.apache.sling.commons.json.JSONObject} based on a {@link com.citytechinc.cq.clientlibs.api.domain.component.DependentComponent}.
     *
     * @param component component to transform into JSON.
     * @return  {@link org.apache.sling.commons.json.JSONObject} containing component data.
     */
    private static JSONObject buildJsonComponent(DependentComponent component) throws JSONException {

        JSONObject jsonComponent = new JSONObject();

        jsonComponent.put(RESP_KEY_COMPONENT_PATH, component.getComponent().getPath());
        jsonComponent.put(RESP_KEY_COMPONENT_TITLE, component.getComponent().getTitle());
        jsonComponent.put(RESP_KEY_COMPONENT_DESCRIPTION, component.getComponent().getDescription());
        jsonComponent.put(RESP_KEY_COMPONENT_RESOURCE_TYPE, component.getResourceType());
        jsonComponent.put(RESP_KEY_COMPONENT_RESOURCE_SUPER_TYPE, component.getResourceSuperType());
        jsonComponent.put(RESP_KEY_COMPONENT_DEPENDENCIES, component.getDependencies());

        return jsonComponent;

    }

}
