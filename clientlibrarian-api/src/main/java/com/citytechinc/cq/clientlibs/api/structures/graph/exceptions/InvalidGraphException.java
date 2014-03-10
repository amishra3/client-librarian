package com.citytechinc.cq.clientlibs.api.structures.graph.exceptions;

public class InvalidGraphException extends Exception {

    public InvalidGraphException(String m) {
        super(m);
    }

    public InvalidGraphException(String m, Throwable e) {
        super(m, e);
    }

}
