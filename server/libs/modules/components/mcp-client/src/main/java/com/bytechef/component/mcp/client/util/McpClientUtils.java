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

import static com.bytechef.component.definition.Authorization.HEADER_PREFIX;
import static com.bytechef.component.definition.Authorization.KEY;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.Authorization.VALUE;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.ALL;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.EXCLUDE;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.HTTP_STREAMABLE;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.INCLUDE;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.TOOLS_TO_EXCLUDE;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.TOOLS_TO_INCLUDE;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.TOOL_FILTER_TYPE;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.TRANSPORT_TYPE;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.URL;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import java.net.http.HttpRequest;
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
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, ActionContext context) {

        try (McpSyncClient mcpSyncClient = createMcpSyncClient(inputParameters, connectionParameters)) {
            mcpSyncClient.initialize();

            List<McpSchema.Tool> tools = mcpSyncClient.listTools()
                .tools();

            return tools.stream()
                .map(tool -> (Option<String>) option(tool.name(), tool.name()))
                .toList();
        }
    }

    public static McpSyncClient createMcpSyncClient(Parameters inputParameters) {
        return createMcpSyncClient(inputParameters, null);
    }

    public static McpSyncClient createMcpSyncClient(
        Parameters inputParameters, Parameters connectionParameters) {

        String serverUrl = inputParameters.getRequiredString(URL);
        String transportType = inputParameters.getString(TRANSPORT_TYPE, HTTP_STREAMABLE);

        Consumer<HttpRequest.Builder> requestCustomizer = getRequestCustomizer(connectionParameters);

        McpClientTransport transport;

        if (HTTP_STREAMABLE.equals(transportType)) {
            HttpClientStreamableHttpTransport.Builder builder =
                HttpClientStreamableHttpTransport.builder(serverUrl);

            if (requestCustomizer != null) {
                builder.customizeRequest(requestCustomizer);
            }

            transport = builder.build();
        } else {
            HttpClientSseClientTransport.Builder builder = HttpClientSseClientTransport.builder(serverUrl);

            if (requestCustomizer != null) {
                builder.customizeRequest(requestCustomizer);
            }

            transport = builder.build();
        }

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

    private static Consumer<HttpRequest.Builder> getRequestCustomizer(Parameters connectionParameters) {
        if (connectionParameters == null) {
            return null;
        }

        String token = connectionParameters.getString(TOKEN);

        if (token != null && !token.isBlank()) {
            String headerPrefix = connectionParameters.getString(HEADER_PREFIX, Authorization.BEARER);

            return requestBuilder -> requestBuilder.header("Authorization", headerPrefix + " " + token);
        }

        String headerName = connectionParameters.getString(KEY);
        String headerValue = connectionParameters.getString(VALUE);

        if (headerName != null && !headerName.isBlank() && headerValue != null) {
            return requestBuilder -> requestBuilder.header(headerName, headerValue);
        }

        return null;
    }
}
