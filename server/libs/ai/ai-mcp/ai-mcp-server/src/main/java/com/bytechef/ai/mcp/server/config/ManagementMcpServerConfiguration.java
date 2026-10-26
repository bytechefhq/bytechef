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
import com.bytechef.automation.ai.tool.ProjectTools;
import com.bytechef.automation.ai.tool.ProjectWorkflowLifecycleTools;
import com.bytechef.automation.ai.tool.WorkspaceTools;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
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

    static final String INSTRUCTIONS =
        """
            ByteChef management server. Two kinds of tools: ordinary tools are deterministic CRUD; call them \
            directly. Intelligent tools (buildWorkflow, importWorkflow, authorSkill, debugWorkflowExecution, \
            configureMcpServer, buildAiAgent) run an inner AI agent and may take minutes; call them for \
            judgment work, not for CRUD.
            To build a workflow: createProject (if needed) → buildWorkflow with a plain-language instruction \
            and the projectId for a new workflow or the workflowId for an existing one. To import an external \
            workflow (n8n, Make, Zapier, Workato): createProject (if needed) → importWorkflow with the source \
            definition and the projectId for a new workflow or the workflowId for an existing one. \
            buildWorkflow and importWorkflow are the only ways to create or author workflow content: they \
            create the workflow themselves and look up components, actions, triggers and task dispatchers \
            themselves, so there are no separate tools for those. Each intelligent-tool call is independent \
            and re-reads current state (e.g. the workflow) rather than remembering earlier calls, so iterate \
            by calling the tool again with the next instruction and restate any context it still needs.
            To find something, listProjects and listWorkflows take an optional query that matches names and \
            descriptions; searchWorkflows without a projectId searches the workflows of every project in a \
            workspace. getProject also reports publication state, the last version and deployments. \
            Skills are read-only here (getAiSkills, getAiSkill, getAiSkillFilePaths, getAiSkillFileContent); \
            author or change a skill with authorSkill.
            To build an AI Agent (a chat-first automation that runs on a model instead of a hand-built \
            workflow): createProject (if needed) → buildAiAgent with a plain-language instruction and the \
            projectId for a new agent or the aiAgentId for an existing one; it looks up tools and \
            connections itself. For precise edits use the agent tools directly: listAiAgents, getAiAgent, \
            createAiAgent, updateAiAgent, add/update/deleteAiAgentElement, add/deleteAiAgentChannel, \
            updateAiAgentSettings, and searchAiAgentTools to find the componentName, componentVersion and \
            actionName of a TOOL element and searchAiAgentReferences to find the referenceId of a skill or knowledge \
            base. An agent's MODEL element needs a connectionId.
            To expose workflows over MCP: createMcpServer(name, environment, enabled?), leaving enabled unset \
            or false, since a server with any unmapped attached workflow cannot be enabled; then \
            createMcpProject(mcpServerId, projectId, projectVersion, workflowIds) to attach a published \
            project version's workflows (the server must already exist); then configureMcpServer(mcpServerId) \
            to synthesize each attached workflow's tool mapping (tool name, tool description, fromAi(...) \
            input expressions); then updateMcpServer(mcpServerId, enabled=true) to bring it online. That last \
            call FAILS with a typed error naming the still-unmapped workflows if any attached workflow lacks a \
            toolName or a required fromAi mapping; that is the enable-guard working as intended, not a bug, \
            so call configureMcpServer again and retry. listMcpServers resolves a user-named server to its \
            numeric id. configureMcpServer never creates the server, attaches workflows to it, or enables it; \
            those are the flat tools above.
            Tools that work within a workspace (listProjects, createProject, searchWorkflows without a \
            projectId, listAiAgents, createAiAgent, and the MCP server, \
            deployment and knowledge-base tools) take a workspaceId, and several also take an environment (DEVELOPMENT, \
            STAGING or PRODUCTION, default DEVELOPMENT). Call listWorkspaces first to find the workspace id. \
            Tools that take a project or workflow id act on that object directly.""";

    private static final Set<String> EXCLUDED_TOOL_NAMES = Set.of(
        "createProjectWorkflow", "getProjectStatus", "searchProjects");

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

    List<McpServerFeatures.AsyncToolSpecification> toolSpecifications() {
        return ToolErrorReportingToolSpecifications.toAsyncToolSpecifications(
            List.of(toolCallbackProvider().getToolCallbacks()));
    }

    ToolCallbackProvider toolCallbackProvider() {
        List<Object> tools = List.of(projectTools, projectWorkflowLifecycleTools, workspaceTools);

        List<ToolCallback> toolCallbacks = new ArrayList<>();

        for (ToolCallback toolCallback : ToolCallbacks.from(tools.toArray())) {
            ToolDefinition toolDefinition = toolCallback.getToolDefinition();

            if (!EXCLUDED_TOOL_NAMES.contains(toolDefinition.name())) {
                toolCallbacks.add(toolCallback);
            }
        }

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
