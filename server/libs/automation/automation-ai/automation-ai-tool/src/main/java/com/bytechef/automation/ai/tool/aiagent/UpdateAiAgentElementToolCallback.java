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

package com.bytechef.automation.ai.tool.aiagent;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.automation.ai.agent.facade.AiAgentFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Updates an AI Agent element's parameters and/or connectionId. Partial update: an omitted field keeps its current
 * value — there is no way to clear a previously-set connectionId through this tool (delete and re-add instead).
 *
 * @author Ivica Cardic
 */
public class UpdateAiAgentElementToolCallback implements ToolCallback {

    private static final String TOOL_NAME = "updateAiAgentElement";

    private static final String DESCRIPTION = """
        Update an AI Agent element's parameters and/or connectionId, by element id (from getAiAgent's
        elements list). Only the fields you supply are changed; an omitted field keeps its current
        value. To reconfigure a TOOL's requiresApproval flag, pass the full parameters object with the
        updated value — parameters is replaced wholesale, not merged.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "elementId": {"type": "integer", "description": "Element id"},
                "parameters": {"type": "object", "description": "New element parameters (replaces the existing map)"},
                "connectionId": {"type": "integer", "description": "New connection id"}
            },
            "required": ["elementId"]
        }""";

    private final AiAgentFacade aiAgentFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public UpdateAiAgentElementToolCallback(AiAgentFacade aiAgentFacade) {
        this.aiAgentFacade = aiAgentFacade;
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
            UpdateAiAgentElementInput input = jsonMapper.readValue(toolInput, UpdateAiAgentElementInput.class);

            if (input.elementId() == null) {
                return toolError("elementId is required");
            }

            aiAgentFacade.updateAgentElement(input.elementId(), input.parameters(), input.connectionId());

            return jsonMapper.writeValueAsString(Map.of("updated", true, "elementId", input.elementId()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, UpdateAiAgentElementToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    @SuppressFBWarnings({
        "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
    })
    public record UpdateAiAgentElementInput(
        Long elementId, @Nullable Map<String, Object> parameters, @Nullable Long connectionId) {
    }
}
