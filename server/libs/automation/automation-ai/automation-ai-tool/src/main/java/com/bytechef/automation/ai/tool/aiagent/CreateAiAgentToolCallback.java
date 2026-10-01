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
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.ai.agent.dto.AiAgentDTO;
import com.bytechef.automation.ai.agent.facade.AiAgentFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
public class CreateAiAgentToolCallback implements ToolCallback {

    private static final String TOOL_NAME = "createAiAgent";

    private static final String DESCRIPTION = """
        Create a new AI Agent in the current workspace, inside an existing project (projectId) or a new project
        named after the agent. Supply a title (and optionally a description and a projectId). When the user
        names a project to put the agent in, create or find that project first and pass its id: never call
        createProject and createAiAgent in the same step, or the agent lands in a second project of its own.
        The agent starts with no MODEL, no tools, and only the two permanent channels (chat,
        workflowCall) — it cannot be published until a MODEL element is added via addAiAgentElement.
        Returns the new agent's id, name (slugified from the title), title, and projectId.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "title": {"type": "string", "description": "Display title for the new agent"},
                    "description": {"type": "string", "description": "Optional human-readable description"},
                    "projectId": {"type": "integer", "description": "Optional id of an existing project to add the agent to. When omitted a new project named after the agent is created."}
                },
                "required": ["title"]
            }""";

    private final AiAgentFacade aiAgentFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CreateAiAgentToolCallback(AiAgentFacade aiAgentFacade) {
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
            CreateAiAgentInput input = jsonMapper.readValue(toolInput, CreateAiAgentInput.class);

            if (input.title() == null || input.title()
                .isBlank()) {

                return toolError("title is required");
            }

            AgentToolInvocationContext invocationContext = AgentToolInvocationContext.fromToolContext(toolContext);

            Long workspaceId = invocationContext == null ? null : invocationContext.workspaceId();

            if (workspaceId == null) {
                return toolError("Workspace context unavailable - open this chat from the AI Hub of a workspace.");
            }

            AiAgentDTO agentDTO = aiAgentFacade.createAgent(
                input.title(), input.description(), workspaceId, input.projectId());

            return jsonMapper.writeValueAsString(
                new CreateAiAgentOutput(
                    agentDTO.agent()
                        .getId(),
                    agentDTO.agent()
                        .getName(),
                    agentDTO.agent()
                        .getTitle(),
                    agentDTO.projectId()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, CreateAiAgentToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record CreateAiAgentInput(String title, @Nullable String description, @Nullable Long projectId) {
    }

    public record CreateAiAgentOutput(Long id, String name, String title, long projectId) {
    }
}
