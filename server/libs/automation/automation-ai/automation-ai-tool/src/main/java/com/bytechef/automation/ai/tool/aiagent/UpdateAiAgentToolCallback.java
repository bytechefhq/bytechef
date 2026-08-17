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
import com.bytechef.automation.ai.agent.dto.AiAgentDTO;
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
 * Updates an AI Agent's title, description, and/or instructions (the agent's system prompt). Partial update: any field
 * left out of the input keeps its current value — there is no way to explicitly clear a field through this tool.
 *
 * @author Ivica Cardic
 */
public class UpdateAiAgentToolCallback implements ToolCallback {

    private static final String TOOL_NAME = "updateAiAgent";

    private static final String DESCRIPTION = """
        Update an AI Agent's title, description, and/or instructions. instructions is the agent's system
        prompt text — the persona and behaviour the model follows on every turn. Only the fields you
        supply are changed; omitted fields keep their current value (there is no way to clear a field to
        empty through this tool). Every mutating call regenerates the agent's draft workflow — the
        change is live in the test-chat panel immediately, but is only visible to deployed
        channels after an explicit publishAiAgent call.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "id": {"type": "integer", "description": "AI Agent id"},
                "title": {"type": "string", "description": "New display title"},
                "description": {"type": "string", "description": "New human-readable description"},
                "instructions": {"type": "string", "description": "New system prompt text"}
            },
            "required": ["id"]
        }""";

    private final AiAgentFacade aiAgentFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public UpdateAiAgentToolCallback(AiAgentFacade aiAgentFacade) {
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
            UpdateAiAgentInput input = jsonMapper.readValue(toolInput, UpdateAiAgentInput.class);

            if (input.id() == null) {
                return toolError("id is required");
            }

            AiAgentDTO agentDTO = aiAgentFacade.updateAgent(
                input.id(), input.title(), input.description(), input.instructions());

            return jsonMapper.writeValueAsString(
                Map.of(
                    "updated", true, "id", agentDTO.agent()
                        .getId(),
                    "title", agentDTO.agent()
                        .getTitle()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, UpdateAiAgentToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record UpdateAiAgentInput(
        Long id, @Nullable String title, @Nullable String description, @Nullable String instructions) {
    }
}
