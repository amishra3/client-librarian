/**
 * Copyright 2014, CITYTECH, Inc.
 * All rights reserved - Do Not Redistribute
 * Confidential and Proprietary
 */
package com.citytechinc.cq.clientlibs.core.jmx;

import javax.jcr.RepositoryException;

import com.citytechinc.cq.clientlibs.api.jmx.ClientLibraryRepositoryReportingAndMaintenanceMBean;
import com.citytechinc.cq.clientlibs.api.services.clientlibs.ClientLibraryRepository;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component(immediate = true)
@Property(name = "jmx.objectname", value = "com.citytechinc.cq.library:type=Client Library Repository Reporting and Maintenance")
@Service
public class DefaultClientLibraryRepositoryReportingAndMaintenanceMBean implements
        ClientLibraryRepositoryReportingAndMaintenanceMBean {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultClientLibraryRepositoryReportingAndMaintenanceMBean.class);

    @Reference
    ClientLibraryRepository repository;

    @Override
    public void refresh() {
        try {
            repository.refresh();
        } catch (RepositoryException e) {
            LOG.error("Error refreshing repository", e);
        } catch (LoginException e) {
            LOG.error("Error refreshing repository", e);
        }
    }

    @Override
    public Integer getClientLibraryCount() {
        return repository.getClientLibraryCount();
    }

}
