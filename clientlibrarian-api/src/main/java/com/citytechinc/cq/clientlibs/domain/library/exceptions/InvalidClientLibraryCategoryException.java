package com.citytechinc.cq.clientlibs.domain.library.exceptions;

public class InvalidClientLibraryCategoryException extends Exception {

    public InvalidClientLibraryCategoryException(String m) {
        super(m);
    }

    public InvalidClientLibraryCategoryException(String m, Throwable e) {
        super(m, e);
    }

}
