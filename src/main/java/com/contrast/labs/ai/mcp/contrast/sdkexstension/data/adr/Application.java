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
package com.contrast.labs.ai.mcp.contrast.sdkexstension.data.adr;

/**
 * Class representing the application information in an attack event.
 */
public class Application {
    private boolean primary;
    private boolean master;
    private boolean child;
    private String status;
    private int importance;
    private String app_id;
    private String name;
    private String parent_app_id;
    private int total_modules;
    private String language;
    private String context_path;
    private long last_seen;
    private String license_level;
    private String importance_description;
    private long first_seen;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getImportance() {
        return importance;
    }

    public void setImportance(int importance) {
        this.importance = importance;
    }

    public String getApp_id() {
        return app_id;
    }

    public void setApp_id(String app_id) {
        this.app_id = app_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParent_app_id() {
        return parent_app_id;
    }

    public void setParent_app_id(String parent_app_id) {
        this.parent_app_id = parent_app_id;
    }

    public int getTotal_modules() {
        return total_modules;
    }

    public void setTotal_modules(int total_modules) {
        this.total_modules = total_modules;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getContext_path() {
        return context_path;
    }

    public void setContext_path(String context_path) {
        this.context_path = context_path;
    }

    public long getLast_seen() {
        return last_seen;
    }

    public void setLast_seen(long last_seen) {
        this.last_seen = last_seen;
    }

    public String getLicense_level() {
        return license_level;
    }

    public void setLicense_level(String license_level) {
        this.license_level = license_level;
    }

    public String getImportance_description() {
        return importance_description;
    }

    public void setImportance_description(String importance_description) {
        this.importance_description = importance_description;
    }

    public long getFirst_seen() {
        return first_seen;
    }

    public void setFirst_seen(long first_seen) {
        this.first_seen = first_seen;
    }
}