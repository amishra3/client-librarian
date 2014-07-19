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
package com.citytechinc.cq.clientlibs.api.util;

import java.util.Iterator;
import java.util.Set;

import javax.jcr.query.Query;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComponentUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentUtils.class);

    private static final String TYPED_COMPONENT_QUERY = "SELECT * FROM \"nt:base\" WHERE ISDESCENDANTNODE( \'{path}\' ) AND [sling:resourceType] IS NOT NULL";

    public static Set<String> getNestedComponentTypes(Resource root) {
        return getNestedComponentTypes(root, true);
    }

    public static Set<Resource> flattenResourceTree(Resource root, boolean inclusive) {

        Iterator<Resource> typedResourceIterator = root.getResourceResolver().findResources(TYPED_COMPONENT_QUERY.replace("{path}", root.getPath()), Query.JCR_SQL2);

        Set<Resource> flattenedResourceTree = Sets.newHashSet(typedResourceIterator);

        if (inclusive) {
            flattenedResourceTree.add(root);
        }

        return flattenedResourceTree;

    }

    public static Set<String> getNestedComponentTypes(Resource root, boolean inclusive) {

        Set<Resource> flattenedResourceTree = flattenResourceTree(root, inclusive);

        Set<String> resourceTypeSet = Sets.newHashSet();

        for (Resource currentResource : flattenedResourceTree) {
            if (currentResource.getResourceType() != null && StringUtils.isNotBlank(currentResource.getResourceType())) {
                resourceTypeSet.add(currentResource.getResourceType());
            }
        }

        return resourceTypeSet;

    }
}
