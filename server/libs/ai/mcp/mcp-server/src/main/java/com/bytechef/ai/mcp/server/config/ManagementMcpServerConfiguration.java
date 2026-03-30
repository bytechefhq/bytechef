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
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import java.net.SocketException;
import java.net.URI;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Configuration for the ByteChef management MCP server.
 *
 * <p>Exposes ByteChef automation tools (projects, workflows, components, tasks) to MCP clients via
 * Streamable HTTP at {@code /api/management/{secretKey}/mcp}.
 *
 * <p>When {@code bytechef.ai.mcp.client.remote.url} is set, this configuration also acts as a
 * remote bridge: after startup it connects to the remote MCP server, fetches its tool list, and
 * registers every remote tool locally via {@link McpAsyncServer#addTool}. Both SSE ({@code /sse})
 * and Streamable HTTP remote endpoints are supported and auto-detected from the URL path. Bearer-
 * token authentication for the remote connection is supported via
 * {@code bytechef.ai.mcp.client.remote.auth-token}.
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(name = "bytechef.ai.mcp.server.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(McpRemoteBridgeProperties.class)
public class ManagementMcpServerConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ManagementMcpServerConfiguration.class);

    private final ComponentTools componentTools;
    private final @Nullable FirecrawlTools firecrawlTools;
    private final ProjectToolsImpl projectTools;
    private final ProjectWorkflowToolsImpl projectWorkflowTools;
    private final McpRemoteBridgeProperties remoteBridgeProperties;
    private final TaskTools taskTools;

    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    private volatile @Nullable McpAsyncClient activeRemoteClient;

    @SuppressFBWarnings("EI")
    public ManagementMcpServerConfiguration(
        ComponentTools componentTools, @Nullable FirecrawlTools firecrawlTools, ProjectToolsImpl projectTools,
        ProjectWorkflowToolsImpl projectWorkflowTools, McpRemoteBridgeProperties remoteBridgeProperties,
        TaskTools taskTools) {

        this.componentTools = componentTools;
        this.firecrawlTools = firecrawlTools;
        this.projectTools = projectTools;
        this.projectWorkflowTools = projectWorkflowTools;
        this.remoteBridgeProperties = remoteBridgeProperties;
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
        return webMvcStreamableHttpServerTransportProvider().getRouterFunction();
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
     * Creates the async MCP client for the remote bridge. Initialisation is deferred to
     * {@link #onApplicationReady} so the remote URL can safely point to the same ByteChef instance
     * without hitting a "not yet listening" failure at bean-creation time.
     */
    @Bean("remoteMcpClient")
    @ConditionalOnProperty(prefix = "bytechef.ai.mcp.client.remote", name = "url")
    McpAsyncClient remoteMcpClient() {
        String remoteUrl = Objects.requireNonNull(
            remoteBridgeProperties.getUrl(), "bytechef.ai.mcp.client.remote.url must be set");
        String authToken = remoteBridgeProperties.getAuthToken();

        URI serverUri = URI.create(remoteUrl);
        String baseUrl = serverUri.getScheme() + "://" + serverUri.getAuthority();
        String endpoint = serverUri.getRawPath();

        Consumer<java.net.http.HttpRequest.Builder> authCustomizer = StringUtils.hasText(authToken)
            ? requestBuilder -> requestBuilder.header("Authorization", "Bearer " + authToken)
            : _ -> {};

        McpClientTransport transport;

        if (endpoint.endsWith("/sse")) {
            transport = HttpClientSseClientTransport.builder(baseUrl)
                .sseEndpoint(endpoint)
                .customizeRequest(authCustomizer)
                .build();
        } else {
            transport = HttpClientStreamableHttpTransport.builder(baseUrl)
                .endpoint(endpoint)
                .connectTimeout(remoteBridgeProperties.getConnectTimeout())
                .customizeRequest(authCustomizer)
                .build();
        }

        return McpClient.async(transport)
            .clientInfo(new McpSchema.Implementation("bytechef-mcp-bridge", "1.0.0"))
            .requestTimeout(remoteBridgeProperties.getRequestTimeout())
            .build();
    }

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

    /**
     * After the application is fully started, initialises the remote MCP client, fetches its tool
     * list, and registers each tool into the local {@link McpAsyncServer}. Connected MCP clients
     * are notified of the updated tool list automatically.
     */
    @EventListener
    void onApplicationReady(ApplicationReadyEvent event) {
        ConfigurableApplicationContext applicationContext = event.getApplicationContext();

        if (!applicationContext.containsBean("remoteMcpClient")) {
            return;
        }

        McpAsyncClient remoteClient = applicationContext.getBean("remoteMcpClient", McpAsyncClient.class);
        McpAsyncServer server = applicationContext.getBean("mcpAsyncServer", McpAsyncServer.class);

        try {
            remoteClient.initialize()
                .block(remoteBridgeProperties.getConnectTimeout());
        } catch (Exception exception) {
            logger.warn(
                "Failed to initialise remote MCP client at '{}': {}",
                remoteBridgeProperties.getUrl(), exception.getMessage());

            return;
        }

        activeRemoteClient = remoteClient;

        List<McpServerFeatures.AsyncToolSpecification> bridgedTools = listRemoteToolSpecs(remoteClient);

        if (bridgedTools.isEmpty()) {
            return;
        }

        for (McpServerFeatures.AsyncToolSpecification toolSpec : bridgedTools) {
            server.addTool(toolSpec)
                .block(remoteBridgeProperties.getRequestTimeout());
        }

        server.notifyToolsListChanged()
            .block(remoteBridgeProperties.getRequestTimeout());

        logger.info(
            "Bridged {} tool(s) from remote MCP server at '{}'",
            bridgedTools.size(), remoteBridgeProperties.getUrl());
    }

    private List<McpServerFeatures.AsyncToolSpecification> listRemoteToolSpecs(McpAsyncClient remoteClient) {
        try {
            McpSchema.ListToolsResult result = remoteClient.listTools()
                .block(remoteBridgeProperties.getRequestTimeout());

            if (result == null || result.tools().isEmpty()) {
                return List.of();
            }

            return result.tools()
                .stream()
                .map(tool -> new McpServerFeatures.AsyncToolSpecification(
                    tool, (_, request) -> callToolWithReconnect(request)))
                .toList();
        } catch (Exception exception) {
            logger.warn(
                "Failed to list tools from remote MCP server at '{}': {}",
                remoteBridgeProperties.getUrl(), exception.getMessage());

            return List.of();
        }
    }

    private Mono<McpSchema.CallToolResult> callToolWithReconnect(McpSchema.CallToolRequest request) {
        McpAsyncClient currentClient = activeRemoteClient;

        if (currentClient == null) {
            return Mono.error(new RuntimeException("Remote MCP client is not available"));
        }

        return currentClient.callTool(request)
            .onErrorResume(
                ManagementMcpServerConfiguration::isConnectionError,
                error -> {
                    logger.warn(
                        "Remote MCP connection lost, attempting to reconnect to '{}': {}",
                        remoteBridgeProperties.getUrl(), error.getMessage());

                    if (!reconnecting.compareAndSet(false, true)) {
                        return Mono.error(
                            new RuntimeException("Remote MCP reconnection already in progress, please retry later"));
                    }

                    McpAsyncClient newClient = remoteMcpClient();

                    return newClient.initialize()
                        .doOnSuccess(ignored -> activeRemoteClient = newClient)
                        .doFinally(ignored -> reconnecting.set(false))
                        .then(Mono.defer(() -> newClient.callTool(request)))
                        .doOnError(retryError -> logger.error(
                            "Failed to reconnect to remote MCP server at '{}': {}",
                            remoteBridgeProperties.getUrl(), retryError.getMessage()));
                });
    }

    private static boolean isConnectionError(Throwable error) {
        Throwable current = error;

        while (current != null) {
            if (current instanceof ClosedChannelException || current instanceof SocketException) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }
}
