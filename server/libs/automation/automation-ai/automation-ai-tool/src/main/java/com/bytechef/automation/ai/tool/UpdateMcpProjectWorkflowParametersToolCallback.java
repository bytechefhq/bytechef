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
import com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.ai.tool.constant.ToolConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that completes an attached MCP workflow's tool mapping: the {@code toolName} /
 * {@code toolDescription} shown to MCP clients plus the per-input parameter values, where a value may be a
 * {@code fromAi('name', 'TYPE', {description: ..., required: ...})} expression string marking an input the calling
 * agent must supply. The MCP server derives each tool's JSON input schema from these expressions at serve time, so this
 * mapping — not the workflow definition — is what shapes the tool contract.
 *
 * <p>
 * Merge semantics: the callback reads the existing parameter map and overlays only the supplied fields, so a follow-up
 * call that fixes one input does not wipe the rest. Authorization rides on
 * {@link McpProjectWorkflowService#updateParameters} ({@code MCP_EDIT} permission on the row).
 * </p>
 *
 *
 * @author Ivica Cardic
 */
public class UpdateMcpProjectWorkflowParametersToolCallback implements ToolCallback {

    static final String TOOL_NAME = "updateMcpProjectWorkflowParameters";

    private static final String DESCRIPTION = """
        Set or update the MCP tool mapping of a workflow attached to an MCP server. Supply
        mcpProjectWorkflowId (from listMcpProjectWorkflows) and any of: toolName (snake_case identifier
        MCP clients call), toolDescription (what the tool does, shown to the calling model), parameters
        (object of per-input values keyed by the input names from the workflow's inputSchema). A parameter
        value may be a literal, or a fromAi(...) expression string marking an input the CALLING agent
        supplies at invocation time, e.g. "fromAi('city', 'STRING', {description: 'City to fetch weather
        for', required: true})". Valid fromAi types: STRING, NUMBER, INTEGER, BOOLEAN, ARRAY, OBJECT,
        DATE, TIME, DATE_TIME; the options entry (a list) renders as an enum. Only supplied fields are
        changed — existing parameters are preserved. A workflow without a toolName is never served as a
        tool. Returns the updated mapping or {error: <message>} on failure.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "mcpProjectWorkflowId": {
                        "type": "integer",
                        "description": "Numeric MCP project workflow id from listMcpProjectWorkflows"
                    },
                    "toolName": {
                        "type": "string",
                        "description": "Tool identifier exposed to MCP clients (snake_case, e.g. get_weather)"
                    },
                    "toolDescription": {
                        "type": "string",
                        "description": "Tool description exposed to MCP clients"
                    },
                    "parameters": {
                        "type": "object",
                        "description": "Per-input values keyed by input name; a value may be a fromAi(...) expression string",
                        "additionalProperties": true
                    }
                },
                "required": ["mcpProjectWorkflowId"]
            }""";

    private final McpProjectWorkflowService mcpProjectWorkflowService;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public UpdateMcpProjectWorkflowParametersToolCallback(McpProjectWorkflowService mcpProjectWorkflowService) {
        this.mcpProjectWorkflowService = mcpProjectWorkflowService;
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
            UpdateMcpProjectWorkflowParametersInput input = jsonMapper.readValue(
                toolInput, UpdateMcpProjectWorkflowParametersInput.class);

            if (input.mcpProjectWorkflowId() == null) {
                return ToolErrors.toolError(jsonMapper, "mcpProjectWorkflowId is required");
            }

            if (input.toolName() != null && StringUtils.isBlank(input.toolName())) {
                return ToolErrors.toolError(jsonMapper, "toolName must not be blank when supplied");
            }

            McpProjectWorkflow mcpProjectWorkflow = mcpProjectWorkflowService
                .fetchMcpProjectWorkflow(input.mcpProjectWorkflowId())
                .orElse(null);

            if (mcpProjectWorkflow == null) {
                return ToolErrors.toolError(
                    jsonMapper, "McpProjectWorkflow not found: " + input.mcpProjectWorkflowId());
            }

            Map<String, Object> mergedParameters = new HashMap<>(mcpProjectWorkflow.getParameters());

            if (input.toolName() != null) {
                mergedParameters.put(ToolConstants.TOOL_NAME, input.toolName());
            }

            if (input.toolDescription() != null) {
                mergedParameters.put(ToolConstants.TOOL_DESCRIPTION, input.toolDescription());
            }

            if (input.parameters() != null) {
                mergedParameters.putAll(input.parameters());
            }

            McpProjectWorkflow updatedMcpProjectWorkflow = mcpProjectWorkflowService.updateParameters(
                input.mcpProjectWorkflowId(), mergedParameters);

            Map<String, ?> updatedParameters = updatedMcpProjectWorkflow.getParameters();

            return jsonMapper.writeValueAsString(
                new UpdateMcpProjectWorkflowParametersOutput(
                    updatedMcpProjectWorkflow.getId(),
                    MapUtils.getString(updatedParameters, ToolConstants.TOOL_NAME),
                    MapUtils.getString(updatedParameters, ToolConstants.TOOL_DESCRIPTION), updatedParameters));
        } catch (JacksonException exception) {
            return ToolErrors.toolError(jsonMapper, "Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return ToolErrors.toolError(jsonMapper, exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, UpdateMcpProjectWorkflowParametersToolCallback.class, TOOL_NAME, exception);
        }
    }

    @SuppressFBWarnings({
        "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
    })
    public record UpdateMcpProjectWorkflowParametersInput(
        @Nullable Long mcpProjectWorkflowId, @Nullable String toolName, @Nullable String toolDescription,
        @Nullable Map<String, Object> parameters) {
    }

    @SuppressFBWarnings({
        "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
    })
    public record UpdateMcpProjectWorkflowParametersOutput(
        Long mcpProjectWorkflowId, @Nullable String toolName, @Nullable String toolDescription,
        Map<String, ?> parameters) {
    }
}
