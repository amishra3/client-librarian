package com.citytechinc.cq.clientlibs.core.servlets;

import com.citytechinc.cq.clientlibs.api.constants.ServletConstants;
import com.citytechinc.cq.clientlibs.api.domain.library.ClientLibrary;
import com.citytechinc.cq.clientlibs.api.services.clientlibs.ClientLibraryManager;
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
 * Servlet wrapper for {@link com.citytechinc.cq.clientlibs.api.services.clientlibs.ClientLibraryManager} service which
 *  provides access via JSON to data about client libraries in the repository. Only responds to GET requests.
 */
@SlingServlet(
        name = "Client Library Graph Servlet",
        description = "Provides a JSON representation of client libraries.",
        paths = "/bin/clientlibrarian/graphs/libraries",
        extensions = "json",
        methods = "GET"
)
public class ClientLibraryGraphServlet extends SlingSafeMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientLibraryGraphServlet.class);

    private static final String REQ_PARAM_CATEGORY = "category";

    private static final String RESP_KEY_LIBRARIES = "libraries";

    private static final String RESP_KEY_LIBRARY_PATH = "path";
    private static final String RESP_KEY_LIBRARY_INCLUDE_PATHS = "includePaths";
    private static final String RESP_KEY_LIBRARY_RESOURCE_PATHS = "resourcePaths";
    private static final String RESP_KEY_LIBRARY_CATEGORIES = "categories";
    private static final String RESP_KEY_LIBRARY_EMBEDS = "embeds";
    private static final String RESP_KEY_LIBRARY_RUN_MODES = "runModes";

    @Reference
    private ClientLibraryManager clientLibraryManager;

// TODO : make this return an object instead of an array, will not be viable to loop through an array to make graph

    /**
     * GET request handler. Takes the following parameters:
     *
     *  category    = limits the libraries returned to only those in a given category.
     *
     * All parameters are optional. If not parameters are provided, all libraries will be returned.
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {

        // get request parameters
        String paramCategory = request.getParameter(REQ_PARAM_CATEGORY);

        // flags for parameters
        boolean categoryGiven = StringUtils.isNotBlank(paramCategory);

        // set up response stuff
        JSONObject jsonResponse = new JSONObject();
        int statusCode = 400;

        // set up status message array
        JSONArray jsonStatusMessages = new JSONArray();

        // initialize library json
        JSONObject jsonLibraries = new JSONObject();

        Collection<ClientLibrary> libraries =
                categoryGiven ?
                        clientLibraryManager.getLibrariesForCategory(paramCategory)
                            : clientLibraryManager.getAllLibraries();

        // loop through libraries and write them out to JSON objects
        for(ClientLibrary library : libraries) {

            try {

                jsonLibraries.put(library.getClientLibraryPath(), buildJsonLibrary(library));

            } catch (JSONException e) {

                // problem occurred writing out JSON, log and return an error message
                String errMsg = "Could not write out library at " + library.getClientLibraryPath() + ".";
                LOGGER.error(errMsg, e);
                jsonStatusMessages.put(errMsg);

            }

        }

        try {

            // add objects to response JSON
            jsonResponse.put(ServletConstants.RESP_KEY_STATUS, jsonStatusMessages.length() > 0 ? jsonStatusMessages : ServletConstants.STATUS_SUCCESS);
            jsonResponse.put(RESP_KEY_LIBRARIES, jsonLibraries);

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
     * Create a {@link org.apache.sling.commons.json.JSONObject} based on a {@link com.citytechinc.cq.clientlibs.api.domain.library.ClientLibrary}.
     *
     * @param clientLibrary library to transform into JSON.
     * @return  {@link org.apache.sling.commons.json.JSONObject} containing library data.
     * @throws JSONException
     */
    private static JSONObject buildJsonLibrary(ClientLibrary clientLibrary) throws JSONException {

        JSONObject jsonClientLibrary = new JSONObject();

        jsonClientLibrary.put(RESP_KEY_LIBRARY_PATH, clientLibrary.getClientLibraryPath());
        jsonClientLibrary.put(RESP_KEY_LIBRARY_INCLUDE_PATHS, clientLibrary.getLibraryIncludeFilePaths());
        jsonClientLibrary.put(RESP_KEY_LIBRARY_RESOURCE_PATHS, clientLibrary.getClientLibraryResourcePaths());
        jsonClientLibrary.put(RESP_KEY_LIBRARY_CATEGORIES, clientLibrary.getCategories());
        jsonClientLibrary.put(RESP_KEY_LIBRARY_EMBEDS, clientLibrary.getEmbeddedCategories());
        jsonClientLibrary.put(RESP_KEY_LIBRARY_RUN_MODES, clientLibrary.getRunModeGroups());

        return jsonClientLibrary;

    }

}
