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
package com.citytechinc.cq.clientlibs.core.services.clientlibs.cache.impl;

import com.citytechinc.cq.clientlibs.api.constants.Brands;
import com.citytechinc.cq.clientlibs.api.domain.library.ClientLibrary;
import com.citytechinc.cq.clientlibs.api.domain.library.LibraryType;
import com.citytechinc.cq.clientlibs.api.services.clientlibs.cache.ClientLibraryCacheManager;
import com.citytechinc.cq.clientlibs.api.services.clientlibs.exceptions.CachedClientLibraryLookupException;
import com.citytechinc.cq.clientlibs.api.services.clientlibs.exceptions.ClientLibraryCachingException;
import com.google.common.base.Optional;
import com.google.common.io.CharStreams;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.resource.*;
import org.apache.sling.jcr.resource.JcrResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import java.io.*;
import java.nio.charset.StandardCharsets;

@Component
@Service
public class DefaultClientLibraryCacheManager implements ClientLibraryCacheManager {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultClientLibraryCacheManager.class);

    private ResourceResolver resourceResolver;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Override
    public Optional<String> getCachedLibrary(Resource root, LibraryType type) throws CachedClientLibraryLookupException {
        return getCachedLibrary(root, type, Brands.DEFAULT_BRAND);
    }

    @Override
    public Optional<String> getCachedLibrary(Resource root, LibraryType type, String brand) throws CachedClientLibraryLookupException {

        Resource cachedResource = root.getResourceResolver().getResource(getPathToLibraryFile(root.getPath(), type, brand));

        if (cachedResource != null) {

            Node node = cachedResource.adaptTo(Node.class);

            try {

                Property dataProperty = node.getProperty( "jcr:content/jcr:data" );

                Reader libraryReader = new InputStreamReader(dataProperty.getBinary().getStream(), ClientLibrary.UTF_8_ENCODING);

                String libraryString = CharStreams.toString(libraryReader);

                libraryReader.close();

                return Optional.of(libraryString);

            } catch (RepositoryException e) {
                LOG.error("Repository Exception encountered looking up cached library for " + cachedResource.getPath(), e);
                throw new CachedClientLibraryLookupException("Repository Exception encountered looking up cached library for " + cachedResource.getPath(), e);
            } catch (IOException e) {
                LOG.error("IO Exception encountered looking up cached library for " + cachedResource.getPath(), e);
                throw new CachedClientLibraryLookupException("IO Exception encountered looking up cached library for " + cachedResource.getPath(), e);
            }
        }

        return Optional.absent();

    }

    @Override
    public void cacheLibrary(Resource root, LibraryType type, String libraryContent) throws ClientLibraryCachingException {
        cacheLibrary(root, type, Brands.DEFAULT_BRAND, libraryContent);
    }

    @Override
    public void cacheLibrary(Resource root, LibraryType type, String brand, String libraryContent) throws ClientLibraryCachingException {

        try {

            Resource cachedResourceFolder = getOrCreateCachedLibraryFolderResource(root, type, brand);

            InputStream stream = new ByteArrayInputStream(libraryContent.getBytes(StandardCharsets.UTF_8));

            JcrUtils.putFile(
                    cachedResourceFolder.adaptTo(Node.class),
                    "library",
                    "application/javascript",
                    stream
            );

            getAdministrativeResourceResolver().commit();

        } catch (LoginException e) {
            LOG.error("Login Exception encountered attempting to Cache Library " + root.getPath());
            throw new ClientLibraryCachingException("Login Exception encountered attempting to Cache Library " + root.getPath(), e);
        } catch (RepositoryException e) {
            LOG.error("Repository Exception encountered attempting to Cache Library " + root.getPath());
            throw new ClientLibraryCachingException("Repository Exception encountered attempting to Cache Library " + root.getPath(), e);
        } catch (PersistenceException e) {
            LOG.error("Persistence Exception encountered attempting to save the cached file for " + root.getPath());
            throw new ClientLibraryCachingException("Persistence Exception encountered attempting to save the cached file for " + root.getPath(), e);
        }

    }

    @Override
    public void invalidateCache(String rootPath) throws ClientLibraryCachingException {

        LOG.debug("Invalidating Cache Set for " + rootPath);

        try {
            Resource cachedLibraryResource = getAdministrativeResourceResolver().getResource(getPathToLibrarySet(rootPath));

            LOG.debug("Looking up current cache at " + getPathToLibrarySet(rootPath));

            if (cachedLibraryResource != null) {
                getAdministrativeResourceResolver().delete(cachedLibraryResource);
                getAdministrativeResourceResolver().commit();
            }
        } catch (LoginException e) {
            LOG.error("Login Exception encountered attempting to invalidate cache for page libraries " + rootPath, e);
            throw new ClientLibraryCachingException("Login Exception encountered attempting to invalidate cache for page libraries " + rootPath, e);
        } catch (PersistenceException e) {
            LOG.error("Persistence Exception encountered attempting to invalidate cache for page libraries " + rootPath, e);
            throw new ClientLibraryCachingException("Persistence Exception encountered attempting to invalidate cache for page libraries " + rootPath, e);
        }

    }

    @Override
    public void invalidateCache(String rootPath, LibraryType type) throws ClientLibraryCachingException {
        invalidateCache(rootPath, type, Brands.DEFAULT_BRAND);
    }

    @Override
    public void invalidateCache(String rootPath, LibraryType type, String brand) throws ClientLibraryCachingException {

        try {
            Resource libraryResource = getAdministrativeResourceResolver().getResource(getPathToLibrary(rootPath, type, brand));

            if (libraryResource != null) {

                getAdministrativeResourceResolver().delete(libraryResource);

                getAdministrativeResourceResolver().commit();

            }
        } catch (LoginException e) {

            LOG.error("Login Exception encountered invalidating cached library " + rootPath);
            throw new ClientLibraryCachingException("Login Exception encountered invalidating cached library " + rootPath, e);

        } catch (PersistenceException e) {

            LOG.error("Persistence Exception encountered invalidating cached library " + rootPath);
            throw new ClientLibraryCachingException("Persistence Exception encountered invalidating cached library " + rootPath, e);

        }

    }

    public void clearCache() throws ClientLibraryCachingException {

        try {

            Resource cacheRoot = getAdministrativeResourceResolver().getResource("/var/clientlibrarian");

            if (cacheRoot != null) {

                getAdministrativeResourceResolver().delete(cacheRoot);

                getAdministrativeResourceResolver().commit();

            }

        }
        catch (PersistenceException e) {
            LOG.error("Persistence Exception encountered clearing the Cache");
            throw new ClientLibraryCachingException("Persistence Exception encountered clearing the Cache", e);
        } catch (LoginException e) {
            LOG.error("Login Exception encountered clearing the Cache");
            throw new ClientLibraryCachingException("Login Exception encountered clearing the Cache", e);
        }
    }

    protected final ResourceResolver getAdministrativeResourceResolver() throws LoginException {
        if (resourceResolver == null) {
            resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
        }

        return resourceResolver;
    }

    private Resource getOrCreateCachedLibraryFolderResource(Resource root, LibraryType type, String brand) throws LoginException, RepositoryException {

        String pathToLibrary = getPathToLibrary(root.getPath(), type, brand);

        Resource cachedLibraryResource = getAdministrativeResourceResolver().getResource(pathToLibrary);

        if (cachedLibraryResource != null) {
            return cachedLibraryResource;
        }

        Resource varResource = getAdministrativeResourceResolver().getResource("/var");

        JcrResourceUtil.createPath(
                varResource.adaptTo(Node.class),
                getPathToLibraryRelativeToVar(root.getPath(), type, brand),
                "sling:Folder",
                "sling:Folder",
                true);

        return getAdministrativeResourceResolver().getResource(pathToLibrary);

    }

    /**
     * The Path to the Library Set will be /var/clientlibrarian/[path-of-root].
     *
     */
    private static String getPathToLibrarySet(String rootPath) {
        return "/var/" + getPathToLibrarySetRelativeToVar(rootPath);
    }

    private static String getPathToLibrarySetRelativeToVar(String rootPath) {
        return "clientlibrarian" + rootPath;
    }

    private static String getPathToLibrary(String rootPath, LibraryType type, String brand) {
        return "/var/" + getPathToLibraryRelativeToVar(rootPath, type, brand);
    }

    private static String getPathToLibraryRelativeToVar(String rootPath, LibraryType type, String brand) {
        return "clientlibrarian" + rootPath + "/" + brand + "/" + type;
    }

    private static String getPathToLibraryFile(String rootPath, LibraryType type, String brand) {
        return getPathToLibrary(rootPath, type, brand) + "/library";
    }

}
