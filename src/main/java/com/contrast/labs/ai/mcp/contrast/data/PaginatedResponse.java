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

import java.util.List;

/**
 * Generic paginated response wrapper for all list-returning MCP tools.
 * Provides consistent pagination metadata and messaging across all endpoints.
 *
 * @param <T> The type of items in the paginated response
 * @param items The data for the current page (never null, empty list if no results)
 * @param page The page number returned (1-based, always ≥ 1)
 * @param pageSize Items per page used for this response (1-100)
 * @param totalItems Total count across all pages (null if unavailable or expensive to compute)
 * @param hasMorePages true if additional pages exist beyond this page
 * @param message Optional informational message for the AI (validation warnings, empty result explanations, etc.)
 */
public record PaginatedResponse<T>(
    List<T> items,
    int page,
    int pageSize,
    Integer totalItems,
    boolean hasMorePages,
    String message
) {
    /**
     * Creates a paginated response with no message
     */
    public PaginatedResponse(List<T> items, int page, int pageSize,
                            Integer totalItems, boolean hasMorePages) {
        this(items, page, pageSize, totalItems, hasMorePages, null);
    }

    /**
     * Creates an empty paginated response with a message
     */
    public static <T> PaginatedResponse<T> empty(int page, int pageSize, String message) {
        return new PaginatedResponse<>(List.of(), page, pageSize, 0, false, message);
    }

    /**
     * Creates an error response for validation failures.
     * Returns empty items with error message.
     */
    public static <T> PaginatedResponse<T> error(int page, int pageSize, String errorMessage) {
        return new PaginatedResponse<>(List.of(), page, pageSize, 0, false, errorMessage);
    }
}
