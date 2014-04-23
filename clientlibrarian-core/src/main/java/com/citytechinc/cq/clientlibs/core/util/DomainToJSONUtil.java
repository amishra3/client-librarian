package com.citytechinc.cq.clientlibs.core.util;

import com.citytechinc.cq.clientlibs.api.domain.component.DependentComponent;
import com.citytechinc.cq.clientlibs.api.domain.library.ClientLibrary;
import com.citytechinc.cq.clientlibs.api.domain.sling.runmode.SlingRunModeGroup;
import com.citytechinc.cq.clientlibs.core.structures.graph.dag.EdgeType;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

public class DomainToJSONUtil {

    private static final String KEY_NODE_ID = "id";

    private static final String KEY_TYPE = "type";
    private static final String TYPE_CATEGORY = "category";
    private static final String TYPE_LIBRARY = "library";
    private static final String TYPE_COMPONENT = "component";

    private static final String KEY_CATEGORY_NAME = "name";

    private static final String KEY_COMPONENT_PATH = "path";
    private static final String KEY_COMPONENT_TITLE = "title";
    private static final String KEY_COMPONENT_DESCRIPTION = "description";
    private static final String KEY_COMPONENT_RESOURCE_TYPE = "resourceType";
    private static final String KEY_COMPONENT_RESOURCE_SUPER_TYPE = "resourceSuperType";
    private static final String KEY_COMPONENT_DEPENDENCIES = "dependencies";

    private static final String KEY_LIBRARY_PATH = "path";
    private static final String KEY_LIBRARY_INCLUDE_PATHS = "includePaths";
    private static final String KEY_LIBRARY_RESOURCE_PATHS = "resourcePaths";
    private static final String KEY_LIBRARY_CATEGORIES = "categories";
    private static final String KEY_LIBRARY_EMBEDS = "embeds";
    private static final String KEY_LIBRARY_DEPENDENCIES = "dependencies";
    private static final String KEY_LIBRARY_RUN_MODES = "runModes";

    private static final String KEY_EDGE_FROM = "from";
    private static final String KEY_EDGE_TO = "to";
    private static final String KEY_EDGE_TYPE = "type";

    /**
     * Create a {@link org.apache.sling.commons.json.JSONObject} based on a category.
     *
     * @param category  Name of category
     * @return  {@link org.apache.sling.commons.json.JSONObject} containing category data.
     * @throws JSONException
     */
    public static JSONObject buildJsonCategory(String category) throws JSONException {

        JSONObject jsonCategory = new JSONObject();

        jsonCategory.put(KEY_NODE_ID, category);
        jsonCategory.put(KEY_TYPE, TYPE_CATEGORY);
        jsonCategory.put(KEY_CATEGORY_NAME, category);

        return jsonCategory;

    }

    /**
     * Create a {@link org.apache.sling.commons.json.JSONObject} based on a {@link com.citytechinc.cq.clientlibs.api.domain.library.ClientLibrary}.
     *
     * @param clientLibrary library to transform into JSON.
     * @return  {@link org.apache.sling.commons.json.JSONObject} containing library data.
     * @throws org.apache.sling.commons.json.JSONException
     */
    public static JSONObject buildJsonLibrary(ClientLibrary clientLibrary) throws JSONException {

        JSONObject jsonClientLibrary = new JSONObject();

        jsonClientLibrary.put(KEY_NODE_ID, clientLibrary.getClientLibraryPath());
        jsonClientLibrary.put(KEY_TYPE, TYPE_LIBRARY);
        jsonClientLibrary.put(KEY_LIBRARY_PATH, clientLibrary.getClientLibraryPath());
        jsonClientLibrary.put(KEY_LIBRARY_INCLUDE_PATHS, clientLibrary.getLibraryIncludeFilePaths());
        jsonClientLibrary.put(KEY_LIBRARY_RESOURCE_PATHS, clientLibrary.getClientLibraryResourcePaths());
        jsonClientLibrary.put(KEY_LIBRARY_CATEGORIES, clientLibrary.getCategories());
        jsonClientLibrary.put(KEY_LIBRARY_EMBEDS, clientLibrary.getEmbeddedCategories());
        jsonClientLibrary.put(KEY_LIBRARY_DEPENDENCIES, clientLibrary.getDependencies());

// TODO : is there some way to just get the string array of runmodes available?
        jsonClientLibrary.put(KEY_LIBRARY_RUN_MODES, clientLibrary.getRunModeGroups());


        return jsonClientLibrary;

    }

    /**
     * Create a {@link org.apache.sling.commons.json.JSONObject} based on a component resource type. This is for components
     *  that cannot be cast as {@link com.citytechinc.cq.clientlibs.api.domain.component.DependentComponent} which is
     *  the case with OOTB components.
     *
     * @param resourceType  Resource type of component.
     * @return {@link org.apache.sling.commons.json.JSONObject} containing component data.
     * @throws JSONException
     */
    public static JSONObject buildJsonBareComponent(String resourceType) throws JSONException {

        JSONObject jsonComponent = new JSONObject();

        jsonComponent.put(KEY_NODE_ID, resourceType);
        jsonComponent.put(KEY_TYPE, TYPE_COMPONENT);
        jsonComponent.put(KEY_COMPONENT_RESOURCE_TYPE, resourceType);

        return jsonComponent;

    }

    /**
     * Create a {@link org.apache.sling.commons.json.JSONObject} based on a {@link com.citytechinc.cq.clientlibs.api.domain.component.DependentComponent}.
     *
     * @param component component to transform into JSON.
     * @return  {@link org.apache.sling.commons.json.JSONObject} containing component data.
     */
    public static JSONObject buildJsonComponent(DependentComponent component) throws JSONException {

        JSONObject jsonComponent = new JSONObject();

        jsonComponent.put(KEY_NODE_ID, component.getResourceType());
        jsonComponent.put(KEY_TYPE, TYPE_COMPONENT);
        jsonComponent.put(KEY_COMPONENT_PATH, component.getComponent().getPath());
        jsonComponent.put(KEY_COMPONENT_TITLE, component.getComponent().getTitle());
        jsonComponent.put(KEY_COMPONENT_DESCRIPTION, component.getComponent().getDescription());
        jsonComponent.put(KEY_COMPONENT_RESOURCE_TYPE, component.getResourceType());
        jsonComponent.put(KEY_COMPONENT_RESOURCE_SUPER_TYPE, component.getResourceSuperType());
        jsonComponent.put(KEY_COMPONENT_DEPENDENCIES, component.getDependencies());

        return jsonComponent;

    }

    /**
     * Create a JSON object representing the directed relationship between two nodes.
     *
     * @param from      identifier of the node this relationship is coming from.
     * @param to        identifier of the node this relationship is going to.
     * @param edgeType  type of edge this
     * @return  {@link org.apache.sling.commons.json.JSONObject} containing edge data.
     * @throws JSONException
     */
    public static JSONObject buildJsonEdge(String from, String to, EdgeType edgeType) throws JSONException {

        JSONObject jsonEdge = new JSONObject();

        jsonEdge.put(KEY_EDGE_FROM, from);
        jsonEdge.put(KEY_EDGE_TO, to);
        jsonEdge.put(KEY_EDGE_TYPE, edgeType.toString());

        return jsonEdge;

    }

}
