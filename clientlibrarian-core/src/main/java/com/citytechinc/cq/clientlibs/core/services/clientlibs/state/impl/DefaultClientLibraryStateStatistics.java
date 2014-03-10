package com.citytechinc.cq.clientlibs.core.services.clientlibs.state.impl;

import com.citytechinc.cq.clientlibs.api.services.clientlibs.state.ClientLibraryStateStatistics;

public class DefaultClientLibraryStateStatistics implements ClientLibraryStateStatistics {

    private final Integer clientLibraryCount;

    public DefaultClientLibraryStateStatistics(Integer clientLibraryCount) {
        this.clientLibraryCount = clientLibraryCount;
    }

    @Override
    public Integer getClientLibraryCount() {
        return clientLibraryCount;
    }

}
