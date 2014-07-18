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
package com.citytechinc.cq.clientlibs.core.domain.component.impl;

import com.citytechinc.cq.clientlibs.api.domain.component.EmbeddedComponent;

public class DefaultEmbeddedComponent implements EmbeddedComponent {

    private final String relativePath;
    private final String resourceType;

    public DefaultEmbeddedComponent(String relativePath, String resourceType) {
        this.relativePath = relativePath;
        this.resourceType = resourceType;
    }

    @Override
    public String getRelativePath() {
        return relativePath;
    }

    @Override
    public String getResourceType() {
        return resourceType;
    }
}
