/**
 * Copyright 2014, CITYTECH, Inc.
 * All rights reserved - Do Not Redistribute
 * Confidential and Proprietary
 */
package com.citytechinc.cq.clientlibs.api.util;

import java.util.HashSet;
import java.util.Set;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComponentUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentUtils.class);

    private static final String TYPED_COMPONENT_QUERY = "SELECT * FROM \"nt:base\" WHERE ISDESCENDANTNODE( \'{path}\' ) AND [sling:resourceType] IS NOT NULL";

    public static final Set<String> getNestedComponentTypes(Resource root) throws InvalidQueryException, RepositoryException {
        return getNestedComponentTypes(root, true);
    }

    public static final Set<String> getNestedComponentTypes(Resource root, boolean inclusive) throws InvalidQueryException, RepositoryException {

        QueryManager queryManager = root.getResourceResolver().adaptTo(Session.class).getWorkspace().getQueryManager();

        LOG.debug("Requesting nested components for " + root);

        Query componentQuery = queryManager.createQuery(TYPED_COMPONENT_QUERY.replace("{path}", root.getPath()), Query.JCR_SQL2);

        QueryResult componentQueryResult = componentQuery.execute();

        Set<String> retSet = new HashSet<String>();

        RowIterator resultIterator = componentQueryResult.getRows();

        while (resultIterator.hasNext()) {
            Row curResultRow = resultIterator.nextRow();

            Property resourceType = curResultRow.getNode().getProperty("sling:resourceType");

            if (resourceType != null) {
                retSet.add(resourceType.getString());
            }
        }

        return retSet;
    }
}
