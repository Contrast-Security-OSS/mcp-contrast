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

import java.util.List;

/**
 * Request body for filtering attacks in the attacks API.
 */
public class AttacksFilterBody {
    private String quickFilter = "ALL";
    private String keyword = "";
    private boolean includeSuppressed = false;
    private boolean includeBotBlockers = false;
    private boolean includeIpBlacklist = false;
    private List<String> tags;
    private List<String> statusFilter;
    private List<String> protectionRules;
    private List<String> applications;
    private List<String> applicationImportances;
    private List<String> attackers;
    private List<String> servers;
    private List<String> serverEnvironments;
    private List<String> severities;

    public AttacksFilterBody() {
        // Initialize empty lists for default constructor
        this.tags = List.of();
        this.statusFilter = List.of();
        this.protectionRules = List.of();
        this.applications = List.of();
        this.applicationImportances = List.of();
        this.attackers = List.of();
        this.servers = List.of();
        this.serverEnvironments = List.of();
        this.severities = List.of();
    }

    public String getQuickFilter() {
        return quickFilter;
    }

    public void setQuickFilter(String quickFilter) {
        this.quickFilter = quickFilter;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public boolean isIncludeSuppressed() {
        return includeSuppressed;
    }

    public void setIncludeSuppressed(boolean includeSuppressed) {
        this.includeSuppressed = includeSuppressed;
    }

    public boolean isIncludeBotBlockers() {
        return includeBotBlockers;
    }

    public void setIncludeBotBlockers(boolean includeBotBlockers) {
        this.includeBotBlockers = includeBotBlockers;
    }

    public boolean isIncludeIpBlacklist() {
        return includeIpBlacklist;
    }

    public void setIncludeIpBlacklist(boolean includeIpBlacklist) {
        this.includeIpBlacklist = includeIpBlacklist;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getStatusFilter() {
        return statusFilter;
    }

    public void setStatusFilter(List<String> statusFilter) {
        this.statusFilter = statusFilter;
    }

    public List<String> getProtectionRules() {
        return protectionRules;
    }

    public void setProtectionRules(List<String> protectionRules) {
        this.protectionRules = protectionRules;
    }

    public List<String> getApplications() {
        return applications;
    }

    public void setApplications(List<String> applications) {
        this.applications = applications;
    }

    public List<String> getApplicationImportances() {
        return applicationImportances;
    }

    public void setApplicationImportances(List<String> applicationImportances) {
        this.applicationImportances = applicationImportances;
    }

    public List<String> getAttackers() {
        return attackers;
    }

    public void setAttackers(List<String> attackers) {
        this.attackers = attackers;
    }

    public List<String> getServers() {
        return servers;
    }

    public void setServers(List<String> servers) {
        this.servers = servers;
    }

    public List<String> getServerEnvironments() {
        return serverEnvironments;
    }

    public void setServerEnvironments(List<String> serverEnvironments) {
        this.serverEnvironments = serverEnvironments;
    }

    public List<String> getSeverities() {
        return severities;
    }

    public void setSeverities(List<String> severities) {
        this.severities = severities;
    }
}