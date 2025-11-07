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

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Class representing a complete attack event with all associated applications and servers.
 */
@Getter
@Setter
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
    private List<String> user_agents;

    /**
     * Nested class representing an application's involvement in an attack.
     */
    @Getter
    @Setter
    public static class ApplicationAttackInfo {
        private String status;
        private String severity;
        private long startTime;
        private long endTime;
        private Application application;
        private int id;
        private String uuid;
        private String attack_label;
    }
}