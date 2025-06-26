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
package com.contrast.labs.ai.mcp.contrast.sdkexstension.data.traces;

import java.util.List;

/**
 * Extended Traces class that includes a list of extended trace objects with session metadata.
 * This replaces the standard Traces class from the Contrast SDK.
 */
public class TracesExtended {
    private boolean success;
    private List<String> messages;
    private List<TraceExtended> traces;
    private int count;
    private int licensedCount;
    private List<Object> links;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    public List<TraceExtended> getTraces() {
        return traces;
    }

    public void setTraces(List<TraceExtended> traces) {
        this.traces = traces;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getLicensedCount() {
        return licensedCount;
    }

    public void setLicensedCount(int licensedCount) {
        this.licensedCount = licensedCount;
    }

    public List<Object> getLinks() {
        return links;
    }

    public void setLinks(List<Object> links) {
        this.links = links;
    }
}
