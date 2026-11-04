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

import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.tool.EnvironmentScopedToolCallback;
import com.bytechef.automation.ai.tool.ProjectTools;
import com.bytechef.automation.ai.tool.ProjectWorkflowLifecycleTools;
import com.bytechef.automation.ai.tool.WorkspaceTools;
import com.bytechef.jackson.config.JacksonConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(name = "bytechef.ai.mcp.server.enabled", havingValue = "true", matchIfMissing = true)
@DependsOn(JacksonConfiguration.JSON_UTILS_CONFIGURATION)
public class ManagementMcpServerConfiguration {

    private static final Set<String> EXCLUDED_TOOL_NAMES = Set.of("getProjectStatus", "searchProjects");

    private static final Set<String> ENVIRONMENT_SCOPED_TOOL_NAMES = Set.of(
        "updateWorkflow", "saveWorkflowTestConnection");

    private final ProjectTools projectTools;
    private final ProjectWorkflowLifecycleTools projectWorkflowLifecycleTools;
    private final WorkspaceTools workspaceTools;
    private final List<McpServerToolCallbackContributor> mcpServerToolCallbackContributors;

    @SuppressFBWarnings("EI")
    public ManagementMcpServerConfiguration(
        ProjectTools projectTools, ProjectWorkflowLifecycleTools projectWorkflowLifecycleTools,
        WorkspaceTools workspaceTools, List<McpServerToolCallbackContributor> mcpServerToolCallbackContributors) {

        this.projectTools = projectTools;
        this.projectWorkflowLifecycleTools = projectWorkflowLifecycleTools;
        this.workspaceTools = workspaceTools;
        this.mcpServerToolCallbackContributors = mcpServerToolCallbackContributors;
    }

    @Bean
    WebMvcStreamableServerTransportProvider webMvcStreamableHttpServerTransportProvider() {
        return WebMvcStreamableServerTransportProvider.builder()
            .mcpEndpoint("/api/management/{secretKey}/mcp")
            .build();
    }

    @Bean
    WebMvcStreamableServerTransportProvider canonicalWebMvcStreamableHttpServerTransportProvider() {
        return WebMvcStreamableServerTransportProvider.builder()
            .mcpEndpoint("/api/management/mcp")
            .build();
    }

    @Bean
    RouterFunction<ServerResponse> mcpRouterFunction() {
        return webMvcStreamableHttpServerTransportProvider().getRouterFunction();
    }

    @Bean
    RouterFunction<ServerResponse> canonicalMcpRouterFunction() {
        return canonicalWebMvcStreamableHttpServerTransportProvider().getRouterFunction();
    }

    @Bean
    McpAsyncServer mcpAsyncServer() {
        return createMcpAsyncServer(webMvcStreamableHttpServerTransportProvider());
    }

    @Bean
    McpAsyncServer canonicalMcpAsyncServer() {
        return createMcpAsyncServer(canonicalWebMvcStreamableHttpServerTransportProvider());
    }

    ToolCallbackProvider toolCallbackProvider() {
        List<Object> tools = List.of(projectTools, projectWorkflowLifecycleTools, workspaceTools);

        List<ToolCallback> toolCallbacks = new ArrayList<>();

        for (ToolCallback toolCallback : ToolCallbacks.from(tools.toArray())) {
            ToolDefinition toolDefinition = toolCallback.getToolDefinition();

            String name = toolDefinition.name();

            if (EXCLUDED_TOOL_NAMES.contains(name)) {
                continue;
            }

            toolCallbacks.add(
                ENVIRONMENT_SCOPED_TOOL_NAMES.contains(name)
                    ? new EnvironmentScopedToolCallback(toolCallback)
                    : toolCallback);
        }

        for (McpServerToolCallbackContributor contributor : mcpServerToolCallbackContributors) {
            toolCallbacks.addAll(contributor.getToolCallbacks());
        }

        return ToolCallbackProvider.from(toolCallbacks);
    }

    private McpAsyncServer createMcpAsyncServer(WebMvcStreamableServerTransportProvider transportProvider) {
        List<ToolCallback> toolCallbacks = List.of(toolCallbackProvider().getToolCallbacks());

        List<String> toolNames = new ArrayList<>();

        for (ToolCallback toolCallback : toolCallbacks) {
            ToolDefinition toolDefinition = toolCallback.getToolDefinition();

            toolNames.add(toolDefinition.name());
        }

        return McpServer.async(transportProvider)
            .serverInfo("mcp-server", "1.0.0")
            .instructions(ManagementMcpInstructions.build(toolNames))
            .capabilities(
                McpSchema.ServerCapabilities.builder()
                    .resources(false, true)
                    .tools(true)
                    .prompts(true)
                    .logging()
                    .build())
            .tools(ToolErrorReportingToolSpecifications.toAsyncToolSpecifications(toolCallbacks))
            .build();
    }
}
