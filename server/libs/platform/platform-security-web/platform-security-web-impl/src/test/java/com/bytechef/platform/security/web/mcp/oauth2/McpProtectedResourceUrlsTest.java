/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.security.web.mcp.oauth2;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class McpProtectedResourceUrlsTest {

    @Test
    void testResolveResourceUrlPrefersPublicUrl() {
        assertThat(
            McpProtectedResourceUrls.resolveResourceUrl(
                "https://bytechef.example.com/", "http://internal:9555", "/api/management/mcp"))
                    .isEqualTo("https://bytechef.example.com/api/management/mcp");
    }

    @Test
    void testResolveResourceUrlFallsBackToRequestBaseUrlWithoutPublicUrl() {
        assertThat(McpProtectedResourceUrls.resolveResourceUrl(" ", "http://internal:9555", "/api/management/mcp"))
            .isEqualTo("http://internal:9555/api/management/mcp");
    }

    @Test
    void testToPublicResourceUrlKeepsPathAndReplacesOrigin() {
        assertThat(
            McpProtectedResourceUrls.toPublicResourceUrl(
                "https://bytechef.example.com", "http://localhost:9555/api/automation/c2VjcmV0/mcp"))
                    .isEqualTo("https://bytechef.example.com/api/automation/c2VjcmV0/mcp");
    }

    @Test
    void testToPublicResourceUrlLeavesUrlUnchangedWithoutPublicUrl() {
        assertThat(McpProtectedResourceUrls.toPublicResourceUrl(null, "http://localhost:9555/api/management/mcp"))
            .isEqualTo("http://localhost:9555/api/management/mcp");
    }

    @Test
    void testGetRequiredScopeMapsEachMcpEndpoint() {
        assertThat(McpProtectedResourceUrls.getRequiredScope("/api/automation/c2VjcmV0/mcp"))
            .isEqualTo(McpProtectedResourceUrls.AUTOMATION_SCOPE);
        assertThat(McpProtectedResourceUrls.getRequiredScope("/api/management/c2VjcmV0/mcp"))
            .isEqualTo(McpProtectedResourceUrls.MANAGEMENT_SCOPE);
        assertThat(McpProtectedResourceUrls.getRequiredScope("/api/management/mcp"))
            .isEqualTo(McpProtectedResourceUrls.MANAGEMENT_SCOPE);
    }

    @Test
    void testGetRequiredScopeReturnsNullForOtherPaths() {
        assertThat(McpProtectedResourceUrls.getRequiredScope("/api/platform/v1/workflows")).isNull();
        assertThat(McpProtectedResourceUrls.getRequiredScope("/api/embedded/c2VjcmV0/mcp")).isNull();
    }
}
