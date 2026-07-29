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

package com.bytechef.automation.ai.tool.knowledgebase;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that creates a new knowledge base in the current workspace and environment. The
 * mutation executes immediately; documents are added afterwards with {@code addKnowledgeBaseDocument}.
 *
 * <p>
 * This callback is registered on {@code aiHubBuildSpringAIAgent} only — the ASK variant is read-only.
 * </p>
 *
 *
 * @author Ivica Cardic
 */
public class CreateKnowledgeBaseToolCallback implements ToolCallback {

    private static final long DEFAULT_ENVIRONMENT_ORDINAL = 0L;
    private static final String TOOL_NAME = "createKnowledgeBase";

    private static final String DESCRIPTION = """
        Create a new, empty knowledge base in the current workspace. Supply a name and an optional
        description. Returns the new knowledgeBaseId — use addKnowledgeBaseDocument to add text
        documents to it afterwards.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "name": {"type": "string", "description": "Name of the knowledge base"},
                    "description": {"type": "string", "description": "Optional description of what the knowledge base contains"}
                },
                "required": ["name"]
            }""";

    private final WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CreateKnowledgeBaseToolCallback(WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade) {
        this.workspaceKnowledgeBaseFacade = workspaceKnowledgeBaseFacade;
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
            CreateKnowledgeBaseInput input = jsonMapper.readValue(toolInput, CreateKnowledgeBaseInput.class);

            if (input.name() == null || input.name()
                .isBlank()) {
                return toolError("name is required");
            }

            AgentToolInvocationContext invocationContext =
                AgentToolInvocationContext.fromToolContext(toolContext);

            Long workspaceId = invocationContext == null ? null : invocationContext.workspaceId();

            if (workspaceId == null) {
                return toolError(
                    "Workspace context unavailable - open this chat from the AI Hub of a workspace.");
            }

            KnowledgeBase knowledgeBase = new KnowledgeBase();

            knowledgeBase.setName(input.name());

            if (input.description() != null && !input.description()
                .isBlank()) {
                knowledgeBase.setDescription(input.description());
            }

            KnowledgeBase createdKnowledgeBase = workspaceKnowledgeBaseFacade.createWorkspaceKnowledgeBase(
                knowledgeBase, workspaceId, resolveEnvironmentId(invocationContext));

            return jsonMapper.writeValueAsString(
                new CreateKnowledgeBaseOutput(
                    String.valueOf(createdKnowledgeBase.getId()), createdKnowledgeBase.getName()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, CreateKnowledgeBaseToolCallback.class, TOOL_NAME, exception);
        }
    }

    private long resolveEnvironmentId(AgentToolInvocationContext invocationContext) {
        Long environmentId = invocationContext.environmentId();

        return environmentId != null ? environmentId : DEFAULT_ENVIRONMENT_ORDINAL;
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record CreateKnowledgeBaseInput(String name, @Nullable String description) {
    }

    public record CreateKnowledgeBaseOutput(String knowledgeBaseId, String name) {
    }
}
