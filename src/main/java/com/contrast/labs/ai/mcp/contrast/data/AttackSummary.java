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
package com.contrast.labs.ai.mcp.contrast.data;

import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.adr.Attack;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Simplified attack data record focusing on key information for MCP tools.
 * Contains: dates, rules, status, severity, applications, source IP, and probe count.
 */
public record AttackSummary(
    String attackId,
    String status,
    String source,
    List<String> rules,
    int probes,
    String startTime,
    String endTime,
    long startTimeMs,
    long endTimeMs,
    String firstEventTime,
    String lastEventTime,
    long firstEventTimeMs,
    long lastEventTimeMs,
    List<ApplicationAttackInfo> applications
) {
    
    /**
     * Creates an AttackSummary from a full Attack object, extracting key information.
     */
    public static AttackSummary fromAttack(Attack attack) {
        List<ApplicationAttackInfo> appInfos = attack.getAttacksApplication().stream()
            .map(ApplicationAttackInfo::fromAttackApplication)
            .collect(Collectors.toList());
            
        return new AttackSummary(
            attack.getUuid(),
            attack.getStatus(),
            attack.getSource(),
            attack.getRules(),
            attack.getProbes(),
            new Date(attack.getStart_time()).toString(),
            new Date(attack.getEnd_time()).toString(),
            attack.getStart_time(),
            attack.getEnd_time(),
            new Date(attack.getFirst_event_time()).toString(),
            new Date(attack.getLast_event_time()).toString(),
            attack.getFirst_event_time(),
            attack.getLast_event_time(),
            appInfos
        );
    }
    
    /**
     * Information about an application involved in the attack.
     */
    public record ApplicationAttackInfo(
        String applicationId,
        String applicationName,
        String language,
        String severity,
        String status,
        String startTime,
        String endTime,
        long startTimeMs,
        long endTimeMs
    ) {
        
        public static ApplicationAttackInfo fromAttackApplication(Attack.ApplicationAttackInfo attackApp) {
            return new ApplicationAttackInfo(
                attackApp.getApplication().getApp_id(),
                attackApp.getApplication().getName(),
                attackApp.getApplication().getLanguage(),
                attackApp.getSeverity(),
                attackApp.getStatus(),
                new Date(attackApp.getStartTime()).toString(),
                new Date(attackApp.getEndTime()).toString(),
                attackApp.getStartTime(),
                attackApp.getEndTime()
            );
        }
    }
}