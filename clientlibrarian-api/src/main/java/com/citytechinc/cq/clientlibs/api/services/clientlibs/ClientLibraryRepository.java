/**
 * Copyright 2014, CITYTECH, Inc.
 * All rights reserved - Do Not Redistribute
 * Confidential and Proprietary
 */
package com.citytechinc.cq.clientlibs.api.services.clientlibs;

import com.citytechinc.cq.clientlibs.api.domain.library.ClientLibrary;
import com.citytechinc.cq.clientlibs.api.domain.library.LibraryType;
import com.citytechinc.cq.clientlibs.api.services.clientlibs.exceptions.ClientLibraryCompilationException;
import com.citytechinc.cq.clientlibs.api.structures.graph.DependencyGraph;
import com.google.common.base.Optional;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;

import javax.jcr.RepositoryException;

public interface ClientLibraryRepository {

    public String compileClientLibrary(Resource root, LibraryType type, Optional<String> brand) throws ClientLibraryCompilationException;

    public DependencyGraph<ClientLibrary> getClientLibraryDependencyGraph(Resource root);

    public void refresh() throws RepositoryException, LoginException;

    public Integer getClientLibraryCount();

}
