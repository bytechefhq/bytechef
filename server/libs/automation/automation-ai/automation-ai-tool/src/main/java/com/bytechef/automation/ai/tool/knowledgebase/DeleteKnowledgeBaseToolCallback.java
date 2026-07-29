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
import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Deletes an entire Knowledge Base (and, by cascade, all its documents). Irreversible — the caller must confirm with
 * the user first. Authorization is enforced by {@link WorkspaceKnowledgeBaseFacade#deleteWorkspaceKnowledgeBase}
 * itself, so this callback does not need to read the invocation context.
 *
 * @author Ivica Cardic
 */
public class DeleteKnowledgeBaseToolCallback implements ToolCallback {

    static final String TOOL_NAME = "deleteKnowledgeBase";

    private static final String DESCRIPTION = """
        Delete an entire Knowledge Base. Cascade deletes all its documents. Irreversible.
        Always confirm with the user before calling.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "id": {"type": "integer", "description": "Knowledge Base id to delete"}
            },
            "required": ["id"]
        }""";

    private final WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public DeleteKnowledgeBaseToolCallback(WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade) {
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
            DeleteKnowledgeBaseToolInput input = jsonMapper.readValue(toolInput, DeleteKnowledgeBaseToolInput.class);

            if (input.id() == null) {
                return toolError("id is required");
            }

            workspaceKnowledgeBaseFacade.deleteWorkspaceKnowledgeBase(input.id());

            return jsonMapper.writeValueAsString(Map.of("deleted", true, "id", input.id()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, DeleteKnowledgeBaseToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record DeleteKnowledgeBaseToolInput(Long id) {
    }
}
