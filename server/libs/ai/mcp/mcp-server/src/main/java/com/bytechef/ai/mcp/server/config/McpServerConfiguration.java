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

import com.bytechef.ai.mcp.server.security.web.config.McpServerAuthorizeHttpRequestContributor;
import com.bytechef.ai.mcp.server.security.web.config.McpServerCsrfContributor;
import com.bytechef.ai.mcp.tool.automation.ProjectTools;
import com.bytechef.ai.mcp.tool.automation.ProjectWorkflowTools;
import com.bytechef.ai.mcp.tool.platform.TaskTools;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for ByteChef MCP Server using Streamable HTTP transport.
 *
 * This configuration provides MCP tools for project and workflow management through
 * Spring AI's auto-configured Streamable HTTP MCP server. The server is automatically
 * configured by spring-ai-starter-mcp-server-webmvc and exposes tools at /api/mcp.
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(name = "bytechef.mcp.server.enabled", havingValue = "true", matchIfMissing = true)
public class McpServerConfiguration {

    private final ProjectTools projectTools;
    private final ProjectWorkflowTools projectWorkflowTools;
    private final TaskTools taskTools;

    @SuppressFBWarnings("EI")
    public McpServerConfiguration(
        ProjectTools projectTools, ProjectWorkflowTools projectWorkflowTools, TaskTools taskTools) {

        this.projectTools = projectTools;
        this.projectWorkflowTools = projectWorkflowTools;
        this.taskTools = taskTools;
    }

    /**
     * Provides tool callbacks for ByteChef automation tools.
     * These tools are automatically registered with the MCP server by Spring AI's auto-configuration.
     * The MCP server is exposed via Streamable HTTP at /api/mcp endpoint.
     */
    @Bean
    ToolCallbackProvider toolCallbackProvider() {
        return ToolCallbackProvider.from(ToolCallbacks.from(projectTools, projectWorkflowTools, taskTools));
    }

    @Bean
    McpServerAuthorizeHttpRequestContributor mcpServerAuthorizeHttpRequestContributor() {
        return new McpServerAuthorizeHttpRequestContributor();
    }

    @Bean
    McpServerCsrfContributor mcpServerCsrfContributor() {
        return new McpServerCsrfContributor();
    }
}
