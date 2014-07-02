/**
 * Copyright 2014 CITYTECH, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.citytechinc.cq.clientlibs.core.servlets;

import com.citytechinc.cq.clientlibs.api.constants.ServletConstants;
import com.citytechinc.cq.clientlibs.api.domain.component.DependentComponent;
import com.citytechinc.cq.clientlibs.api.domain.library.ClientLibrary;
import com.citytechinc.cq.clientlibs.api.services.clientlibs.ClientLibraryRepository;
import com.citytechinc.cq.clientlibs.api.services.components.DependentComponentManager;
import com.citytechinc.cq.clientlibs.api.structures.graph.DependencyGraph;
import com.citytechinc.cq.clientlibs.api.util.ComponentUtils;
import com.citytechinc.cq.clientlibs.core.structures.graph.dag.EdgeType;
import com.citytechinc.cq.clientlibs.core.util.DomainToJSONUtil;
import com.day.cq.commons.jcr.JcrConstants;
import com.google.common.net.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SlingServlet(
        name = "Client Librarian - Graph Servlet",
        description = "Provides a JSON representation of the graph used to resolve dependencies for a given page.",
        paths = "/bin/clientlibrarian/graph",
        extensions = "json",
        methods = "GET"
)
public class GraphServlet extends SlingSafeMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphServlet.class);

    private static final String REQ_PARAM_PAGE_PATH = "page.path";

    private static final String RESP_KEY_NODES = "nodes";
    private static final String RESP_KEY_EDGES = "edges";

    @Reference
    private DependentComponentManager dependentComponentManager;

    @Reference
    private ClientLibraryRepository clientLibraryRepository;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {

        // get request parameters
        String paramPagePath = request.getParameter(REQ_PARAM_PAGE_PATH);

        // set up response stuff
        JSONObject jsonResponse = new JSONObject();
        int statusCode = 400;

        // initialize json collections for response
        JSONArray jsonStatusMessages = new JSONArray();
        JSONArray jsonNodes = new JSONArray();
        JSONArray jsonEdges = new JSONArray();

        // resolve given path
        ResourceResolver resourceResolver = request.getResourceResolver();
        Resource pageResource = resourceResolver.resolve(paramPagePath);

        // check if path resolved
        if(StringUtils.isNotBlank(paramPagePath) && !ResourceUtil.isNonExistingResource(pageResource)) {

            // get content node
            Resource jcrContentResource = pageResource.getChild(JcrConstants.JCR_CONTENT);

            // keep track of categories
            Set<String> categories = new HashSet<String>();

            try {

                // write components to JSON
                for (String resourceType : ComponentUtils.getNestedComponentTypes(jcrContentResource)) {

                    // get dependent component
                    DependentComponent dependentComponent =
                            dependentComponentManager.getDependentComponentForResourceType(resourceType).orNull();

                    if(dependentComponent != null) {

                        // found dependent component, write it to JSON
                        jsonNodes.put(DomainToJSONUtil.buildJsonComponent(dependentComponent));

                        // add all dependencies to categories list, add edges from component to categories
                        String componentResourceType = dependentComponent.getResourceType();
                        Set<String> dependencies = dependentComponent.getDependencies();
                        for(String dependency : dependencies) {

                            JSONObject jsonEdge =
                                    DomainToJSONUtil.buildJsonEdge(componentResourceType, dependency, EdgeType.DEPENDS_ON);
                            jsonEdges.put(jsonEdge);

                        }
                        categories.addAll(dependencies);

                    } else {

                        // this is not a dependent component (probably OOTB), write it to JSON with what info it has
                        jsonNodes.put(DomainToJSONUtil.buildJsonBareComponent(resourceType));

                    }

                }

                // get graph
                DependencyGraph<ClientLibrary> dependencyGraph
                        = clientLibraryRepository.getClientLibraryDependencyGraph(jcrContentResource);

                // write client libraries to JSON
                for (ClientLibrary clientLibrary : dependencyGraph.getNodes()) {

                    jsonNodes.put(DomainToJSONUtil.buildJsonLibrary(clientLibrary));

                    String clientLibraryPath = clientLibrary.getClientLibraryPath();

                    // add all categories to categories list, add edges from categories to this library
                    Set<String> memberOfCategories = clientLibrary.getCategories();
                    for(String category : memberOfCategories) {

                        JSONObject jsonEdge =
                                DomainToJSONUtil.buildJsonEdge(clientLibraryPath, category, EdgeType.MEMBER_OF);
                        jsonEdges.put(jsonEdge);

                    }
                    categories.addAll(memberOfCategories);

                    // add all dependencies to categories list, add edges from categories to libraries
                    List<String> dependencies = clientLibrary.getDependencies();
                    for(String dependency : dependencies) {

                        JSONObject jsonEdge =
                                DomainToJSONUtil.buildJsonEdge(clientLibraryPath, dependency, EdgeType.DEPENDS_ON);
                        jsonEdges.put(jsonEdge);

                    }
                    categories.addAll(dependencies);

                    // add all embeds to categories list, add edges from embeds to categories
                    List<String> embeds = clientLibrary.getEmbeddedCategories();
                    for(String embed : embeds) {

                        JSONObject jsonEdge =
                                DomainToJSONUtil.buildJsonEdge(clientLibraryPath, embed, EdgeType.EMBEDS);
                        jsonEdges.put(jsonEdge);

                    }
                    categories.addAll(embeds);

                }

                // write categories to JSON
                for(String category : categories) {

                    jsonNodes.put(DomainToJSONUtil.buildJsonCategory(category));

                }

            } catch (JSONException e) {

                // JSON error
                LOGGER.error("Failed to build JSON. Returning 500.", e);
                jsonStatusMessages.put("Failed to build JSON.");
                statusCode = 500;

            } catch (InvalidQueryException e) {

                // error, log it set status code
                LOGGER.error("An error occurred. Returning 500.", e);
                jsonStatusMessages.put("Failed to build JSON.");
                statusCode = 500;

            } catch (RepositoryException e) {

                // error, log it set status code
                LOGGER.error("An error occurred. Returning 500.", e);
                jsonStatusMessages.put("Failed to build JSON.");
                statusCode = 500;

            }

        } else {

            // requested a non-existing resource, return a bad request
            LOGGER.error("Requested dependency graph for non-existing resource at '" + pageResource + "'. Returning 400.");
            jsonStatusMessages.put("No resource exists at '" + pageResource + "'.");
            statusCode = 400;

        }

        try {

            // add objects to response JSON
            jsonResponse.put(ServletConstants.RESP_KEY_STATUS, jsonStatusMessages.length() > 0 ? jsonStatusMessages : ServletConstants.STATUS_SUCCESS);

            if(statusCode != 500) {

                // haven't already failed, write out objects
                jsonResponse.put(RESP_KEY_NODES, jsonNodes);
                jsonResponse.put(RESP_KEY_EDGES, jsonEdges);

                // set status to OK
                statusCode = 200;

            }

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
