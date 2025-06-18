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
 * Represents a server in the route coverage response.
 */
public class Server {

    private String name;
    private String hostname;
    private String serverpath;
    private String environment;
    private boolean enabled;

    @SerializedName("server_id")
    private long serverId;

    @SerializedName("agent_version")
    private String agentVersion;

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getServerpath() {
        return serverpath;
    }

    public void setServerpath(String serverpath) {
        this.serverpath = serverpath;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getServerId() {
        return serverId;
    }

    public void setServerId(long serverId) {
        this.serverId = serverId;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public void setAgentVersion(String agentVersion) {
        this.agentVersion = agentVersion;
    }

    @Override
    public String toString() {
        return "Server{" +
                "name='" + name + '\'' +
                ", hostname='" + hostname + '\'' +
                ", serverpath='" + serverpath + '\'' +
                ", environment='" + environment + '\'' +
                ", enabled=" + enabled +
                ", serverId=" + serverId +
                ", agentVersion='" + agentVersion + '\'' +
                '}';
    }
}
