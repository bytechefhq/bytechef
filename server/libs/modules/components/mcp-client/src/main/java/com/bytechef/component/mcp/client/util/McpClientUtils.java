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

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static com.bytechef.component.definition.Authorization.KEY;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.ALL;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.EXCLUDE;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.HTTP_SSE;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.HTTP_STREAMABLE;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.INCLUDE;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.TOOLS_TO_EXCLUDE;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.TOOLS_TO_INCLUDE;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.TOOL_FILTER_TYPE;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.TOOL_NAME;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.TRANSPORT_TYPE;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.URL;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.mcp.client.constant.McpClientConstants;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.springframework.ai.mcp.McpToolFilter;

/**
 * @author Ivica Cardic
 */
public class McpClientUtils {

    private McpClientUtils() {
    }

    public static List<Option<String>> getToolOptions(
        Parameters connectionParameters, ConnectionDefinitionService connectionDefinitionService,
        Context context) {

        try (McpSyncClient mcpSyncClient = createMcpSyncClient(
            connectionParameters, connectionDefinitionService, context)) {
            mcpSyncClient.initialize();

            McpSchema.ListToolsResult listToolsResult = mcpSyncClient.listTools();

            List<McpSchema.Tool> tools = listToolsResult.tools();

            return tools.stream()
                .map(tool -> (Option<String>) option(tool.name(), tool.name()))
                .toList();
        }
    }

    public static List<? extends Property.ValueProperty<?>> getToolProperties(
        Parameters inputParameters, Parameters connectionParameters,
        ConnectionDefinitionService connectionDefinitionService, Context context) {

        String toolName = inputParameters.getString(TOOL_NAME);

        if (toolName == null || toolName.isBlank()) {
            return List.of();
        }

        try (McpSyncClient mcpSyncClient = createMcpSyncClient(
            connectionParameters, connectionDefinitionService, context)) {

            mcpSyncClient.initialize();

            List<McpSchema.Tool> tools = mcpSyncClient.listTools()
                .tools();

            McpSchema.Tool tool = tools.stream()
                .filter(mcpTool -> toolName.equals(mcpTool.name()))
                .findFirst()
                .orElse(null);

            if (tool == null || tool.inputSchema() == null) {
                return List.of();
            }

            McpSchema.JsonSchema inputSchema = tool.inputSchema();

            Map<String, Object> schemaProperties = inputSchema.properties();

            if (schemaProperties == null) {
                return List.of();
            }

            List<String> requiredProperties = inputSchema.required();

            Set<String> requiredPropertyNames =
                requiredProperties != null ? Set.copyOf(requiredProperties) : Set.of();

            List<ModifiableValueProperty<?, ?>> properties = new ArrayList<>();

            for (Map.Entry<String, Object> entry : schemaProperties.entrySet()) {
                String propertyName = entry.getKey();

                @SuppressWarnings("unchecked")
                Map<String, Object> propertySchema = (Map<String, Object>) entry.getValue();

                ModifiableValueProperty<?, ?> property = createProperty(propertyName, propertySchema);

                if (requiredPropertyNames.contains(propertyName)) {
                    property.required(true);
                }

                properties.add(property);
            }

            return properties;
        }
    }

    public static McpSyncClient createMcpSyncClient(
        Parameters connectionParameters, ConnectionDefinitionService connectionDefinitionService,
        Context context) {

        String serverUrl = connectionParameters.getRequiredString(URL);
        String transportType = connectionParameters.getString(TRANSPORT_TYPE, HTTP_STREAMABLE);

        Consumer<HttpRequest.Builder> requestCustomizer = getRequestCustomizer(
            connectionParameters, connectionDefinitionService, context);

        McpClientTransport transport = switch (transportType) {
            case HTTP_STREAMABLE -> {
                HttpClientStreamableHttpTransport.Builder builder =
                    HttpClientStreamableHttpTransport.builder(serverUrl);

                if (requestCustomizer != null) {
                    builder.customizeRequest(requestCustomizer);
                }

                yield builder.build();
            }
            case HTTP_SSE -> {
                HttpClientSseClientTransport.Builder builder = HttpClientSseClientTransport.builder(serverUrl);

                if (requestCustomizer != null) {
                    builder.customizeRequest(requestCustomizer);
                }

                yield builder.build();
            }
            default -> throw new IllegalArgumentException(
                "Unsupported MCP transport type: " + transportType +
                    ". Supported values are: " + HTTP_STREAMABLE + ", " + HTTP_SSE);
        };

        return McpClient.sync(transport)
            .build();
    }

    public static McpToolFilter createToolFilter(Parameters inputParameters) {
        String filterType = inputParameters.getString(TOOL_FILTER_TYPE, ALL);

        return switch (filterType) {
            case INCLUDE -> {
                List<String> toolsToInclude = inputParameters.getList(TOOLS_TO_INCLUDE, String.class, List.of());

                Set<String> includeSet = Set.copyOf(toolsToInclude);

                yield (connectionInfo, tool) -> includeSet.contains(tool.name());
            }
            case EXCLUDE -> {
                List<String> toolsToExclude = inputParameters.getList(TOOLS_TO_EXCLUDE, String.class, List.of());

                Set<String> excludeSet = Set.copyOf(toolsToExclude);

                yield (connectionInfo, tool) -> !excludeSet.contains(tool.name());
            }
            default -> (connectionInfo, tool) -> true;
        };
    }

    private static ModifiableValueProperty<?, ?> createProperty(
        String propertyName, Map<String, Object> propertySchema) {

        String type = (String) propertySchema.getOrDefault("type", "string");
        String description = (String) propertySchema.get("description");

        ModifiableValueProperty<?, ?> property = switch (type) {
            case "integer" -> integer(propertyName);
            case "number" -> number(propertyName);
            case "boolean" -> bool(propertyName);
            case "array" -> array(propertyName);
            case "object" -> object(propertyName);
            default -> string(propertyName);
        };

        if (description != null) {
            property.description(description);
        }

        property.label(propertyName);

        return property;
    }

    private static Consumer<HttpRequest.Builder> getRequestCustomizer(
        Parameters connectionParameters, ConnectionDefinitionService connectionDefinitionService,
        Context context) {

        if (connectionParameters == null) {
            return null;
        }

        AuthorizationType authorizationType = getAuthorizationType(connectionParameters);

        if (authorizationType == null) {
            return null;
        }

        ApplyResponse applyResponse = connectionDefinitionService.executeAuthorizationApply(
            McpClientConstants.MCP_CLIENT, 1, authorizationType, connectionParameters.toMap(), context);

        Map<String, List<String>> headers = applyResponse.getHeaders();

        if (headers.isEmpty()) {
            return null;
        }

        return requestBuilder -> {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                for (String headerValue : entry.getValue()) {
                    requestBuilder.header(entry.getKey(), headerValue);
                }
            }
        };
    }

    private static AuthorizationType getAuthorizationType(Parameters connectionParameters) {
        String token = connectionParameters.getString(TOKEN);

        if (token != null && !token.isBlank()) {
            return AuthorizationType.BEARER_TOKEN;
        }

        String accessToken = connectionParameters.getString(ACCESS_TOKEN);

        if (accessToken != null && !accessToken.isBlank()) {
            String authorizationUrl = connectionParameters.getString(Authorization.AUTHORIZATION_URL);

            if (authorizationUrl != null && !authorizationUrl.isBlank()) {
                return AuthorizationType.OAUTH2_AUTHORIZATION_CODE;
            }

            return AuthorizationType.OAUTH2_CLIENT_CREDENTIALS;
        }

        String key = connectionParameters.getString(KEY);

        if (key != null && !key.isBlank()) {
            return AuthorizationType.API_KEY;
        }

        return null;
    }
}
