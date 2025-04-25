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

/**
 * Class representing application information affected by a vulnerability.
 */
public class App {

    private String name;
    private String app_id;
    private long last_seen;
    private Object last_reset;  // Using Object as last_reset was null in the example
    private long first_seen;
    private String importance_description;
    private int classCount;

    public int getClassUsage() {
        return classUsage;
    }

    public void setClassUsage(int classUsage) {
        this.classUsage = classUsage;
    }

    public int getClassCount() {
        return classCount;
    }

    public void setClassCount(int classCount) {
        this.classCount = classCount;
    }

    private int classUsage;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApp_id() {
        return app_id;
    }

    public void setApp_id(String app_id) {
        this.app_id = app_id;
    }

    public long getLast_seen() {
        return last_seen;
    }

    public void setLast_seen(long last_seen) {
        this.last_seen = last_seen;
    }

    public Object getLast_reset() {
        return last_reset;
    }

    public void setLast_reset(Object last_reset) {
        this.last_reset = last_reset;
    }

    public long getFirst_seen() {
        return first_seen;
    }

    public void setFirst_seen(long first_seen) {
        this.first_seen = first_seen;
    }

    public String getImportance_description() {
        return importance_description;
    }

    public void setImportance_description(String importance_description) {
        this.importance_description = importance_description;
    }
}