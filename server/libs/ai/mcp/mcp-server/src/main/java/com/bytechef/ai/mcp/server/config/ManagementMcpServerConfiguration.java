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
import com.bytechef.ai.mcp.tool.platform.TaskTools;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.config.SecurityConfigurerContributor;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.WebMvcStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.web.servlet.function.RouterFunction;
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
@ConditionalOnProperty(name = "bytechef.mcp.server.enabled", havingValue = "true", matchIfMissing = true)
public class ManagementMcpServerConfiguration {

    private final ProjectToolsImpl projectTools;
    private final ProjectWorkflowToolsImpl projectWorkflowTools;
    private final TaskTools taskTools;

    @SuppressFBWarnings("EI")
    public ManagementMcpServerConfiguration(
        ProjectToolsImpl projectTools, ProjectWorkflowToolsImpl projectWorkflowTools, TaskTools taskTools) {

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
     * Provides tool callbacks for ByteChef automation tools. These tools are automatically registered with the MCP
     * server by Spring AI's auto-configuration. The MCP server is exposed via Streamable HTTP at /api/mcp endpoint.
     */
    @Bean
    ToolCallbackProvider toolCallbackProvider() {
        return ToolCallbackProvider.from(ToolCallbacks.from(projectTools, projectWorkflowTools, taskTools));
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
