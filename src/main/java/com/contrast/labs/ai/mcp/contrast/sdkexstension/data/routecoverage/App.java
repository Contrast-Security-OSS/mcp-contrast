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
package com.contrast.labs.ai.mcp.contrast.sdkexstension.data.routecoverage;

import com.google.gson.annotations.SerializedName;

/**
 * Represents an application in the route coverage response.
 */
public class App {

    private boolean primary;
    private boolean master;
    private boolean child;
    private String importance;

    @SerializedName("app_id")
    private String appId;

    private String name;

    @SerializedName("parent_app_id")
    private String parentAppId;

    @SerializedName("total_modules")
    private int totalModules;

    private String language;

    @SerializedName("context_path")
    private String contextPath;

    @SerializedName("last_seen")
    private long lastSeen;

    @SerializedName("importance_description")
    private String importanceDescription;

    // Getters and setters
    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public boolean isMaster() {
        return master;
    }

    public void setMaster(boolean master) {
        this.master = master;
    }

    public boolean isChild() {
        return child;
    }

    public void setChild(boolean child) {
        this.child = child;
    }

    public String getImportance() {
        return importance;
    }

    public void setImportance(String importance) {
        this.importance = importance;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParentAppId() {
        return parentAppId;
    }

    public void setParentAppId(String parentAppId) {
        this.parentAppId = parentAppId;
    }

    public int getTotalModules() {
        return totalModules;
    }

    public void setTotalModules(int totalModules) {
        this.totalModules = totalModules;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public String getImportanceDescription() {
        return importanceDescription;
    }

    public void setImportanceDescription(String importanceDescription) {
        this.importanceDescription = importanceDescription;
    }

    @Override
    public String toString() {
        return "App{" +
                "primary=" + primary +
                ", master=" + master +
                ", child=" + child +
                ", importance='" + importance + '\'' +
                ", appId='" + appId + '\'' +
                ", name='" + name + '\'' +
                ", parentAppId='" + parentAppId + '\'' +
                ", totalModules=" + totalModules +
                ", language='" + language + '\'' +
                ", contextPath='" + contextPath + '\'' +
                ", lastSeen=" + lastSeen +
                ", importanceDescription='" + importanceDescription + '\'' +
                '}';
    }
}
