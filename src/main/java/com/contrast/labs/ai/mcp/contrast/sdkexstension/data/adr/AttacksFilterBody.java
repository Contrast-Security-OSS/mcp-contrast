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

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

/**
 * Request body for filtering attacks in the attacks API.
 * This class is immutable - use the Builder to construct instances.
 */
@Value
@Builder
public class AttacksFilterBody {
    @Builder.Default
    String quickFilter = "ALL";

    @Builder.Default
    String keyword = "";

    @Builder.Default
    boolean includeSuppressed = false;

    @Builder.Default
    boolean includeBotBlockers = false;

    @Builder.Default
    boolean includeIpBlacklist = false;

    @Singular
    List<String> tags;

    @Singular("statusFilter")
    List<String> statusFilter;

    @Singular("protectionRule")
    List<String> protectionRules;

    @Singular("application")
    List<String> applications;

    @Singular
    List<String> applicationImportances;

    @Singular("attacker")
    List<String> attackers;

    @Singular("server")
    List<String> servers;

    @Singular
    List<String> serverEnvironments;

    @Singular
    List<String> severities;
}
