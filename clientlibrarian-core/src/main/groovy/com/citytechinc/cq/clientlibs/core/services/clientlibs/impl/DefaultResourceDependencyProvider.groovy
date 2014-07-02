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
package com.citytechinc.cq.clientlibs.core.services.clientlibs.impl

import com.citytechinc.cq.clientlibs.api.domain.component.DependentComponent
import com.citytechinc.cq.clientlibs.api.domain.library.ClientLibrary
import com.citytechinc.cq.clientlibs.api.services.clientlibs.ClientLibraryManager
import com.citytechinc.cq.clientlibs.api.services.clientlibs.ResourceDependencyProvider
import com.citytechinc.cq.clientlibs.api.services.components.DependentComponentManager
import com.citytechinc.cq.clientlibs.api.util.ComponentUtils
import com.google.common.collect.Sets
import org.apache.felix.scr.annotations.Component
import org.apache.felix.scr.annotations.Service
import org.apache.sling.api.resource.Resource
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Component(
        label="Default Resource Dependency Provider",
        description="A dependency provider for the Client Librarian which produces a dependency set based on the dependencies declared for the resource types of the concrete resource instances found under a given resource" )
@Service
class DefaultResourceDependencyProvider implements ResourceDependencyProvider {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultResourceDependencyProvider)

    @org.apache.felix.scr.annotations.Reference
    private ClientLibraryManager clientLibraryManager

    @org.apache.felix.scr.annotations.Reference
    private DependentComponentManager dependentComponentManager

    @Override
    Set<ClientLibrary> getDependenciesForResource(Resource r) {

        Map<String, Set<ClientLibrary>> clientLibrariesByCategoryMap = clientLibraryManager.librariesByCategory
        Map<String, DependentComponent> dependentComponentsByComponentPathMap = dependentComponentManager.componentsByPath

        Set<String> componentTypes = ComponentUtils.getNestedComponentTypes(r)

        Set<ClientLibrary> dependencies = Sets.newHashSet()

        String[] searchPaths = r.resourceResolver.searchPath

        componentTypes.each { String currentComponentType ->

            String fullComponentPath = getFullPathToComponentForResourceType(currentComponentType, searchPaths, dependentComponentsByComponentPathMap.keySet())

            if (fullComponentPath == null) {
                LOG.debug("No full component path was found for component type " + currentComponentType)
                return
            }

            DependentComponent currentDependentComponent = dependentComponentsByComponentPathMap.get(fullComponentPath)

            currentDependentComponent.dependencies.each { String currentDependency ->
                if (!clientLibrariesByCategoryMap.containsKey(currentDependency)) {
                    LOG.error("Component " + currentComponentType + " indicates it is dependent on client library " + currentDependency + " however no Client Libraries answer to that name")
                    return
                }

                dependencies.addAll(clientLibrariesByCategoryMap.get(currentDependency))
            }

        }

        return dependencies

    }

    protected static String getFullPathToComponentForResourceType(String resourceType, String[] searchPaths, Set<String> componentPaths) {

        String searchPath = searchPaths.find {
            LOG.debug("Checking search path " + it + " to determine whether the full path to resource type " + resourceType + " starts with it")
            return componentPaths.contains(it + resourceType)
        }

        if (searchPath) {
            return searchPath + resourceType
        }

        return null

    }

}
