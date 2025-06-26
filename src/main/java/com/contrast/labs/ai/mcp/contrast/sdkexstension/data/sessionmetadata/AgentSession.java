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
package com.contrast.labs.ai.mcp.contrast.sdkexstension.data.sessionmetadata;

import java.util.List;

/**
 * Represents an agent session with its metadata
 */
public class AgentSession {
    private String agentSessionId;
    private List<MetadataSession> metadataSessions;
    private double createdDate;
    private String sessionStatus;

    public String getAgentSessionId() {
        return agentSessionId;
    }

    public void setAgentSessionId(String agentSessionId) {
        this.agentSessionId = agentSessionId;
    }

    public List<MetadataSession> getMetadataSessions() {
        return metadataSessions;
    }

    public void setMetadataSessions(List<MetadataSession> metadataSessions) {
        this.metadataSessions = metadataSessions;
    }

    public double getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(double createdDate) {
        this.createdDate = createdDate;
    }

    public String getSessionStatus() {
        return sessionStatus;
    }

    public void setSessionStatus(String sessionStatus) {
        this.sessionStatus = sessionStatus;
    }
}
