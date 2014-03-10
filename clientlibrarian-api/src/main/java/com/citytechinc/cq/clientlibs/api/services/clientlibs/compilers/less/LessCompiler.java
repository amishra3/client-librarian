/**
 * Copyright 2014, CITYTECH, Inc.
 * All rights reserved - Do Not Redistribute
 * Confidential and Proprietary
 */
package com.citytechinc.cq.clientlibs.api.services.clientlibs.compilers.less;

import com.citytechinc.cq.clientlibs.api.services.clientlibs.compilers.less.exceptions.LessCompilationException;

public interface LessCompiler {

    public String compile(String source) throws LessCompilationException;

}
