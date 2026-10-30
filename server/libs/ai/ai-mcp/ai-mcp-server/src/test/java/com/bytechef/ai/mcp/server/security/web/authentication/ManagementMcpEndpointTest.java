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

package com.bytechef.ai.mcp.server.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.tenant.domain.TenantKey;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ManagementMcpEndpointTest {

    private static final String MCP_SERVER_SECRET_KEY = String.valueOf(TenantKey.of("acme"));

    private final ManagementMcpEndpoint managementMcpEndpoint = new ManagementMcpEndpoint();

    @Test
    void testMatchesManagementPaths() {
        assertThat(managementMcpEndpoint.matches("/api/management/mcp")).isTrue();
        assertThat(managementMcpEndpoint.matches("/api/management/" + MCP_SERVER_SECRET_KEY + "/mcp")).isTrue();
        assertThat(managementMcpEndpoint.matches("/api/management/a/b/mcp")).isFalse();
        assertThat(managementMcpEndpoint.matches("/api/automation/" + MCP_SERVER_SECRET_KEY + "/mcp")).isFalse();
    }

    @Test
    void testScope() {
        assertThat(managementMcpEndpoint.getScope()).isEqualTo("mcp:management");
    }

    @Test
    void testTenantLessPathLeavesTenantToToken() {
        assertThat(managementMcpEndpoint.resolveTenantId("/api/management/mcp")).isEmpty();
    }

    @Test
    void testResolveTenantIdFromSecretKey() {
        assertThat(managementMcpEndpoint.resolveTenantId("/api/management/" + MCP_SERVER_SECRET_KEY + "/mcp"))
            .contains("acme");
    }

    @Test
    void testResolveTenantIdRejectsOtherPath() {
        assertThatThrownBy(() -> managementMcpEndpoint.resolveTenantId("/api/management/a/b/mcp"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testResolveResourceName() {
        assertThat(managementMcpEndpoint.resolveResourceName("https://bytechef.test/api/management/mcp"))
            .contains("Management");
        assertThat(
            managementMcpEndpoint.resolveResourceName(
                "https://bytechef.test/api/management/" + MCP_SERVER_SECRET_KEY + "/mcp"))
                    .contains("Management");
        assertThat(
            managementMcpEndpoint.resolveResourceName(
                "https://bytechef.test/api/automation/" + MCP_SERVER_SECRET_KEY + "/mcp"))
                    .isEmpty();
    }

}
