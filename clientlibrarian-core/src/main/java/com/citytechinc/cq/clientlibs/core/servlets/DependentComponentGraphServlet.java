package com.citytechinc.cq.clientlibs.core.servlets;

import com.citytechinc.cq.clientlibs.api.constants.ServletConstants;
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

/**
 * Servlet wrapper for {@link com.citytechinc.cq.clientlibs.api.services.components.DependentComponentManager} service
 *  which provides access via JSON to data about Dependent Components in the repository. Only responds to GET requests.
 */
@SlingServlet(
        name = "Dependent Component Graph Servlet",
        description = "Provides a JSON representation of component client library dependencies.",
        paths = "/bin/clientlibrarian/graphs/components",
        extensions = "json",
        methods = "GET"
)
public class DependentComponentGraphServlet extends SlingSafeMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(DependentComponentGraphServlet.class);

    private static final String REQ_PARAM_COMPONENT_RESOURCE_TYPE = "resourceType";
    private static final String REQ_PARAM_CATEGORY = "category";
    
    private static final String RESP_KEY_COMPONENTS = "components";

    private static final String RESP_KEY_COMPONENT_PATH = "path";
    private static final String RESP_KEY_COMPONENT_TITLE = "title";
    private static final String RESP_KEY_COMPONENT_DESCRIPTION = "description";
    private static final String RESP_KEY_COMPONENT_RESOURCE_TYPE = "resourceType";
    private static final String RESP_KEY_COMPONENT_RESOURCE_SUPER_TYPE = "resourceSuperType";
    private static final String RESP_KEY_COMPONENT_DEPENDENCIES = "dependencies";

    @Reference
    private DependentComponentManager dependentComponentManager;

// TODO : make this return an object instead of an array, will not be viable to loop through an array to make graph

    /**
     * GET request handler. Takes the following parameters:
     *
     *  resourceType    = set this to a resource type to search for only that particular component.
     *  category        = limits the search to only components that have the given category.
     *
     * All parameters are optional. If no parameters are provided, all components will be returned.
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
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
        JSONObject jsonComponents = new JSONObject();

        // figure out where the collection of components is coming from
        Collection<DependentComponent> components =
                categoryGiven ?
                        dependentComponentManager.getComponentsDependentOnLibraryCategory(paramCategory)
                            : dependentComponentManager.getComponentsByPath().values();

        // loop through component collection and write them out to JSON objects
        for(DependentComponent component : components) {

            try {

                // check resource type if it was given as a parameter, add component JSON to response collection
                if (!resourceTypeGiven || StringUtils.equals(paramResourceType, component.getResourceType())) {

                    jsonComponents.put(component.getComponent().getPath(), buildJsonComponent(component));

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
            jsonResponse.put(ServletConstants.RESP_KEY_STATUS, jsonStatusMessages.length() > 0 ? jsonStatusMessages : ServletConstants.STATUS_SUCCESS);
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
