package com.citytechinc.cq.clientlibs.api.services.clientlibs;

import com.citytechinc.cq.clientlibs.api.domain.library.ClientLibrary;

import java.util.Map;
import java.util.Set;

public interface ClientLibraryManager {

    public Set<ClientLibrary> getLibrariesForCategory(String category);
    public Map<String, Set<ClientLibrary>> getLibrariesByCategory();
    public Integer getClientLibraryCount();

    public void requestRefresh();

}
