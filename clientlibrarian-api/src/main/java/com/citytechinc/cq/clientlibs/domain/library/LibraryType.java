/**
 * Copyright 2014, CITYTECH, Inc.
 * All rights reserved - Do Not Redistribute
 * Confidential and Proprietary
 */
package com.citytechinc.cq.clientlibs.domain.library;

import org.apache.sling.api.SlingHttpServletRequest;

/**
 * Largely copied from com.day.cq.widget
 *
 */
public enum LibraryType
{
  JS(".js", "application/x-javascript"),

  CSS(".css", "text/css");

  public static final String JS_EXTENSION = "js";
  public static final String CSS_EXTENSION = "css";

  public final String extension;
  public final String contentType;

  private LibraryType(String extension, String contentType)
  {
    this.extension = extension;
    this.contentType = contentType;
  }

  public static LibraryType fromRequest(SlingHttpServletRequest request) {
    String ext = request.getRequestPathInfo().getExtension();

    if (ext.equals(JS_EXTENSION))
      return JS;
    if (ext.equals(CSS_EXTENSION)) {
      return CSS;
    }
    return null;
  }
}