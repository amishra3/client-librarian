/**
 * Copyright 2014, CITYTECH, Inc.
 * All rights reserved - Do Not Redistribute
 * Confidential and Proprietary
 */
package com.citytechinc.cq.clientlibs.services.clientlibs.compilers.less;

import com.citytechinc.cq.clientlibs.services.clientlibs.compilers.less.exceptions.LessCompilationException;

import java.io.IOException;

public interface LessCompiler {

    public String compile(String source) throws LessCompilationException;

}
