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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import com.citytechinc.cq.clientlibs.api.domain.library.LibraryType;
import com.citytechinc.cq.clientlibs.api.services.clientlibs.ClientLibraryRepository;
import com.citytechinc.cq.clientlibs.api.services.clientlibs.exceptions.ClientLibraryCompilationException;
import com.day.cq.commons.jcr.JcrConstants;
import com.google.common.base.Optional;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.NameConstants;

@SlingServlet(
    resourceTypes = { NameConstants.NT_PAGE },
    selectors = { ComponentClientLibraryServlet.SELECTOR },
    extensions = { LibraryType.JS_EXTENSION, LibraryType.CSS_EXTENSION },
    methods = { "GET" })
public class ComponentClientLibraryServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 5890860219429530486L;

    private static final Logger LOG = LoggerFactory.getLogger(ComponentClientLibraryServlet.class);

    public static final String SELECTOR = "pagelib";

    @Reference
    ClientLibraryRepository clientLibraryRepository;

    @Override
    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        Optional<LibraryType> requestedLibraryType = Optional.fromNullable(LibraryType.fromRequest(request));

        if (requestedLibraryType == null) {
            LOG.error("Invalid extension for client library");
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Optional<String> brand = lookupBrandForRequest(request);

        try {
            final Resource jcrContent = request.getResource().getChild(JcrConstants.JCR_CONTENT);

            String compiledLibrary = clientLibraryRepository.compileClientLibrary(jcrContent, requestedLibraryType.get(), brand);
            response.setContentType(requestedLibraryType.get().contentType);
            response.getWriter().write(compiledLibrary);
        } catch (ClientLibraryCompilationException e) {
            LOG.error("Error encountered requesting page library for " + request.getResource().getPath(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private static Optional<String> lookupBrandForRequest(SlingHttpServletRequest request) {

        String[] selectors = request.getRequestPathInfo().getSelectors();

        for (String currentSelector : selectors) {
            if (!NumberUtils.isNumber(currentSelector) && !SELECTOR.equals(currentSelector)) {
                return Optional.of(currentSelector);
            }
        }

        return Optional.absent();

    }
}
