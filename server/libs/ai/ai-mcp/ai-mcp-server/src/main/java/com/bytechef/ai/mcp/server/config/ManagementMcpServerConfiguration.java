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
import com.bytechef.automation.ai.tool.ProjectWorkflowTools;
import com.bytechef.automation.ai.tool.ScriptTools;
import com.bytechef.platform.ai.tool.ComponentTools;
import com.bytechef.platform.ai.tool.TaskDispatcherTools;
import com.bytechef.platform.ai.tool.TaskTools;
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

    private static final String INSTRUCTIONS =
        """
            ByteChef management server. Two kinds of tools: ordinary tools are deterministic CRUD — call them \
            directly; intelligent tools (buildWorkflow, importWorkflow, configureClusterElement, \
            writeScript, authorSkill, debugWorkflowExecution, configureMcpServer) run an inner AI agent and \
            may take minutes — call them for judgment work, not for CRUD.
            To build a workflow: createProject (if needed) → createProjectWorkflow → buildWorkflow with \
            the workflowId and a plain-language instruction. To import an external workflow (n8n, Make, \
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
            listMcpServers, createMcpServer and listMcpProjectWorkflows additionally accept an optional \
            workspaceId (and environment) since MCP servers are workspace-scoped: omit it when the account \
            has exactly one \
            workspace, otherwise retry with an explicit workspaceId from the workspace_required error's \
            'workspaces' field.
            The project, workflow and script tools (listProjects, searchProjects, getProject, createProject, \
            listWorkflows, searchWorkflows, getWorkflow, createProjectWorkflow, updateWorkflow, \
            updateScriptComponentCode, updateWorkflowRootProperties, updateClusterElementTask and the rest) \
            likewise act within a single workspace and accept an \
            optional workspaceId: they only list projects and workflows of that workspace and reject a project \
            or workflow id that belongs to another one.
            Most tools require workspace context: if a tool returns a workspace_required error, retry with one \
            of the workspaceId values listed in its 'workspaces' field.""";

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
            .instructions(INSTRUCTIONS)
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
}
