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
package com.contrast.labs.ai.mcp.contrast.sdkexstension.data;

import com.contrastsecurity.models.Application;
import com.contrastsecurity.models.Server;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/** An application library. */
public class LibraryExtended {

    /**
     * Return the filename for this library.
     *
     * @return the simple name of the library, like 'log4j-2.1.4.jar'.
     */
    public String getFilename() {
        return fileName;
    }

    @SerializedName("file_name")
    private String fileName;

    /**
     * Return the version of this library according to the library authority like Maven Central or
     * NuGet.
     *
     * @return the version of this library
     */
    public String getVersion() {
        return version;
    }

    private String version;

    public List<Application> getApplications() {
        return apps;
    }

    private List<Application> apps;

    public List<Server> getServers() {
        return servers;
    }

    private List<Server> servers;

    public List<LibraryVulnerabilityExtended> getVulnerabilities() {
        return vulns;
    }

    private List<LibraryVulnerabilityExtended> vulns;

    /**
     * Return the number of classes in this library.
     *
     * @return the number of classes in this library
     */
    public int getClassCount() {
        return classCount;
    }

    @SerializedName("class_count")
    private int classCount;

    /**
     * Return the number of classes used by this library. Right now, this only returns the maximum
     * number of classes used by any one instance of the running application. In the future, this will
     * be changed to represent the total number of distinct classes used across all instances of the
     * running application.
     *
     * @return the maximum number of classes used in any instance of this library
     */
    public int getClassedUsed() {
        return classesUsed;
    }

    @SerializedName("classes_used")
    private int classesUsed;

    /**
     * Return the blob of MANIFEST.MF in plaintext.
     *
     * @return the plaintext MANIFEST.MF file in one String
     */
    public String getManifest() {
        return manifest;
    }

    private String manifest;

    @SerializedName("library_id")
    private long libraryId;

    private String grade;
    private String hash;
    private String group;

    @SerializedName("file_version")
    private String fileVersion;

    @SerializedName("app_id")
    private String appId;

    @SerializedName("app_name")
    private String app_name;

    @SerializedName("app_context_path")
    private String appContextPath;

    @SerializedName("app_language")
    private String appLanguage;

    @SerializedName("latest_version")
    private String latestVersion;

    @SerializedName("release_date")
    private long releaseDate;

    @SerializedName("latest_release_date")
    private long latestReleaseDate;

    @SerializedName("total_vulnerabilities")
    private int totalVulnerabilities;

    @SerializedName("high_vulnerabilities")
    private int highVulnerabilities;

    private boolean custom;

    @SerializedName("lib_score")
    private double libScore;

    @SerializedName("months_outdated")
    private int monthsOutdated;

    public long getLibraryId() {
        return libraryId;
    }

    public String getGrade() {
        return grade;
    }

    public String getHash() {
        return hash;
    }

    public String getFileName() {
        return fileName;
    }

    public String getGroup() {
        return group;
    }

    public String getFileVersion() {
        return fileVersion;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public long getReleaseDate() {
        return releaseDate;
    }

    public long getLatestReleaseDate() {
        return latestReleaseDate;
    }

    public int getTotalVulnerabilities() {
        return totalVulnerabilities;
    }

    public int getHighVulnerabilities() {
        return highVulnerabilities;
    }

    public boolean getCustom() {
        return custom;
    }

    public double getLibScore() {
        return libScore;
    }

    public String getAppLanguage() {
        return appLanguage;
    }

    public int getMonthsOutdated() {
        return monthsOutdated;
    }

    public String getAppId() {
        return appId;
    }

    public String getapp_name() {
        return app_name;
    }

    public String getAppContextPath() {
        return appContextPath;
    }
}
