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

public class SDKHelper {


    private static final String MCP_SERVER_NAME = "contrast-mcp";
    private static final String MCP_VERSION = "1.0";

    private static final Logger logger = LoggerFactory.getLogger(SDKHelper.class);

    private static final Cache<String, List<LibraryExtended>> libraryCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    private static final Cache<String, String> appIDCache = CacheBuilder.newBuilder()
            .maximumSize(100)
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

    // The withUserAgentProduct will generate a user agent header that looks like
    // User-Agent: contrast-mcp/1.0 contrast-sdk-java/3.4.2 Java/19.0.2+7
    public static ContrastSDK getSDK(String hostName, String apiKey, String serviceKey, String userName)  {
        logger.debug("Initializing ContrastSDK with username: {}, host: {}", userName,  hostName);
        return new ContrastSDK.Builder(userName, serviceKey, apiKey)
                .withApiUrl(  hostName + "/Contrast/api")
                .withUserAgentProduct(UserAgentProduct.of(MCP_SERVER_NAME,MCP_VERSION))
                .build();

    }


}