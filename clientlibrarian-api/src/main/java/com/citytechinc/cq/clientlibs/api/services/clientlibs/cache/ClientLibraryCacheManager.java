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
package com.citytechinc.cq.clientlibs.api.services.clientlibs.cache;

import com.citytechinc.cq.clientlibs.api.domain.library.LibraryType;
import com.citytechinc.cq.clientlibs.api.services.clientlibs.exceptions.CachedClientLibraryLookupException;
import com.citytechinc.cq.clientlibs.api.services.clientlibs.exceptions.ClientLibraryCachingException;
import com.google.common.base.Optional;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;

/**
 * A management interface for the library cache of the Client Librarian.  Individual libraries may
 * be cached for ease of retrieval later.  As content or individual libraries change the related caches
 * are cleared.
 */
public interface ClientLibraryCacheManager {

    public Optional<String> getCachedLibrary(Resource root, LibraryType type) throws CachedClientLibraryLookupException, LoginException;

    public Optional<String> getCachedLibrary(Resource root, LibraryType type, String brand) throws CachedClientLibraryLookupException, LoginException;

    public void cacheLibrary(Resource root, LibraryType type, String libraryContent) throws ClientLibraryCachingException;

    public void cacheLibrary(Resource root, LibraryType type, String brand, String libraryContent) throws ClientLibraryCachingException;

    public void invalidateCache(String rootPath) throws ClientLibraryCachingException;

    public void invalidateCache(String rootPath, LibraryType type) throws ClientLibraryCachingException;

    public void invalidateCache(String rootPath, LibraryType type, String brand) throws ClientLibraryCachingException;

    public void clearCache() throws ClientLibraryCachingException;
}
