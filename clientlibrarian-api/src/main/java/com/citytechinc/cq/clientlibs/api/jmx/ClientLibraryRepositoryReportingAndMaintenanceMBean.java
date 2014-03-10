/**
 * Copyright 2014, CITYTECH, Inc.
 * All rights reserved - Do Not Redistribute
 * Confidential and Proprietary
 */
package com.citytechinc.cq.clientlibs.api.jmx;


import com.adobe.granite.jmx.annotation.Description;

public interface ClientLibraryRepositoryReportingAndMaintenanceMBean {

    @Description("Refresh the client libraries and component dependencies in the Repository.")
    void refresh();

    @Description("The count of client libraries found in the content repository.")
    Integer getClientLibraryCount();

}
