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
package com.contrast.labs.ai.mcp.contrast;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

import static java.util.List.of;

@SpringBootApplication
public class McpContrastApplication {

	public static void main(String[] args)  {
		SpringApplication.run(McpContrastApplication.class, args);
	}

	@Bean
	public List<ToolCallback> tools(AssessService assessService, SastService sastService, SCAService scaService, ADRService adrService, RouteCoverageService routeCoverageService) {
		return of(ToolCallbacks.from(assessService, sastService, scaService, adrService, routeCoverageService));
	}

}
