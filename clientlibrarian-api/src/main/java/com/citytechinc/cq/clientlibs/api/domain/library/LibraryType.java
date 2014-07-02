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
package com.citytechinc.cq.clientlibs.api.domain.library;

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