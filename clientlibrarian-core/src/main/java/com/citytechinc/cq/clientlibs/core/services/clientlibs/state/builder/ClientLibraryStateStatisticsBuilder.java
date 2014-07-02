/**
 * Copyright 2014 CITYTECH, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.citytechinc.cq.clientlibs.core.services.clientlibs.state.builder;

import com.citytechinc.cq.clientlibs.api.services.clientlibs.state.ClientLibraryStateStatistics;
import com.citytechinc.cq.clientlibs.core.services.clientlibs.state.impl.DefaultClientLibraryStateStatistics;

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
