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
import com.citytechinc.cq.clientlibs.core.services.clientlibs.state.manager.impl.ClientLibraryRepositoryStateManager;
import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultClientLibraryRepositoryTest extends BaseConcurrencyTest {

    // The interesting methods are overridden to simulate load/latency
    //
    // Writes are slow
    // Reads are set to be 5 x faster than writes
    //
    List<ResourceDependencyProvider> slowResourceDependencyProviderList;


    // This is a mock list that a test can use to verify interaction or mock interactions
    // The 'slowResourceDependencyProviderList' delegates list operations to this mock.
    //
    @Mock
    List<ResourceDependencyProvider> mockResourceDependencyProviderListInternal;

    @Mock
    ClientLibraryRepositoryStateManager clientLibraryRepositoryStateManager;

    @Before
    public void before() {
        slowResourceDependencyProviderList = new ArrayList<ResourceDependencyProvider>() {
            @Override
            public boolean contains(Object o) {
                letsSlowDown(two_seconds).on("contains");
                return mockResourceDependencyProviderListInternal.contains(o);
            }

            @Override
            public boolean add(ResourceDependencyProvider e) {
                letsSlowDown(ten_seconds).on("add");
                return mockResourceDependencyProviderListInternal.add(e);
            };

            @Override
            public boolean remove(Object o) {
                letsSlowDown(ten_seconds).on("remove");
                return mockResourceDependencyProviderListInternal.remove(o);
            }
        };
    }

    @Test
    public void should() throws Exception {
        final DefaultClientLibraryRepository defaultClientLibraryRepository =
            new DefaultClientLibraryRepository(slowResourceDependencyProviderList, clientLibraryRepositoryStateManager);

        final ResourceDependencyProvider bindResourceDependencyProvider = mock(ResourceDependencyProvider.class);
        final ResourceDependencyProvider unbindResourceDependencyProvider = mock(ResourceDependencyProvider.class);

        final Resource resource = mock(Resource.class);

        // If we are binding, then report 'false' to cause an 'add'
        when(mockResourceDependencyProviderListInternal.contains(bindResourceDependencyProvider))
            .thenReturn(false);

        // If we are unbinding, then report 'true' to cause 'remove'
        when(mockResourceDependencyProviderListInternal.contains(unbindResourceDependencyProvider))
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
        })
        .execute();
    }



}
