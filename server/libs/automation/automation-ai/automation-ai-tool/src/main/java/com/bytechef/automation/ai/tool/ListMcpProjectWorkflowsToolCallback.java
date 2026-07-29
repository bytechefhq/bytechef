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

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.ai.tool.constant.ToolConstants;
import com.bytechef.platform.component.constant.WorkflowConstants;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.definition.WorkflowNodeType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that lists the workflows attached to an MCP server (through its MCP projects),
 * including each workflow's tool-mapping state: the {@code toolName} / {@code toolDescription} exposed to MCP clients,
 * the current parameter map (whose values may be {@code fromAi(...)} expressions), and the trigger's declared input
 * schema. Read leg paired with {@link UpdateMcpProjectWorkflowParametersToolCallback} — without this list the LLM has
 * no way to see which attached workflows still lack a tool name or fromAi parameter mapping.
 *
 * <p>
 * A workflow with {@code toolCallable = false} has no {@code workflow/newWorkflowCall} trigger and can never serve as
 * an MCP tool regardless of its parameters; the LLM should surface that to the user instead of trying to fix it via
 * parameters.
 * </p>
 *
 *
 * @author Ivica Cardic
 */
public class ListMcpProjectWorkflowsToolCallback implements ToolCallback {

    static final String TOOL_NAME = "listMcpProjectWorkflows";

    private static final String DESCRIPTION = """
        List the workflows attached to an MCP server (via its MCP projects) together with their MCP tool
        mapping state. Returns a JSON array of {mcpProjectId, mcpProjectWorkflowId, workflowId,
        workflowLabel, toolCallable, toolName, toolDescription, parameters, inputSchema} entries.
        toolName/toolDescription are what MCP clients will see; parameters holds per-input values where a
        value may be a fromAi(...) expression string; inputSchema is the JSON schema the workflow's
        callable trigger declares for its input. A row with toolCallable=false cannot be exposed as a tool
        at all (its workflow lacks a callable trigger). A row with a null toolName is attached but NOT yet
        servable — complete it with updateMcpProjectWorkflowParameters.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "mcpServerId": {"type": "integer", "description": "Numeric MCP server id from listMcpServers"}
            },
            "required": ["mcpServerId"]
        }""";

    private final McpProjectService mcpProjectService;
    private final McpProjectWorkflowService mcpProjectWorkflowService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final WorkflowService workflowService;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ListMcpProjectWorkflowsToolCallback(
        McpProjectService mcpProjectService, McpProjectWorkflowService mcpProjectWorkflowService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, WorkflowService workflowService) {

        this.mcpProjectService = mcpProjectService;
        this.mcpProjectWorkflowService = mcpProjectWorkflowService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.workflowService = workflowService;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name(TOOL_NAME)
            .description(DESCRIPTION)
            .inputSchema(INPUT_SCHEMA)
            .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        try {
            ListMcpProjectWorkflowsInput input = jsonMapper.readValue(toolInput, ListMcpProjectWorkflowsInput.class);

            if (input.mcpServerId() == null) {
                return ToolErrors.toolError(jsonMapper, "mcpServerId is required");
            }

            List<McpProjectWorkflowSummary> summaries = new ArrayList<>();

            for (McpProject mcpProject : mcpProjectService.getMcpServerMcpProjects(input.mcpServerId())) {
                for (McpProjectWorkflow mcpProjectWorkflow : mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(
                    mcpProject.getId())) {

                    summaries.add(toSummary(mcpProject, mcpProjectWorkflow));
                }
            }

            return jsonMapper.writeValueAsString(summaries);
        } catch (JacksonException exception) {
            return ToolErrors.toolError(jsonMapper, "Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return ToolErrors.toolError(jsonMapper, exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, ListMcpProjectWorkflowsToolCallback.class, TOOL_NAME, exception);
        }
    }

    private McpProjectWorkflowSummary toSummary(McpProject mcpProject, McpProjectWorkflow mcpProjectWorkflow) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowService
            .getProjectDeploymentWorkflow(mcpProjectWorkflow.getProjectDeploymentWorkflowId());

        Workflow workflow = workflowService.getWorkflow(projectDeploymentWorkflow.getWorkflowId());

        WorkflowTrigger callableTrigger = getMcpToolCallableTrigger(workflow);

        String inputSchema = callableTrigger == null
            ? null
            : MapUtils.getString(callableTrigger.getParameters(), WorkflowConstants.INPUT_SCHEMA);

        Map<String, ?> parameters = mcpProjectWorkflow.getParameters();

        return new McpProjectWorkflowSummary(
            mcpProject.getId(), mcpProjectWorkflow.getId(), workflow.getId(), workflow.getLabel(),
            callableTrigger != null, MapUtils.getString(parameters, ToolConstants.TOOL_NAME),
            MapUtils.getString(parameters, ToolConstants.TOOL_DESCRIPTION), parameters, inputSchema);
    }

    private static @Nullable WorkflowTrigger getMcpToolCallableTrigger(Workflow workflow) {
        for (WorkflowTrigger workflowTrigger : WorkflowTrigger.of(workflow)) {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            if (Objects.equals(workflowNodeType.name(), WorkflowConstants.WORKFLOW) &&
                Objects.equals(workflowNodeType.operation(), WorkflowConstants.NEW_WORKFLOW_CALL)) {

                return workflowTrigger;
            }
        }

        return null;
    }

    public record ListMcpProjectWorkflowsInput(@Nullable Long mcpServerId) {
    }

    @SuppressFBWarnings({
        "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
    })
    public record McpProjectWorkflowSummary(
        Long mcpProjectId, Long mcpProjectWorkflowId, String workflowId, @Nullable String workflowLabel,
        boolean toolCallable, @Nullable String toolName, @Nullable String toolDescription,
        Map<String, ?> parameters, @Nullable String inputSchema) {
    }
}
