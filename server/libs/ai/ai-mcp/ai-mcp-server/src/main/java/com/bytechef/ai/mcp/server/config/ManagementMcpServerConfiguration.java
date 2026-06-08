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
import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.tool.ClusterElementTools;
import com.bytechef.automation.ai.tool.ProjectTools;
import com.bytechef.automation.ai.tool.ProjectWorkflowTools;
import com.bytechef.automation.ai.tool.ScriptTools;
import com.bytechef.platform.ai.tool.ComponentTools;
import com.bytechef.platform.ai.tool.TaskDispatcherTools;
import com.bytechef.platform.ai.tool.TaskTools;
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
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
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
 * This configuration registers a set of deterministic CE automation/platform tools directly, and folds in any
 * {@link McpServerToolCallbackContributor} beans (EE deployments contribute the Copilot subagent agent-tools). The
 * server is exposed via Streamable HTTP at /api/management/{secretKey}/mcp.
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(name = "bytechef.ai.mcp.server.enabled", havingValue = "true", matchIfMissing = true)
public class ManagementMcpServerConfiguration {

    private final ComponentTools componentTools;
    private final ProjectTools projectTools;
    private final ProjectWorkflowTools projectWorkflowTools;
    private final TaskTools taskTools;
    private final TaskDispatcherTools taskDispatcherTools;
    private final ScriptTools scriptTools;
    private final ClusterElementTools clusterElementTools;
    private final List<McpServerToolCallbackContributor> mcpServerToolCallbackContributors;

    @SuppressFBWarnings("EI")
    public ManagementMcpServerConfiguration(
        ComponentTools componentTools, ProjectTools projectTools, ProjectWorkflowTools projectWorkflowTools,
        TaskTools taskTools, TaskDispatcherTools taskDispatcherTools, ScriptTools scriptTools,
        ClusterElementTools clusterElementTools,
        List<McpServerToolCallbackContributor> mcpServerToolCallbackContributors) {

        this.componentTools = componentTools;
        this.projectTools = projectTools;
        this.projectWorkflowTools = projectWorkflowTools;
        this.taskTools = taskTools;
        this.taskDispatcherTools = taskDispatcherTools;
        this.scriptTools = scriptTools;
        this.clusterElementTools = clusterElementTools;
        this.mcpServerToolCallbackContributors = mcpServerToolCallbackContributors;
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
    McpAsyncServer mcpAsyncServer() {
        return McpServer.async(webMvcStreamableHttpServerTransportProvider())
            .serverInfo("mcp-server", "1.0.0")
            .capabilities(
                McpSchema.ServerCapabilities.builder()
                    .resources(false, true)
                    .tools(true)
                    .prompts(true)
                    .logging()
                    .build())
            .tools(McpToolUtils.toAsyncToolSpecifications(toolCallbackProvider().getToolCallbacks()))
            .build();
    }

    ToolCallbackProvider toolCallbackProvider() {
        List<Object> tools = List.of(
            projectTools, projectWorkflowTools, componentTools, taskTools, taskDispatcherTools, scriptTools,
            clusterElementTools);

        List<ToolCallback> toolCallbacks = new ArrayList<>(List.of(ToolCallbacks.from(tools.toArray())));

        for (McpServerToolCallbackContributor contributor : mcpServerToolCallbackContributors) {
            toolCallbacks.addAll(contributor.getToolCallbacks());
        }

        return ToolCallbackProvider.from(toolCallbacks);
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
