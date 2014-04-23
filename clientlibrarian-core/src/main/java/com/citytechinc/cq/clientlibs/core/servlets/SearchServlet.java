package com.citytechinc.cq.clientlibs.core.servlets;

import com.citytechinc.cq.clientlibs.api.constants.ServletConstants;
import com.citytechinc.cq.clientlibs.api.domain.component.DependentComponent;
import com.citytechinc.cq.clientlibs.api.domain.library.ClientLibrary;
import com.citytechinc.cq.clientlibs.api.services.clientlibs.ClientLibraryManager;
import com.citytechinc.cq.clientlibs.api.services.components.DependentComponentManager;
import com.citytechinc.cq.clientlibs.core.util.DomainToJSONUtil;
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
 * Servlet wrapper for client library services. Provides access to JSON representations of libraries, components, and
 *  categories.
 */
@SlingServlet(
        name = "Client Librarian - Search Servlet",
        description = "Provides a JSON representation of client libraries, components, and categories.",
        paths = "/bin/clientlibrarian/search",
        extensions = "json",
        methods = "GET"
)
public class SearchServlet extends SlingSafeMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchServlet.class);

    private static final String REQ_PARAM_LIBRARY_INCLUDE = "libraries.include";
    private static final String REQ_PARAM_LIBRARY_PATH = "libraries.path";
    private static final String REQ_PARAM_COMPONENTS_INCLUDE = "components.include";
    private static final String REQ_PARAM_COMPONENT_RESOURCE_TYPE = "components.resourceType";
    private static final String REQ_PARAM_LIMIT_CATEGORY = "limit.category";

    private static final String RESP_KEY_LIBRARIES = "libraries";
    private static final String RESP_KEY_COMPONENTS = "components";

    @Reference
    private DependentComponentManager dependentComponentManager;

    @Reference
    private ClientLibraryManager clientLibraryManager;

    /**
     * GET request handler. Takes the following parameters:
     *
     *  libraries.include       = set to true to have response include libraries.
     *  libraries.path          = an absolute path to a library to return.
     *  components.include      = set to true to have response include components.
     *  components.resourceType = set this to a resource type to search for only that particular component.
     *  limit.category          = limits the libraries and components returned to only those in a given category. This
     *                              may produce confusing results if used in conjunction with library or category
     *                              specific restrictions.
     *
     * All parameters are optional. If no parameters are provided, nothing will be returned.
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {

        // figure out what response wants
        boolean includeLibraries = Boolean.valueOf(request.getParameter(REQ_PARAM_LIBRARY_INCLUDE));
        boolean includeComponents = Boolean.valueOf(request.getParameter(REQ_PARAM_COMPONENTS_INCLUDE));

        // category provided?
        String paramCategory = request.getParameter(REQ_PARAM_LIMIT_CATEGORY);
        boolean categoryGiven = StringUtils.isNotBlank(paramCategory);

        // set up response stuff
        JSONObject jsonResponse = new JSONObject();
        int statusCode = 400;

        // initialize json collections for response
        JSONArray jsonStatusMessages = new JSONArray();
        JSONObject jsonLibraries = new JSONObject();
        JSONObject jsonComponents = new JSONObject();

        // libraries should be included
        if(includeLibraries) {

            // libraries should be included, get necessary stuff out of request
            String paramLibraryPath = request.getParameter(REQ_PARAM_LIBRARY_PATH);
            boolean libraryPathGiven = StringUtils.isNotBlank(paramLibraryPath);

            // no specific library given, return collection of libraries
            Collection<ClientLibrary> libraries =
                    categoryGiven ?
                            clientLibraryManager.getLibrariesForCategory(paramCategory)
                            : clientLibraryManager.getAllLibraries();

            // loop through libraries and write them out to JSON objects
            for (ClientLibrary library : libraries) {

                // if path was given check to make sure it matches, otherwise, just add library
                if (!libraryPathGiven || StringUtils.equals(paramLibraryPath, library.getClientLibraryPath())) {

                    try {

                        jsonLibraries.put(library.getClientLibraryPath(), DomainToJSONUtil.buildJsonLibrary(library));

                        if(libraryPathGiven) {

                            // library path was given, therefore to be here a match happened, break out of loop
                            break;

                        }

                    } catch (JSONException e) {

                        // problem occurred writing out JSON, log and return an error message
                        String errMsg = "Could not write out library at " + library.getClientLibraryPath() + ".";
                        LOGGER.error(errMsg, e);
                        jsonStatusMessages.put(errMsg);

                    }

                }

            }

        }

        // check if components should be included
        if (includeComponents) {

            // get necessary stuff out of request
            String paramResourceType = request.getParameter(REQ_PARAM_COMPONENT_RESOURCE_TYPE);
            boolean resourceTypeGiven = StringUtils.isNotBlank(paramResourceType);

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

                        jsonComponents.put(component.getComponent().getPath(), DomainToJSONUtil.buildJsonComponent(component));

                    }

                } catch (JSONException e) {

                    // problem occurred writing out JSON, log and return an error message
                    String errMsg = "Could not write out component at " + component.getComponent().getPath() + ".";
                    LOGGER.error(errMsg, e);
                    jsonStatusMessages.put(errMsg);

                }

            }

        }

        try {

            // add objects to response JSON
            jsonResponse.put(ServletConstants.RESP_KEY_STATUS, jsonStatusMessages.length() > 0 ? jsonStatusMessages : ServletConstants.STATUS_SUCCESS);
            jsonResponse.put(RESP_KEY_LIBRARIES, jsonLibraries);
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

}
