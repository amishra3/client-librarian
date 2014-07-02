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
package com.citytechinc.cq.clientlibs.core.tags;

import com.citytechinc.cq.clientlibs.core.servlets.ComponentClientLibraryServlet;
import org.apache.commons.lang3.StringUtils;
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

    private String brand;

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

        if (StringUtils.isNotBlank(brand)) {
            jsOutputBuilder.append(".");
            jsOutputBuilder.append(brand);
        }

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

        if (StringUtils.isNotBlank(brand)) {
            cssOutputBuilder.append(".");
            cssOutputBuilder.append(brand);
        }

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
        return type == null || "js".equals(type) || "both".equals(type);
    }

    private Boolean buildCss() {
        return type == null || "css".equals(type) || "both".equals(type);
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBrand() {
        return this.brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }
}
