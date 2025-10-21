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

import java.util.ArrayList;
import java.util.List;

/**
 * Request body for filtering attacks in the attacks API.
 * This class is immutable - use the Builder to construct instances.
 */
public class AttacksFilterBody {
    private final String quickFilter;
    private final String keyword;
    private final boolean includeSuppressed;
    private final boolean includeBotBlockers;
    private final boolean includeIpBlacklist;
    private final List<String> tags;
    private final List<String> statusFilter;
    private final List<String> protectionRules;
    private final List<String> applications;
    private final List<String> applicationImportances;
    private final List<String> attackers;
    private final List<String> servers;
    private final List<String> serverEnvironments;
    private final List<String> severities;

    private AttacksFilterBody(Builder builder) {
        this.quickFilter = builder.quickFilter;
        this.keyword = builder.keyword;
        this.includeSuppressed = builder.includeSuppressed;
        this.includeBotBlockers = builder.includeBotBlockers;
        this.includeIpBlacklist = builder.includeIpBlacklist;
        this.tags = List.copyOf(builder.tags);
        this.statusFilter = List.copyOf(builder.statusFilter);
        this.protectionRules = List.copyOf(builder.protectionRules);
        this.applications = List.copyOf(builder.applications);
        this.applicationImportances = List.copyOf(builder.applicationImportances);
        this.attackers = List.copyOf(builder.attackers);
        this.servers = List.copyOf(builder.servers);
        this.serverEnvironments = List.copyOf(builder.serverEnvironments);
        this.severities = List.copyOf(builder.severities);
    }

    public String getQuickFilter() {
        return quickFilter;
    }

    public String getKeyword() {
        return keyword;
    }

    public boolean isIncludeSuppressed() {
        return includeSuppressed;
    }

    public boolean isIncludeBotBlockers() {
        return includeBotBlockers;
    }

    public boolean isIncludeIpBlacklist() {
        return includeIpBlacklist;
    }

    public List<String> getTags() {
        return tags;
    }

    public List<String> getStatusFilter() {
        return statusFilter;
    }

    public List<String> getProtectionRules() {
        return protectionRules;
    }

    public List<String> getApplications() {
        return applications;
    }

    public List<String> getApplicationImportances() {
        return applicationImportances;
    }

    public List<String> getAttackers() {
        return attackers;
    }

    public List<String> getServers() {
        return servers;
    }

    public List<String> getServerEnvironments() {
        return serverEnvironments;
    }

    public List<String> getSeverities() {
        return severities;
    }

    /**
     * Builder for creating immutable AttacksFilterBody instances.
     */
    public static class Builder {
        private String quickFilter = "ALL";
        private String keyword = "";
        private boolean includeSuppressed = false;
        private boolean includeBotBlockers = false;
        private boolean includeIpBlacklist = false;
        private List<String> tags = new ArrayList<>();
        private List<String> statusFilter = new ArrayList<>();
        private List<String> protectionRules = new ArrayList<>();
        private List<String> applications = new ArrayList<>();
        private List<String> applicationImportances = new ArrayList<>();
        private List<String> attackers = new ArrayList<>();
        private List<String> servers = new ArrayList<>();
        private List<String> serverEnvironments = new ArrayList<>();
        private List<String> severities = new ArrayList<>();

        public Builder quickFilter(String quickFilter) {
            this.quickFilter = quickFilter;
            return this;
        }

        public Builder keyword(String keyword) {
            this.keyword = keyword;
            return this;
        }

        public Builder includeSuppressed(boolean includeSuppressed) {
            this.includeSuppressed = includeSuppressed;
            return this;
        }

        public Builder includeBotBlockers(boolean includeBotBlockers) {
            this.includeBotBlockers = includeBotBlockers;
            return this;
        }

        public Builder includeIpBlacklist(boolean includeIpBlacklist) {
            this.includeIpBlacklist = includeIpBlacklist;
            return this;
        }

        public Builder tags(List<String> tags) {
            this.tags = new ArrayList<>(tags);
            return this;
        }

        public Builder statusFilter(List<String> statusFilter) {
            this.statusFilter = new ArrayList<>(statusFilter);
            return this;
        }

        public Builder protectionRules(List<String> protectionRules) {
            this.protectionRules = new ArrayList<>(protectionRules);
            return this;
        }

        public Builder applications(List<String> applications) {
            this.applications = new ArrayList<>(applications);
            return this;
        }

        public Builder applicationImportances(List<String> applicationImportances) {
            this.applicationImportances = new ArrayList<>(applicationImportances);
            return this;
        }

        public Builder attackers(List<String> attackers) {
            this.attackers = new ArrayList<>(attackers);
            return this;
        }

        public Builder servers(List<String> servers) {
            this.servers = new ArrayList<>(servers);
            return this;
        }

        public Builder serverEnvironments(List<String> serverEnvironments) {
            this.serverEnvironments = new ArrayList<>(serverEnvironments);
            return this;
        }

        public Builder severities(List<String> severities) {
            this.severities = new ArrayList<>(severities);
            return this;
        }

        public AttacksFilterBody build() {
            return new AttacksFilterBody(this);
        }
    }
}
