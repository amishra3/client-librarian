package com.citytechinc.cq.clientlibs.services.clientlibs.compilers.less.exceptions;

public class LessCompilationException extends Exception {

    public LessCompilationException(String m) {
        super(m);
    }

    public LessCompilationException(String m, Throwable e) {
        super(m, e);
    }

}
