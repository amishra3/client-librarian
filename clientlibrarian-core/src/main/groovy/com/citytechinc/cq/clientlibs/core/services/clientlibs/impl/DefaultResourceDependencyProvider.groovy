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
import com.citytechinc.cq.clientlibs.api.domain.component.EmbeddedComponent
import com.citytechinc.cq.clientlibs.api.domain.library.ClientLibrary
import com.citytechinc.cq.clientlibs.api.services.clientlibs.ClientLibraryManager
import com.citytechinc.cq.clientlibs.api.services.clientlibs.ResourceDependencyProvider
import com.citytechinc.cq.clientlibs.api.services.clientlibs.ResourceProviderHelper
import com.citytechinc.cq.clientlibs.api.services.clientlibs.exceptions.ClientLibraryCompilationException
import com.citytechinc.cq.clientlibs.api.services.components.DependentComponentManager
import com.citytechinc.cq.clientlibs.api.util.ComponentUtils
import com.google.common.collect.Queues
import com.google.common.collect.Sets
import org.apache.felix.scr.annotations.Component
import org.apache.felix.scr.annotations.ReferenceCardinality
import org.apache.felix.scr.annotations.ReferencePolicy
import org.apache.felix.scr.annotations.Service
import org.apache.sling.api.resource.Resource
import org.apache.sling.api.resource.SyntheticResource
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

    @org.apache.felix.scr.annotations.Reference( cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC, bind = "bindResourceProviderHelper", unbind = "unbindResourceProviderHelper", referenceInterface = ResourceProviderHelper )
    private final Map<String, Set<ResourceProviderHelper>> resourceProviderHelperByResourceTypeMap = [:]

    @Override
    Set<ClientLibrary> getDependenciesForResource(Resource r) throws ClientLibraryCompilationException {

        Map<String, Set<ClientLibrary>> clientLibrariesByCategoryMap = clientLibraryManager.librariesByCategory
        Map<String, DependentComponent> dependentComponentsByComponentPathMap = dependentComponentManager.componentsByPath
        String[] searchPaths = r.resourceResolver.searchPath

        Set<Resource> flattenedResourceTree = ComponentUtils.flattenResourceTree(r, true)
        Set<ClientLibrary> dependencies = Sets.newHashSet()
        Set<String> visitedResourceTypes = Sets.newHashSet()

        Map<String, Resource> flattenedResourcesByPath = [:]
        flattenedResourceTree.each { flattenedResourcesByPath.put( it.path, it ) }

        Queue resourceProcessingQueue = Queues.newArrayDeque(flattenedResourceTree)

        while (!resourceProcessingQueue.isEmpty()) {
            Resource currentResourceUnderProcessing = resourceProcessingQueue.remove()

            if (visitedResourceTypes.add(currentResourceUnderProcessing.resourceType)) {
                String fullComponentPath = getFullPathToComponentForResourceType(currentResourceUnderProcessing.resourceType, searchPaths, dependentComponentsByComponentPathMap.keySet())

                if (fullComponentPath == null) {
                    LOG.debug("No full component path was found for component type " + currentResourceUnderProcessing.resourceType)
                }
                else {
                    DependentComponent currentDependentComponent = dependentComponentsByComponentPathMap.get(fullComponentPath)

                    currentDependentComponent.dependencies.each { String currentDependency ->
                        if (!clientLibrariesByCategoryMap.containsKey(currentDependency)) {
                            throw new ClientLibraryCompilationException("Component " + currentResourceUnderProcessing.resourceType + " indicates it is dependent on client library " + currentDependency + " however no Client Libraries answer to that name")
                        }

                        dependencies.addAll(clientLibrariesByCategoryMap.get(currentDependency))
                    }

                    /*
                     * For every component indicated as an embedded component - if we are not already going to deal with the component in
                     * the context of the calculated content tree then add the resource to the queue for processing
                     */
                    currentDependentComponent.embeddedComponents.each { EmbeddedComponent currentEmbeddedComponent ->
                        if (!flattenedResourcesByPath.containsKey(currentResourceUnderProcessing.path + "/" + currentEmbeddedComponent.relativePath)) {
                            Resource embeddedResource = currentResourceUnderProcessing.getChild(currentEmbeddedComponent.relativePath)

                            if (embeddedResource == null) {
                                embeddedResource = new SyntheticResource(currentResourceUnderProcessing.resourceResolver, currentResourceUnderProcessing.path + "/" + currentEmbeddedComponent.relativePath, currentEmbeddedComponent.resourceType)
                            }

                            resourceProcessingQueue.add(embeddedResource)
                            flattenedResourcesByPath.put(embeddedResource.path, embeddedResource)
                        }
                    }
                }
            }

            //TODO: Switch to checking is resource type on the resource itself
            Set<ResourceProviderHelper> helpersForType = resourceProviderHelperByResourceTypeMap.get(currentResourceUnderProcessing.resourceType)

            if (helpersForType != null) {
                helpersForType.each{ ResourceProviderHelper currentHelper ->
                    currentHelper.getContainedResources(currentResourceUnderProcessing).each { Resource currentContainedResource ->
                        if (!flattenedResourcesByPath.containsKey(currentContainedResource.path)) {
                            resourceProcessingQueue.add(currentContainedResource)
                            flattenedResourcesByPath.put(currentContainedResource.path, currentContainedResource)
                        }
                    }
                }
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

    protected void bindResourceProviderHelper(ResourceProviderHelper resourceProviderHelper) {

        LOG.debug("Binding ResourceProviderHelper " + resourceProviderHelper)

        synchronized (resourceProviderHelperByResourceTypeMap) {

            resourceProviderHelper.resourceTypesServed.each {
                if (!resourceProviderHelperByResourceTypeMap.containsKey(it)) {
                    resourceProviderHelperByResourceTypeMap.put(it, [])
                }

                resourceProviderHelperByResourceTypeMap.get(it).add(resourceProviderHelper)
            }

        }

    }

    protected void unbindResourceProviderHelper(ResourceProviderHelper resourceProviderHelper) {

        LOG.debug("Unbinding ResourceProviderHelper " + resourceProviderHelper)

        synchronized (resourceProviderHelperByResourceTypeMap) {

            resourceProviderHelper.resourceTypesServed.each {

                if (resourceProviderHelperByResourceTypeMap.containsKey(it)) {
                    resourceProviderHelperByResourceTypeMap.get(it).remove(resourceProviderHelper)
                }

            }

        }

    }

}
