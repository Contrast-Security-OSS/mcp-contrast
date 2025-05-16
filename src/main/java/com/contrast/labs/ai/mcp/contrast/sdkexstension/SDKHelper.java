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
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.sca.LibraryObservation;
import com.contrastsecurity.models.Application;
import com.contrastsecurity.sdk.ContrastSDK;
import com.contrastsecurity.sdk.UserAgentProduct;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.LibrariesExtended;
import com.contrastsecurity.http.LibraryFilterForm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import com.contrastsecurity.exceptions.UnauthorizedException;

public class SDKHelper {


    private static final String MCP_SERVER_NAME = "contrast-mcp";
    private static final String MCP_VERSION = "0.0.4";

    private static final Logger logger = LoggerFactory.getLogger(SDKHelper.class);

    private static final Cache<String, List<LibraryExtended>> libraryCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    private static final Cache<String, String> appIDCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();
            
    private static final Cache<String, List<LibraryObservation>> libraryObservationsCache = CacheBuilder.newBuilder()
            .maximumSize(5000)
            .expireAfterWrite(60, TimeUnit.MINUTES)
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

    public static String getAppIDFromAppName(String appName, String orgID, ContrastSDK contrastSDK) throws IOException {
        // Check cache for existing result
        String cachedAppID = appIDCache.getIfPresent(appName);
        if (cachedAppID != null) {
            logger.info("Cache hit for application name: {}", appName);
            return cachedAppID;
        }
        logger.debug("Cache miss for application name: {}, searching for application ID", appName);
        Optional<String> appID = Optional.empty();
        for (Application app : contrastSDK.getApplications(orgID).getApplications()) {
            if (app.getName().toLowerCase().contains(appName.toLowerCase())) {
                appID = Optional.of(app.getId());
                logger.info("Found matching application - ID: {}, Name: {}", app.getId(), app.getName());
                break;
            }
        }

        if (appID.isPresent()) {
            // Store result in cache
            appIDCache.put(appName, appID.get());
            return appID.get();
        } else {
            logger.error("Application not found: {}", appName);
            throw new IOException("Application not found");
        }
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
    public static ContrastSDK getSDK(String hostName, String apiKey, String serviceKey, String userName)  {
        logger.info("Initializing ContrastSDK with username: {}, host: {}", userName,  hostName);
        return new ContrastSDK.Builder(userName, serviceKey, apiKey)
                .withApiUrl( "https://" + hostName + "/Contrast/api")
                .withUserAgentProduct(UserAgentProduct.of(MCP_SERVER_NAME,MCP_VERSION))
                .build();

    }


}