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

package com.bytechef.platform.security.web.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.Test;
import org.springaicommunity.mcp.security.server.apikey.authentication.ApiKeyAuthenticationToken;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("HARD_CODE_PASSWORD")
class McpApiKeyCredentialsTest {

    private static final String MCP_SERVER_SECRET_KEY = "mcp-server-path-secret";
    private static final String SECRET_KEY = "bytechef_sk_api_secret";

    @Test
    void testGetIdDoesNotExposeSecret() {
        McpApiKeyCredentials mcpApiKeyCredentials = new McpApiKeyCredentials(
            Environment.PRODUCTION, MCP_SERVER_SECRET_KEY, SECRET_KEY);

        assertThat(mcpApiKeyCredentials.getId())
            .isNotBlank()
            .doesNotContain(SECRET_KEY);
        assertThat(mcpApiKeyCredentials.getSecret()).isEqualTo(SECRET_KEY);
    }

    @Test
    void testGetIdIsStablePerSecret() {
        McpApiKeyCredentials mcpApiKeyCredentials = new McpApiKeyCredentials(
            Environment.PRODUCTION, MCP_SERVER_SECRET_KEY, SECRET_KEY);
        McpApiKeyCredentials sameSecretMcpApiKeyCredentials = new McpApiKeyCredentials(
            Environment.STAGING, "other-path-secret", SECRET_KEY);
        McpApiKeyCredentials otherSecretMcpApiKeyCredentials = new McpApiKeyCredentials(
            Environment.PRODUCTION, MCP_SERVER_SECRET_KEY, "bytechef_sk_other_secret");

        assertThat(mcpApiKeyCredentials.getId()).isEqualTo(sameSecretMcpApiKeyCredentials.getId());
        assertThat(mcpApiKeyCredentials.getId()).isNotEqualTo(otherSecretMcpApiKeyCredentials.getId());
    }

    @Test
    void testGetIdWithoutSecretIsEmpty() {
        McpApiKeyCredentials mcpApiKeyCredentials = new McpApiKeyCredentials(
            Environment.PRODUCTION, MCP_SERVER_SECRET_KEY, null);

        assertThat(mcpApiKeyCredentials.getId()).isEmpty();
        assertThat(mcpApiKeyCredentials.getSecret()).isNull();
    }

    @Test
    void testToStringDoesNotExposeSecrets() {
        McpApiKeyCredentials mcpApiKeyCredentials = new McpApiKeyCredentials(
            Environment.PRODUCTION, MCP_SERVER_SECRET_KEY, SECRET_KEY);

        assertThat(mcpApiKeyCredentials.toString())
            .doesNotContain(SECRET_KEY)
            .doesNotContain(MCP_SERVER_SECRET_KEY);
    }

    @Test
    void testUnauthenticatedTokenNameDoesNotExposeSecret() {
        ApiKeyAuthenticationToken apiKeyAuthenticationToken = ApiKeyAuthenticationToken.unauthenticated(
            new McpApiKeyCredentials(Environment.PRODUCTION, MCP_SERVER_SECRET_KEY, SECRET_KEY));

        assertThat(apiKeyAuthenticationToken.getName()).doesNotContain(SECRET_KEY);
    }
}
