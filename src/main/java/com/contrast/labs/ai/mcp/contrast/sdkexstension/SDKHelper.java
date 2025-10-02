/*
 * Copyright 2025 Contrast Security
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
package com.contrast.labs.ai.mcp.contrast.sdkexstension;

import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.LibraryExtended;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.application.Application;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.sca.LibraryObservation;
import com.contrastsecurity.sdk.ContrastSDK;
import com.contrastsecurity.sdk.UserAgentProduct;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.LibrariesExtended;
import com.contrastsecurity.http.LibraryFilterForm;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import com.contrastsecurity.exceptions.UnauthorizedException;

@Component
public class SDKHelper {

    private static final String MCP_SERVER_NAME = "contrast-mcp";

    private static final Logger logger = LoggerFactory.getLogger(SDKHelper.class);

    private static Environment environment;

    @Autowired
    public void setEnvironment(Environment environment) {
        SDKHelper.environment = environment;
    }

    private static final Cache<String, List<LibraryExtended>> libraryCache = CacheBuilder.newBuilder()
            .maximumSize(500000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();
            
    private static final Cache<String, List<LibraryObservation>> libraryObservationsCache = CacheBuilder.newBuilder()
            .maximumSize(500000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    private static final Cache<String, List<Application>> applicationsCache = CacheBuilder.newBuilder()
            .maximumSize(500000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    public static List<LibraryExtended> getLibsForID(String appID, String orgID, SDKExtension extendedSDK) throws IOException {
        // Check cache for existing result
        List<LibraryExtended> cachedLibraries = libraryCache.getIfPresent(appID);
        if (cachedLibraries != null) {
            logger.info("Cache hit for appID: {}", appID);
            return cachedLibraries;
        }
        logger.info("Cache miss for appID: {}, fetching libraries from SDK", appID);
        int libraryCallSize = 50;
        LibraryFilterForm filterForm = new LibraryFilterForm();
        filterForm.setLimit(libraryCallSize);
        filterForm.setExpand(EnumSet.of(LibraryFilterForm.LibrariesExpandValues.VULNS));
        LibrariesExtended libraries = extendedSDK.getLibrariesWithFilter(orgID, appID, filterForm);
        List<LibraryExtended> libs = new ArrayList<>();
        libs.addAll(libraries.getLibraries());
        int offset = libraryCallSize;
        while (libraries.getLibraries().size() == libraryCallSize) {
            logger.debug("Retrieved {} libraries, fetching more with offset: {}", libraryCallSize, offset);
            filterForm.setOffset(offset);
            libraries = extendedSDK.getLibrariesWithFilter(orgID, appID, filterForm);
            libs.addAll(libraries.getLibraries());
            offset += libraryCallSize;
        }

        logger.info("Successfully retrieved {} libraries for application id: {}", libs.size(), appID);

        // Store result in cache
        libraryCache.put(appID, libs);

        return libs;
    }

    /**
     * Retrieves library observations for a specific library in an application, with caching.
     * 
     * @param libraryId The library ID
     * @param appId The application ID
     * @param orgId The organization ID
     * @param pageSize The number of items per page (default 25)
     * @param extendedSDK The SDK extension instance
     * @return List of library observations
     * @throws IOException If an I/O error occurs
     * @throws UnauthorizedException If the request is not authorized
     */
    public static List<LibraryObservation> getLibraryObservationsWithCache(
            String libraryId, String appId, String orgId, int pageSize, SDKExtension extendedSDK) 
            throws IOException, UnauthorizedException {
        
        // Generate cache key from combination of library, app and org IDs
        String cacheKey = String.format("%s:%s:%s", orgId, appId, libraryId);
        
        // Check cache for existing result
        List<LibraryObservation> cachedObservations = libraryObservationsCache.getIfPresent(cacheKey);
        if (cachedObservations != null) {
            logger.info("Cache hit for library observations: {}", cacheKey);
            return cachedObservations;
        }
        
        logger.info("Cache miss for library observations: {}, fetching from API", cacheKey);
        List<LibraryObservation> observations = extendedSDK.getLibraryObservations(orgId, appId, libraryId, pageSize);
        
        logger.info("Successfully retrieved {} library observations for library: {} in app: {}", 
                observations.size(), libraryId, appId);
        
        // Store result in cache
        libraryObservationsCache.put(cacheKey, observations);
        
        return observations;
    }


    
    /**
     * Retrieves library observations with default page size of 25.
     */
    public static List<LibraryObservation> getLibraryObservationsWithCache(
            String libraryId, String appId, String orgId, SDKExtension extendedSDK) 
            throws IOException, UnauthorizedException {
        return getLibraryObservationsWithCache(libraryId, appId, orgId, 25, extendedSDK);
    }

    // The withUserAgentProduct will generate a user agent header that looks like
    // User-Agent: contrast-mcp/1.0 contrast-sdk-java/3.4.2 Java/19.0.2+7
    public static ContrastSDK getSDK(String hostName, String apiKey, String serviceKey, String userName, String httpProxyHost, String httpProxyPort)  {
        logger.info("Initializing ContrastSDK with username: {}, host: {}", userName, hostName);

        String mcpVersion = SDKHelper.environment.getProperty("spring.ai.mcp.server.version", "unknown");
        ContrastSDK.Builder builder = new ContrastSDK.Builder(userName, serviceKey, apiKey)
                .withApiUrl(SDKHelper.environment.getProperty("contrast.api.protocol", "https") + "://" + hostName + "/Contrast/api")
                .withUserAgentProduct(UserAgentProduct.of(MCP_SERVER_NAME, mcpVersion));

        if (httpProxyHost != null && !httpProxyHost.isEmpty()) {
            int port = httpProxyPort != null && !httpProxyPort.isEmpty() ? Integer.parseInt(httpProxyPort) : 80;
            logger.debug("Configuring HTTP proxy: {}:{}", httpProxyHost, port);

            java.net.Proxy proxy = new java.net.Proxy(
                java.net.Proxy.Type.HTTP,
                new java.net.InetSocketAddress(httpProxyHost, port)
            );

            builder.withProxy(proxy);
        }

        return builder.build();
    }

    public static Optional<Application> getApplicationByName(String appName, String orgId, ContrastSDK contrastSDK) throws IOException {
        logger.debug("Searching for application by name: {}", appName);
        for (Application app : getApplicationsWithCache(orgId, contrastSDK)) {
            if (app.getName().equalsIgnoreCase(appName)) {
                logger.info("Found application - ID: {}, Name: {}", app.getAppId(), app.getName());
                return Optional.of(app);
            }
        }

        logger.warn("No application found with name: {}, clearing cache and retrying", appName);
        clearApplicationsCache();
        for (Application app : getApplicationsWithCache(orgId, contrastSDK)) {
            if (app.getName().equalsIgnoreCase(appName)) {
                logger.info("Found application after cache clear - ID: {}, Name: {}", app.getAppId(), app.getName());
                return Optional.of(app);
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieves all applications from Contrast with caching.
     *
     * @param orgId The organization ID
     * @param contrastSDK The Contrast SDK instance
     * @return List of applications
     * @throws IOException If an I/O error occurs
     */
    public static List<Application> getApplicationsWithCache(String orgId, ContrastSDK contrastSDK) throws IOException {
        // Generate cache key based on organization ID
        String cacheKey = String.format("applications:%s", orgId);

        // Check cache for existing result
        List<Application> cachedApplications = applicationsCache.getIfPresent(cacheKey);
        if (cachedApplications != null) {
            logger.info("Cache hit for applications in org: {}", orgId);
            return cachedApplications;
        }

        logger.info("Cache miss for applications in org: {}, fetching from API", orgId);
        List<com.contrast.labs.ai.mcp.contrast.sdkexstension.data.application.Application> applications = new SDKExtension(contrastSDK).getApplications(orgId).getApplications();
        logger.info("Successfully retrieved {} applications from organization: {}",
                applications.size(), orgId);

        // Store result in cache
        applicationsCache.put(cacheKey, applications);

        return applications;
    }

    /**
     * Clears the libraries cache.
     *
     * @return The number of entries cleared
     */
    public static long clearLibraryCache() {
        long size = libraryCache.size();
        libraryCache.invalidateAll();
        libraryCache.cleanUp();
        logger.info("Cleared {} entries from library cache", size);
        return size;
    }

    /**
     * Clears the library observations cache.
     *
     * @return The number of entries cleared
     */
    public static long clearLibraryObservationsCache() {
        long size = libraryObservationsCache.size();
        libraryObservationsCache.invalidateAll();
        libraryObservationsCache.cleanUp();
        logger.info("Cleared {} entries from library observations cache", size);
        return size;
    }

    /**
     * Clears the applications cache.
     *
     * @return The number of entries cleared
     */
    public static long clearApplicationsCache() {
        long size = applicationsCache.size();
        applicationsCache.invalidateAll();
        applicationsCache.cleanUp();
        logger.info("Cleared {} entries from applications cache", size);
        return size;
    }

    /**
     * Clears all caches maintained by the SDKHelper.
     *
     * @return Total number of entries cleared across all caches
     */
    public static long clearAllCaches() {
        long libraryEntries = clearLibraryCache();
        long observationsEntries = clearLibraryObservationsCache();
        long applicationsEntries = clearApplicationsCache();

        long totalCleared = libraryEntries + observationsEntries + applicationsEntries;
        logger.info("Cleared a total of {} entries from all caches", totalCleared);

        return totalCleared;
    }
}