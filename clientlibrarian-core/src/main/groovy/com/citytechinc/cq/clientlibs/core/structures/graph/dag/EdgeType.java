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
