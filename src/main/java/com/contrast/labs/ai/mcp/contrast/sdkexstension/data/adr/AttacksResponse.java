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
 * Response wrapper for attacks API calls.
 * Captures both the attack data and pagination metadata from the API response.
 */
public class AttacksResponse {
    private List<Attack> attacks;
    private Integer count;
    private Integer total;
    private Links links;

    /**
     * Nested class for pagination links (not currently used but part of API response)
     */
    public static class Links {
        private String first;
        private String last;
        private String next;
        private String prev;

        public String getFirst() {
            return first;
        }

        public void setFirst(String first) {
            this.first = first;
        }

        public String getLast() {
            return last;
        }

        public void setLast(String last) {
            this.last = last;
        }

        public String getNext() {
            return next;
        }

        public void setNext(String next) {
            this.next = next;
        }

        public String getPrev() {
            return prev;
        }

        public void setPrev(String prev) {
            this.prev = prev;
        }
    }

    // Getters and Setters

    public List<Attack> getAttacks() {
        return attacks;
    }

    public void setAttacks(List<Attack> attacks) {
        this.attacks = attacks;
    }

    /**
     * Total count of attacks matching the query (across all pages).
     * @return Total count, or null if not provided by API
     */
    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    /**
     * Alternative total count field (some APIs use 'total' instead of 'count').
     * @return Total count, or null if not provided by API
     */
    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public Links getLinks() {
        return links;
    }

    public void setLinks(Links links) {
        this.links = links;
    }

    /**
     * Get the total count from either count or total field.
     * Some APIs use 'count', others use 'total'.
     * @return The total count, preferring 'count' over 'total', or null if neither present
     */
    public Integer getTotalCount() {
        if (count != null) {
            return count;
        }
        return total;
    }
}
