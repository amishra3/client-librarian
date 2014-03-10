/**
 * Copyright 2014, CITYTECH, Inc.
 * All rights reserved - Do Not Redistribute
 * Confidential and Proprietary
 */
package com.citytechinc.cq.clientlibs.core.structures.graph.dag

class VisitableNode {

    def payload
    private def visiting = false
    private def visited = false

    def visit() {
        visiting = true
        payload
    }

    def leave() {
        visiting= false
        visited = true
    }

    def getVisiting() {
        visiting
    }

    def getVisited() {
        visited
    }

}
