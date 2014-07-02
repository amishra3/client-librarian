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
package com.citytechinc.cq.clientlibs.api.jmx;


import com.adobe.granite.jmx.annotation.Description;

public interface ClientLibraryRepositoryReportingAndMaintenanceMBean {

    @Description("Refresh the client libraries and component dependencies in the Repository.")
    void refresh();

    @Description("The count of client libraries found in the content repository.")
    Integer getClientLibraryCount();

}
