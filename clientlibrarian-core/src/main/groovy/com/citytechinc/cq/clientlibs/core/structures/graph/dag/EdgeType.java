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
package com.citytechinc.cq.clientlibs.core.structures.graph.dag;

import org.apache.commons.lang3.StringUtils;

public enum EdgeType {

    DEPENDS_ON("dependsOn"),
    EMBEDS("embeds"),
    MEMBER_OF("memberOf");

    private String type;

    EdgeType(String type) {

        this.type = type;

    }

    public String getType() {

        return type;

    }

    public String toString() {

        return this.getType();

    }

    public static EdgeType getEdgeType(String type) {

        for(EdgeType edgeType : values()) {

            if(StringUtils.equals(type, edgeType.getType())) {

                return edgeType;

            }

        }

        throw new IllegalArgumentException();

    }

}
