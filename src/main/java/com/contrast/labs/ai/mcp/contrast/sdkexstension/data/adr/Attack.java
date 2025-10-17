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
 * Class representing a complete attack event with all associated applications and servers.
 */
public class Attack {
    private String status;
    private String type;
    private String source;
    private boolean active;
    private boolean suppressed;
    private List<ApplicationAttackInfo> attacksApplication;
    private List<String> rules;
    private List<Server> servers;
    private List<String> attackers; // List of authenticated user identifiers (usernames/user IDs) involved in the attack
    private int id;
    private String uuid;
    private String attack_label;
    private long start_time;
    private long end_time;
    private int probes;
    private long attack_duration;
    private int requests_per_second;
    private String severity_code; // Valid values: "LOW", "MEDIUM", "HIGH"
    private String application_importance; // Valid values: "UNIMPORTANT", "LOW", "MEDIUM", "HIGH", "CRITICAL"
    private String attack_impact; // Valid values: "NONE", "LOW", "MEDIUM", "HIGH"
    private long first_event_time;
    private long last_event_time;
    private Object user_agents;

    /**
     * Nested class representing an application's involvement in an attack.
     */
    public static class ApplicationAttackInfo {
        private String status;
        private String severity;
        private long startTime;
        private long endTime;
        private Application application;
        private int id;
        private String uuid;
        private String attack_label;

        // Getters and Setters
        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public long getStartTime() {
            return startTime;
        }

        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }

        public long getEndTime() {
            return endTime;
        }

        public void setEndTime(long endTime) {
            this.endTime = endTime;
        }

        public Application getApplication() {
            return application;
        }

        public void setApplication(Application application) {
            this.application = application;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getAttack_label() {
            return attack_label;
        }

        public void setAttack_label(String attack_label) {
            this.attack_label = attack_label;
        }
    }

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isSuppressed() {
        return suppressed;
    }

    public void setSuppressed(boolean suppressed) {
        this.suppressed = suppressed;
    }

    public List<ApplicationAttackInfo> getAttacksApplication() {
        return attacksApplication;
    }

    public void setAttacksApplication(List<ApplicationAttackInfo> attacksApplication) {
        this.attacksApplication = attacksApplication;
    }

    public List<String> getRules() {
        return rules;
    }

    public void setRules(List<String> rules) {
        this.rules = rules;
    }

    public List<Server> getServers() {
        return servers;
    }

    public void setServers(List<Server> servers) {
        this.servers = servers;
    }

    public List<String> getAttackers() {
        return attackers;
    }

    public void setAttackers(List<String> attackers) {
        this.attackers = attackers;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getAttack_label() {
        return attack_label;
    }

    public void setAttack_label(String attack_label) {
        this.attack_label = attack_label;
    }

    public long getStart_time() {
        return start_time;
    }

    public void setStart_time(long start_time) {
        this.start_time = start_time;
    }

    public long getEnd_time() {
        return end_time;
    }

    public void setEnd_time(long end_time) {
        this.end_time = end_time;
    }

    public int getProbes() {
        return probes;
    }

    public void setProbes(int probes) {
        this.probes = probes;
    }

    public long getAttack_duration() {
        return attack_duration;
    }

    public void setAttack_duration(long attack_duration) {
        this.attack_duration = attack_duration;
    }

    public int getRequests_per_second() {
        return requests_per_second;
    }

    public void setRequests_per_second(int requests_per_second) {
        this.requests_per_second = requests_per_second;
    }

    public String getSeverity_code() {
        return severity_code;
    }

    public void setSeverity_code(String severity_code) {
        this.severity_code = severity_code;
    }

    public String getApplication_importance() {
        return application_importance;
    }

    public void setApplication_importance(String application_importance) {
        this.application_importance = application_importance;
    }

    public String getAttack_impact() {
        return attack_impact;
    }

    public void setAttack_impact(String attack_impact) {
        this.attack_impact = attack_impact;
    }

    public long getFirst_event_time() {
        return first_event_time;
    }

    public void setFirst_event_time(long first_event_time) {
        this.first_event_time = first_event_time;
    }

    public long getLast_event_time() {
        return last_event_time;
    }

    public void setLast_event_time(long last_event_time) {
        this.last_event_time = last_event_time;
    }

    public Object getUser_agents() {
        return user_agents;
    }

    public void setUser_agents(Object user_agents) {
        this.user_agents = user_agents;
    }
}