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

package com.bytechef.component.mcp.client.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.component.mcp.client.constant.McpClientConstants;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpRequestSnapshot;
import io.modelcontextprotocol.client.transport.McpHttpClientTransportAuthorizationException;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.mcp.ToolContextToMcpMetaConverter;

/**
 * @author Ivica Cardic
 */
class McpClientUtilsTest {

    @Test
    void testToolContextToMcpMetaConverterDropsTheConversationId() {
        ToolContextToMcpMetaConverter toolContextToMcpMetaConverter =
            McpClientUtils.createToolContextToMcpMetaConverter();

        Map<String, Object> mcpMeta = toolContextToMcpMetaConverter.convert(
            new ToolContext(
                Map.of(
                    ChatMemory.CONVERSATION_ID, "conversation-1",
                    McpToolUtils.TOOL_CONTEXT_MCP_EXCHANGE_KEY, "exchange",
                    "tenantHint", "tenant-1")));

        assertThat(mcpMeta).containsExactly(Map.entry("tenantHint", "tenant-1"));
    }

    @Test
    void testToolContextToMcpMetaConverterHandlesAnEmptyToolContext() {
        ToolContextToMcpMetaConverter toolContextToMcpMetaConverter =
            McpClientUtils.createToolContextToMcpMetaConverter();

        assertThat(toolContextToMcpMetaConverter.convert(new ToolContext(Map.of()))).isEmpty();
    }

    @Test
    void testCreateMcpSyncClientAppliesClientCredentialsAuthorization() {
        assertAppliedAuthorizationType(
            Map.of(
                Authorization.CLIENT_ID, "client-id",
                Authorization.CLIENT_SECRET, "client-secret",
                Authorization.TOKEN_URL, "https://auth.example.com/token"),
            AuthorizationType.OAUTH2_CLIENT_CREDENTIALS);
    }

    @Test
    void testCreateMcpSyncClientAppliesAuthorizationCodeAuthorization() {
        assertAppliedAuthorizationType(
            Map.of(
                Authorization.ACCESS_TOKEN, "access-token",
                Authorization.AUTHORIZATION_URL, "https://auth.example.com/authorize",
                Authorization.CLIENT_ID, "client-id",
                Authorization.CLIENT_SECRET, "client-secret",
                Authorization.TOKEN_URL, "https://auth.example.com/token"),
            AuthorizationType.OAUTH2_AUTHORIZATION_CODE);
    }

    @Test
    void testCreateMcpSyncClientAppliesBearerTokenAuthorization() {
        assertAppliedAuthorizationType(Map.of(Authorization.TOKEN, "token"), AuthorizationType.BEARER_TOKEN);
    }

    @Test
    void testTranslateAuthorizationErrorsMapsSdk401ToProviderException() {
        McpHttpClientTransportAuthorizationException authorizationException = authorizationException(401);

        assertThatThrownBy(() -> McpClientUtils.translateAuthorizationErrors(() -> {
            throw authorizationException;
        }))
            .isInstanceOf(ProviderException.class)
            .extracting("statusCode")
            .isEqualTo(401);
    }

    @Test
    void testCreateMcpSyncClientTranslatesWrappedAuthorizationError() {
        RuntimeException wrapped = new RuntimeException("reactor wrapper", authorizationException(403));

        assertThatThrownBy(() -> McpClientUtils.translateAuthorizationErrors(() -> {
            throw wrapped;
        }))
            .isInstanceOf(ProviderException.class)
            .hasCause(wrapped)
            .extracting("statusCode")
            .isEqualTo(403);
    }

    @Test
    void testTranslateAuthorizationErrorsLeavesOtherExceptionsAlone() {
        IllegalStateException other = new IllegalStateException("boom");

        assertThatThrownBy(() -> McpClientUtils.translateAuthorizationErrors(() -> {
            throw other;
        })).isSameAs(other);
    }

    @Test
    void testCreateMcpSyncClientAppliesBearerTokenForDiscoveryConnection() {
        assertAppliedAuthorizationType(
            Map.of(
                Authorization.ACCESS_TOKEN, "access-token",
                Authorization.AUTHORIZATION_URL, "https://auth.example.com/authorize",
                Authorization.TOKEN_URL, "https://auth.example.com/token",
                Authorization.CLIENT_ID, "reg-1",
                Authorization.TOKEN_ENDPOINT_AUTH_METHOD, "none",
                Authorization.RESOURCE, "https://mcp.example.com/mcp"),
            AuthorizationType.OAUTH2_AUTHORIZATION_CODE);
    }

    private static McpHttpClientTransportAuthorizationException authorizationException(int statusCode) {
        HttpResponse.ResponseInfo responseInfo = mock(HttpResponse.ResponseInfo.class);

        when(responseInfo.statusCode()).thenReturn(statusCode);

        return new McpHttpClientTransportAuthorizationException(
            "Authorization error",
            new HttpRequestSnapshot(
                URI.create("https://mcp.example.com/mcp"), "POST", HttpHeaders.of(Map.of(), (name, value) -> true)),
            responseInfo);
    }

    private static void assertAppliedAuthorizationType(
        Map<String, Object> authorizationParameters, AuthorizationType expectedAuthorizationType) {

        Map<String, Object> connectionParameterMap = new HashMap<>(authorizationParameters);

        connectionParameterMap.put(McpClientConstants.URL, "https://mcp.example.com/mcp");

        Parameters connectionParameters = MockParametersFactory.create(connectionParameterMap);
        ConnectionDefinitionService connectionDefinitionService = mock(ConnectionDefinitionService.class);
        Context context = mock(Context.class);

        when(connectionDefinitionService.executeAuthorizationApply(any(), eq(1), any(), anyMap(), any()))
            .thenReturn(ApplyResponse.ofHeaders(Map.of()));

        try (McpSyncClient mcpSyncClient = McpClientUtils.createMcpSyncClient(
            connectionParameters, connectionDefinitionService, context)) {

            assertThat(mcpSyncClient).isNotNull();
        }

        verify(connectionDefinitionService).executeAuthorizationApply(
            eq(McpClientConstants.MCP_CLIENT), eq(1), eq(expectedAuthorizationType), anyMap(), eq(context));
    }
}
