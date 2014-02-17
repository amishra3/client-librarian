package com.citytechinc.cq.clientlibs.services.clientlibs.state.builder;

import com.citytechinc.cq.clientlibs.services.clientlibs.state.ClientLibraryStateStatistics;
import com.citytechinc.cq.clientlibs.services.clientlibs.state.impl.DefaultClientLibraryStateStatistics;

public class ClientLibraryStateStatisticsBuilder {

    private static final ClientLibraryStateStatisticsBuilder instance = new ClientLibraryStateStatisticsBuilder();

    private Integer clientLibraryCount;

    private ClientLibraryStateStatisticsBuilder() {
        refresh();
    }

    private void refresh() {
        clientLibraryCount = 0;
    }

    public static ClientLibraryStateStatisticsBuilder getBuilder() {
        return instance;
    }

    public static ClientLibraryStateStatisticsBuilder getCleanBuilder() {
        instance.refresh();
        return instance;
    }

    public void setClientLibraryCount(Integer clientLibraryCount) {
        this.clientLibraryCount = clientLibraryCount;
    }

    public ClientLibraryStateStatistics build() {
        return new DefaultClientLibraryStateStatistics(clientLibraryCount);
    }

}
