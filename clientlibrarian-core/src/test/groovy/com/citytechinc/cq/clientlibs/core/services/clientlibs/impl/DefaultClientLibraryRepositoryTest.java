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
package com.citytechinc.cq.clientlibs.core.services.clientlibs.impl;

import com.citytechinc.cq.clientlibs.api.services.clientlibs.ResourceDependencyProvider;
import com.citytechinc.cq.clientlibs.api.services.clientlibs.transformer.VariableProvider;
import com.citytechinc.cq.clientlibs.core.services.clientlibs.state.manager.impl.ClientLibraryRepositoryStateManager;
import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DefaultClientLibraryRepositoryTest extends BaseConcurrencyTest {

    // The interesting methods are overridden to simulate load/latency
    //
    // Writes are slow
    // Reads are set to be 5 x faster than writes
    //
    List<ResourceDependencyProvider> slowResourceDependencyProviderList;
    List<VariableProvider> slowVariableProviderList;


    // This is a mock list that a test can use to verify interaction or mock interactions
    // The 'slowResourceDependencyProviderList' delegates list operations to this mock.
    //
    @Mock
    List<ResourceDependencyProvider> mockResourceDependencyProviderListInternal;

    // The 'slowVariableProviderList' delegates list operations to this mock.
    //
    @Mock
    List<ResourceDependencyProvider> mockVariableProviderListInternal;

    @Mock
    ClientLibraryRepositoryStateManager clientLibraryRepositoryStateManager;

    @Before
    public void before() {
//        slowResourceDependencyProviderList = new SlowList<ResourceDependencyProvider>(2, 10, mockResourceDependencyProviderListInternal);
//        slowVariableProviderList = new SlowList<VariableProvider>(2, 10, mockVariableProviderListInternal);
        slowResourceDependencyProviderList = new SlowList<ResourceDependencyProvider>(0, 0, mockResourceDependencyProviderListInternal);
        slowVariableProviderList = new SlowList<VariableProvider>(0, 0, mockVariableProviderListInternal);
    }

    @Test
    public void should() throws Exception {
        final ReentrantReadWriteLock resourceDependencyProviderListReadWriteLock = spy(new ReentrantReadWriteLock(false));
        final ReentrantReadWriteLock variableProviderListReadWriteLock = spy(new ReentrantReadWriteLock(false));

        final DefaultClientLibraryRepository defaultClientLibraryRepository =
            new DefaultClientLibraryRepository(
                    slowResourceDependencyProviderList,
                    slowVariableProviderList,
                    clientLibraryRepositoryStateManager,
                    resourceDependencyProviderListReadWriteLock,
                    variableProviderListReadWriteLock);

        final ResourceDependencyProvider bindResourceDependencyProvider = mock(ResourceDependencyProvider.class);
        final ResourceDependencyProvider unbindResourceDependencyProvider = mock(ResourceDependencyProvider.class);

        final VariableProvider bindVariableProvider = mock(VariableProvider.class);
        final VariableProvider unbindVariableProvider = mock(VariableProvider.class);

        final Resource resource = mock(Resource.class);
        final String library = "library";


        // If we are binding, then report 'false' to cause an 'add'
        when(mockResourceDependencyProviderListInternal.contains(bindResourceDependencyProvider))
            .thenReturn(false);

        when(mockVariableProviderListInternal.contains(bindVariableProvider))
            .thenReturn(false);

        // If we are unbinding, then report 'true' to cause 'remove'
        when(mockResourceDependencyProviderListInternal.contains(unbindResourceDependencyProvider))
            .thenReturn(true);

        when(mockVariableProviderListInternal.contains(unbindVariableProvider))
            .thenReturn(true);


        super.split(new NamedRunnable("bindDependencyProvider") {
            @Override
            public void run() {
                defaultClientLibraryRepository.bindDependencyProvider(bindResourceDependencyProvider);
            }
        }, new NamedRunnable("unbindDependencyProvider") {
            @Override
            public void run() {
                defaultClientLibraryRepository.unbindDependencyProvider(unbindResourceDependencyProvider);
            }
        }, new NamedRunnable("getDependencyGraph") {
            @Override
            public void run() {
                defaultClientLibraryRepository.getDependencyGraph(resource);
            }
        }, new NamedRunnable("getOrderedDependencies") {
            @Override
            public void run() {
                try {
                    defaultClientLibraryRepository.getOrderedDependencies(resource);
                } catch (Exception e) {
                }
            }
        }, new NamedRunnable("transformLibrary") {
            @Override
            public void run() {
                try {
                    defaultClientLibraryRepository.transformLibrary(resource, library);
                } catch (Exception e) {
                }
            }
        }, new NamedRunnable("bindVariableProvider") {
            @Override
            public void run() {
                try {
                    defaultClientLibraryRepository.bindVariableProvider(bindVariableProvider);
                } catch (Exception e) {
                }
            }
        }, new NamedRunnable("unbindVariableProvider") {
            @Override
            public void run() {
                try {
                    defaultClientLibraryRepository.unbindVariableProvider(unbindVariableProvider);
                } catch (Exception e) {
                }
            }
        })
        .execute();
    }



}
