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
