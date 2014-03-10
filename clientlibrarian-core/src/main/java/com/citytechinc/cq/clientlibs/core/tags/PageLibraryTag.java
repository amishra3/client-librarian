/**
 * Copyright 2014, CITYTECH, Inc.
 * All rights reserved - Do Not Redistribute
 * Confidential and Proprietary
 */
package com.citytechinc.cq.clientlibs.core.tags;

import com.citytechinc.cq.clientlibs.core.servlets.ComponentClientLibraryServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.IOException;

public class PageLibraryTag extends TagSupport {

    public static final String ATTR_SLING_REQUEST = "slingRequest";

    private static final Logger LOG = LoggerFactory.getLogger(PageLibraryTag.class);

    private String type;

    @Override
    public int doEndTag() throws JspTagException {

        final SlingHttpServletRequest request = (SlingHttpServletRequest) pageContext.getAttribute(
                ATTR_SLING_REQUEST);

        try {
            if (buildJs()) {

                pageContext.getOut().write(buildJsIncludeString(request));

            }
            if (buildCss()) {

               pageContext.getOut().write(buildCssIncludeString(request));

            }
        } catch (IOException e) {
            LOG.error("error writing page library tag", e);
            throw new JspTagException(e);
        }

        return EVAL_PAGE;

    }

    private String buildJsIncludeString(SlingHttpServletRequest request) {

        StringBuilder jsOutputBuilder = new StringBuilder();

        jsOutputBuilder.append("<script type=\"text/javascript\" src=\"");
        jsOutputBuilder.append(getIncludeFilePath(request));
        jsOutputBuilder.append(".");
        jsOutputBuilder.append(ComponentClientLibraryServlet.SELECTOR);
        jsOutputBuilder.append(".js\"></script>\n");

        return jsOutputBuilder.toString();

    }

    private String buildCssIncludeString(SlingHttpServletRequest request) {

        StringBuilder cssOutputBuilder = new StringBuilder();

        cssOutputBuilder.append("<link ");
        cssOutputBuilder.append("href=\"");
        cssOutputBuilder.append(getIncludeFilePath(request));
        cssOutputBuilder.append(".");
        cssOutputBuilder.append(ComponentClientLibraryServlet.SELECTOR);
        cssOutputBuilder.append(".css\" ");
        cssOutputBuilder.append("rel=\"stylesheet\" ");
        cssOutputBuilder.append("type=\"text/css\"");
        cssOutputBuilder.append("/>\n");

        return cssOutputBuilder.toString();

    }

    /*
     * TODO: Clean this up
     */
    private String getIncludeFilePath(SlingHttpServletRequest request) {
        Resource resource = request.getResource();
        if (resource.getName().equals("jcr:content")) {
            return request.getResourceResolver().map(resource.getParent().getPath());
        }
        return request.getResourceResolver().map(resource.getPath());
    }

    private Boolean buildJs() {
        return type.equals("js") || type.equals("both") || type == null;
    }

    private Boolean buildCss() {
        return type.equals("css") || type.equals("both") || type == null;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
