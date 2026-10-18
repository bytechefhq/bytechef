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
import com.bytechef.automation.ai.tool.ClusterElementTools;
import com.bytechef.automation.ai.tool.ProjectTools;
import com.bytechef.automation.ai.tool.ProjectWorkflowLifecycleTools;
import com.bytechef.automation.ai.tool.ScriptTools;
import com.bytechef.automation.ai.tool.WorkspaceTools;
import com.bytechef.platform.ai.tool.ComponentTools;
import com.bytechef.platform.ai.tool.TaskDispatcherTools;
import com.bytechef.platform.ai.tool.TaskTools;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Configuration for ByteChef MCP Server using Streamable HTTP transport.
 *
 * This configuration registers a set of deterministic CE automation/platform tools directly, and folds in any
 * {@link McpServerToolCallbackContributor} beans (EE deployments contribute the Copilot subagent agent-tools). The
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(name = "bytechef.ai.mcp.server.enabled", havingValue = "true", matchIfMissing = true)
public class ManagementMcpServerConfiguration {

    private static final String INSTRUCTIONS =
        """
            ByteChef management server. Two kinds of tools: ordinary tools are deterministic CRUD — call them \
            directly; intelligent tools (buildWorkflow, importWorkflow, configureClusterElement, \
            writeScript, authorSkill, debugWorkflowExecution, configureMcpServer) run an inner AI agent and \
            may take minutes — call them for judgment work, not for CRUD.
            To build a workflow: createProject (if needed) → createProjectWorkflow → buildWorkflow with \
            the workflowId and a plain-language instruction. createProjectWorkflow creates an EMPTY workflow \
            (manual trigger, no tasks) from a name and description; no tool writes a whole workflow definition \
            directly - buildWorkflow and importWorkflow are the only ways to author content. To import an \
            external workflow (n8n, Make, \
            Zapier, Workato): createProject (if needed) → createProjectWorkflow → importWorkflow with the \
            workflowId and the source definition — it has no project/workflow-creation tools of its own. \
            Each intelligent-tool call is independent and re-reads current state (e.g. the workflow) rather \
            than remembering earlier calls, so iterate by \
            calling the tool again with the next instruction and restate any context it still needs.
            To expose workflows over MCP: createMcpServer(name, environment, enabled?) — leave enabled unset \
            or false, since a server with any unmapped attached workflow cannot be enabled — then \
            createMcpProject(mcpServerId, projectId, projectVersion, workflowIds) to attach a published \
            project version's workflows (the server must already exist; this tool does not create one), then \
            configureMcpServer(mcpServerId) to synthesize each attached workflow's tool mapping (tool name, \
            tool description, fromAi(...) input expressions), then updateMcpServer(mcpServerId, enabled=true) \
            to bring it online. That last call FAILS with a typed error naming the still-unmapped workflows \
            if any attached workflow lacks a toolName or a required fromAi mapping — that is the enable-guard \
            working as intended, not a bug; complete the mapping and retry. listMcpServers resolves a \
            user-named server to its numeric id, cloneMcpProject(mcpProjectId, targetMcpServerId) duplicates \
            an existing MCP project's exposed workflows onto another server, and \
            listMcpProjectWorkflows(mcpServerId) shows each attached workflow's current mapping state. \
            configureMcpServer never creates the server, attaches workflows to it, or enables it — those are \
            the flat tools above.
            Tools that work within a workspace (listProjects, searchProjects, createProject, \
            searchWorkflows without a projectId, and the MCP server, deployment and knowledge-base tools) take \
            a workspaceId, and several also take an environment (DEVELOPMENT, STAGING or PRODUCTION, default \
            DEVELOPMENT). Call listWorkspaces first to find the workspace id. Tools that take a project or \
            workflow id act on that object directly.""";

    private final ComponentTools componentTools;
    private final ProjectTools projectTools;
    private final ProjectWorkflowLifecycleTools projectWorkflowLifecycleTools;
    private final TaskTools taskTools;
    private final TaskDispatcherTools taskDispatcherTools;
    private final ScriptTools scriptTools;
    private final ClusterElementTools clusterElementTools;
    private final WorkspaceTools workspaceTools;
    private final List<McpServerToolCallbackContributor> mcpServerToolCallbackContributors;

    @SuppressFBWarnings("EI")
    public ManagementMcpServerConfiguration(
        ComponentTools componentTools, ProjectTools projectTools,
        ProjectWorkflowLifecycleTools projectWorkflowLifecycleTools,
        TaskTools taskTools, TaskDispatcherTools taskDispatcherTools, ScriptTools scriptTools,
        ClusterElementTools clusterElementTools, WorkspaceTools workspaceTools,
        List<McpServerToolCallbackContributor> mcpServerToolCallbackContributors) {

        this.componentTools = componentTools;
        this.projectTools = projectTools;
        this.projectWorkflowLifecycleTools = projectWorkflowLifecycleTools;
        this.taskTools = taskTools;
        this.taskDispatcherTools = taskDispatcherTools;
        this.scriptTools = scriptTools;
        this.clusterElementTools = clusterElementTools;
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

    List<McpServerFeatures.AsyncToolSpecification> toolSpecifications() {
        return ToolErrorReportingToolSpecifications.toAsyncToolSpecifications(
            List.of(toolCallbackProvider().getToolCallbacks()));
    }

    ToolCallbackProvider toolCallbackProvider() {
        List<Object> tools = List.of(
            projectTools, projectWorkflowLifecycleTools, componentTools, taskTools, taskDispatcherTools, scriptTools,
            clusterElementTools, workspaceTools);

        List<ToolCallback> toolCallbacks = new ArrayList<>(List.of(ToolCallbacks.from(tools.toArray())));

        for (McpServerToolCallbackContributor contributor : mcpServerToolCallbackContributors) {
            toolCallbacks.addAll(contributor.getToolCallbacks());
        }

        return ToolCallbackProvider.from(toolCallbacks);
    }

    private McpAsyncServer createMcpAsyncServer(WebMvcStreamableServerTransportProvider transportProvider) {
        return McpServer.async(transportProvider)
            .serverInfo("mcp-server", "1.0.0")
            .instructions(INSTRUCTIONS)
            .capabilities(
                McpSchema.ServerCapabilities.builder()
                    .resources(false, true)
                    .tools(true)
                    .prompts(true)
                    .logging()
                    .build())
            .tools(toolSpecifications())
            .build();
    }
}
