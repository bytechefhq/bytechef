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

package com.bytechef.ai.mcp.server.config;

import com.bytechef.ai.mcp.server.security.web.configurer.ManagementMcpServerSecurityConfigurer;
import com.bytechef.ai.mcp.tool.automation.impl.ProjectToolsImpl;
import com.bytechef.ai.mcp.tool.automation.impl.ProjectWorkflowToolsImpl;
import com.bytechef.ai.mcp.tool.platform.ComponentTools;
import com.bytechef.ai.mcp.tool.platform.FirecrawlTools;
import com.bytechef.ai.mcp.tool.platform.TaskTools;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.config.SecurityConfigurerContributor;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.web.servlet.function.EntityResponse;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Configuration for ByteChef MCP Server using Streamable HTTP transport.
 *
 * This configuration provides MCP tools for project and workflow management through Spring AI's auto-configured
 * Streamable HTTP MCP server. The server is automatically configured by spring-ai-starter-mcp-server-webmvc and exposes
 * tools at /api/mcp.
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(name = "bytechef.ai.mcp.server.enabled", havingValue = "true", matchIfMissing = true)
public class ManagementMcpServerConfiguration {

    private final ComponentTools componentTools;
    private final @Nullable FirecrawlTools firecrawlTools;
    private final ProjectToolsImpl projectTools;
    private final ProjectWorkflowToolsImpl projectWorkflowTools;
    private final TaskTools taskTools;

    @SuppressFBWarnings("EI")
    public ManagementMcpServerConfiguration(
        ComponentTools componentTools, @Nullable FirecrawlTools firecrawlTools, ProjectToolsImpl projectTools,
        ProjectWorkflowToolsImpl projectWorkflowTools, TaskTools taskTools) {

        this.componentTools = componentTools;
        this.firecrawlTools = firecrawlTools;
        this.projectTools = projectTools;
        this.projectWorkflowTools = projectWorkflowTools;
        this.taskTools = taskTools;
    }

    @Bean
    WebMvcStreamableServerTransportProvider webMvcStreamableHttpServerTransportProvider() {
        return WebMvcStreamableServerTransportProvider.builder()
            .mcpEndpoint("/api/management/{secretKey}/mcp")
            .build();
    }

    @Bean
    RouterFunction<ServerResponse> mcpRouterFunction() {
        Map<String, String> sessionIdCache = new ConcurrentHashMap<>();

        return webMvcStreamableHttpServerTransportProvider()
            .getRouterFunction()
            .filter((request, next) -> {
                List<MediaType> accept = request.headers()
                    .accept();
                ServerRequest workingRequest;

                if ("GET".equals(request.method()
                    .name())) {
                    // GET SSE requests need only text/event-stream — do not inject application/json
                    workingRequest = request;
                } else if (accept.contains(MediaType.TEXT_EVENT_STREAM)
                    && accept.contains(MediaType.APPLICATION_JSON)) {
                    workingRequest = request;
                } else {
                    // POST requests require both Accept types in Spring AI M3
                    workingRequest = ServerRequest.from(request)
                        .headers(headers -> headers.set(
                            HttpHeaders.ACCEPT,
                            MediaType.APPLICATION_JSON_VALUE + ", " + MediaType.TEXT_EVENT_STREAM_VALUE))
                        .build();
                }

                // Inject cached session ID for clients (e.g. supergateway) that do not forward Mcp-Session-Id
                String secretKey = request.pathVariable("secretKey");

                if (request.headers()
                    .header("Mcp-Session-Id")
                    .isEmpty()) {
                    String cachedSessionId = sessionIdCache.get(secretKey);

                    if (cachedSessionId != null) {
                        final String sessionId = cachedSessionId;

                        workingRequest = ServerRequest.from(workingRequest)
                            .headers(headers -> headers.set("Mcp-Session-Id", sessionId))
                            .build();
                    }
                }

                ServerResponse response = next.handle(workingRequest);

                // Cache the session ID returned by the initialize response
                String sessionId = response.headers()
                    .getFirst("Mcp-Session-Id");

                if (sessionId != null) {
                    sessionIdCache.put(secretKey, sessionId);

                    // Downgrade protocol version in the initialize response.
                    // supergateway (3.x) uses @modelcontextprotocol/sdk npm which only supports up to
                    // 2025-06-18; it forwards Claude Desktop's 2025-11-25 but then rejects the server
                    // response if it echoes 2025-11-25 back.
                    response = downgradeMcpProtocolVersion(response, sessionId);
                } else if (response.statusCode()
                    .value() == 404) {
                    sessionIdCache.remove(secretKey);
                }

                return response;
            });
    }

    private static ServerResponse downgradeMcpProtocolVersion(ServerResponse response, String sessionId) {
        if (!(response instanceof EntityResponse<?> entityResponse)) {
            return response;
        }

        Object entity = entityResponse.entity();

        if (!(entity instanceof McpSchema.JSONRPCResponse jsonRpcResponse)) {
            return response;
        }

        if (!(jsonRpcResponse.result() instanceof McpSchema.InitializeResult initResult)) {
            return response;
        }

        McpSchema.InitializeResult downgraded = new McpSchema.InitializeResult(
            "2025-06-18", initResult.capabilities(), initResult.serverInfo(), initResult.instructions());

        McpSchema.JSONRPCResponse newJsonRpcResponse = new McpSchema.JSONRPCResponse(
            jsonRpcResponse.jsonrpc(), jsonRpcResponse.id(), downgraded, null);

        return ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .header("Mcp-Session-Id", sessionId)
            .body(newJsonRpcResponse);
    }

    @Bean
    McpAsyncServer mcpAsyncServer(ToolCallbackProvider toolCallbackProvider) {
        return McpServer.async(webMvcStreamableHttpServerTransportProvider())
            .serverInfo("mcp-server", "1.0.0")
            .capabilities(
                McpSchema.ServerCapabilities.builder()
                    .resources(false, true)
                    .tools(true)
                    .prompts(true)
                    .logging()
                    .build())
            .tools(McpToolUtils.toAsyncToolSpecifications(toolCallbackProvider.getToolCallbacks()))
            .build();
    }

    /**
     * Provides tool callbacks for ByteChef automation tools. These tools are automatically registered with the MCP
     * server by Spring AI's auto-configuration. The MCP server is exposed via Streamable HTTP at /api/mcp endpoint.
     */
    @Bean
    @Primary
    ToolCallbackProvider toolCallbackProvider() {
        List<Object> tools = new ArrayList<>(List.of(projectTools, projectWorkflowTools, componentTools, taskTools));

        if (firecrawlTools != null) {
            tools.add(firecrawlTools);
        }

        return ToolCallbackProvider.from(
            new ArrayList<>(List.of(ToolCallbacks.from(tools.toArray()))));
    }

    @Bean
    SecurityConfigurerContributor mcpServerSecurityConfigurerContributor(
        ApiKeyService apiKeyService, AuthorityService authorityService, PropertyService propertyService,
        UserService userService) {

        return new SecurityConfigurerContributor() {

            @Override
            @SuppressWarnings("unchecked")
            public <T extends AbstractHttpConfigurer<T, B>, B extends HttpSecurityBuilder<B>> T
                getSecurityConfigurerAdapter() {

                return (T) new ManagementMcpServerSecurityConfigurer(
                    apiKeyService, authorityService, propertyService, userService);
            }
        };
    }
}
