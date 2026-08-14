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

package com.bytechef.automation.ai.tool;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.mcp.facade.McpProjectFacade;
import com.bytechef.automation.ai.mcp.facade.WorkspaceMcpServerFacade;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.tool.ToolCallback;

/**
 * Builds the MCP Server tool-callback lists shared by the Copilot panel agents ({@code mcp_server_ask} /
 * {@code mcp_server_build}). Read list feeds ASK; write list feeds BUILD.
 *
 * <p>
 * The read leg spans two tool callbacks with different dependencies: {@link ListMcpServersToolCallback} needs only
 * {@link WorkspaceMcpServerFacade}, while {@link ListMcpProjectWorkflowsToolCallback} needs {@link McpProjectService},
 * {@link McpProjectWorkflowService}, {@link ProjectDeploymentWorkflowService}, and {@link WorkflowService}. This
 * factory therefore carries all six collaborators even though the write leg only adds two more
 * ({@link McpProjectFacade}-backed tools).
 * </p>
 *
 * <p>
 * This factory is additive: {@link McpServerSubAgentConfiguration}'s existing {@code mcpAgentChatClient} bean keeps
 * constructing its own tool list independently, since it backs the {@code mcp_agent} subagent consumed by AI Hub and
 * the management MCP server and must not change behaviour.
 * </p>
 *
 * @author Ivica Cardic
 */
public class McpServerToolCallbacksFactory {

    private final McpProjectFacade mcpProjectFacade;
    private final McpProjectService mcpProjectService;
    private final McpProjectWorkflowService mcpProjectWorkflowService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final WorkflowService workflowService;
    private final WorkspaceMcpServerFacade workspaceMcpServerFacade;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public McpServerToolCallbacksFactory(
        McpProjectFacade mcpProjectFacade, McpProjectService mcpProjectService,
        McpProjectWorkflowService mcpProjectWorkflowService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, WorkflowService workflowService,
        WorkspaceMcpServerFacade workspaceMcpServerFacade) {

        this.mcpProjectFacade = mcpProjectFacade;
        this.mcpProjectService = mcpProjectService;
        this.mcpProjectWorkflowService = mcpProjectWorkflowService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.workflowService = workflowService;
        this.workspaceMcpServerFacade = workspaceMcpServerFacade;
    }

    public List<ToolCallback> readToolCallbacks() {
        List<ToolCallback> toolCallbacks = new ArrayList<>();

        toolCallbacks.add(new ListMcpServersToolCallback(workspaceMcpServerFacade));
        toolCallbacks.add(new ListMcpProjectWorkflowsToolCallback(
            mcpProjectService, mcpProjectWorkflowService, projectDeploymentWorkflowService, workflowService));

        return toolCallbacks;
    }

    public List<ToolCallback> writeToolCallbacks() {
        List<ToolCallback> toolCallbacks = new ArrayList<>(readToolCallbacks());

        toolCallbacks.add(new CreateMcpServerToolCallback(workspaceMcpServerFacade));
        toolCallbacks.add(new UpdateMcpServerToolCallback(workspaceMcpServerFacade));
        toolCallbacks.add(new CreateMcpProjectToolCallback(mcpProjectFacade));
        toolCallbacks.add(new CloneMcpProjectToolCallback(mcpProjectFacade));
        toolCallbacks.add(new UpdateMcpProjectWorkflowParametersToolCallback(mcpProjectWorkflowService));

        return toolCallbacks;
    }
}
