/**
 * Copyright 2014, CITYTECH, Inc.
 * All rights reserved - Do Not Redistribute
 * Confidential and Proprietary
 */
package com.citytechinc.cq.clientlibs.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import com.citytechinc.cq.clientlibs.domain.library.LibraryType;
import com.citytechinc.cq.clientlibs.services.clientlibs.ClientLibraryRepository;
import com.citytechinc.cq.clientlibs.services.clientlibs.exceptions.ClientLibraryCompilationException;
import com.google.common.base.Optional;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
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

        try {
            String compiledLibrary = clientLibraryRepository.compileClientLibrary(request.getResource(), requestedLibraryType.get());
            response.setContentType(requestedLibraryType.get().contentType);
            response.getWriter().write(compiledLibrary);
        } catch (ClientLibraryCompilationException e) {
            LOG.error("Error encountered requesting page library for " + request.getResource().getPath(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
