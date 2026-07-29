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
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that clones a knowledge base into a different environment within the same workspace.
 * The clone preserves the source's name (or a caller-supplied {@code newName}), description, and chunking configuration
 * (max chunk size, min chunk size chars, overlap). Documents are <strong>not</strong> copied — see
 * {@link WorkspaceKnowledgeBaseFacade#cloneWorkspaceKnowledgeBase} for the rationale; the short version is that
 * document ingestion fans out to async embedding jobs and silently kicking those off as a side effect of "promote my
 * KB" would surprise users with an unexpected LLM bill.
 *
 * <p>
 * Security: workspace identity comes from the trusted {@link ToolContext} only — the LLM cannot pass a
 * {@code workspaceId} field. The facade additionally re-validates ownership of the source knowledge base before
 * cloning.
 * </p>
 *
 *
 * @author Ivica Cardic
 */
public class CloneKnowledgeBaseToolCallback implements ToolCallback {

    private static final String TOOL_NAME = "cloneKnowledgeBase";

    private static final String DESCRIPTION = """
        Clone a knowledge base into a different environment within the same workspace. Use when the user says
        "promote my support-docs KB to PROD" or "copy my product-knowledge KB to staging." The clone preserves
        the source's name (or pass `newName` to override), description, and chunking configuration (max chunk
        size, min chunk size chars, overlap). Documents are NOT copied — kicking off async embed jobs for an
        entire document set is a heavyweight side effect and the user should re-ingest deliberately. After
        cloning, use addKnowledgeBaseDocument to populate the destination KB. Returns {id, name, environment}.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "knowledgeBaseId": {
                        "type": "integer",
                        "description": "Source knowledge base id from listKnowledgeBases. Never invent ids."
                    },
                    "targetEnvironment": {
                        "type": "string",
                        "enum": ["DEVELOPMENT", "STAGING", "PRODUCTION"],
                        "description": "Destination environment for the clone."
                    },
                    "newName": {
                        "type": "string",
                        "description": "Optional name override; falls back to the source name when omitted."
                    }
                },
                "required": ["knowledgeBaseId", "targetEnvironment"]
            }""";

    private final WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CloneKnowledgeBaseToolCallback(WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade) {
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
            CloneKnowledgeBaseInput input = jsonMapper.readValue(toolInput, CloneKnowledgeBaseInput.class);

            if (input.knowledgeBaseId() == null) {
                return ToolErrors.toolError(jsonMapper, "knowledgeBaseId is required");
            }

            if (input.targetEnvironment() == null || input.targetEnvironment()
                .isBlank()) {
                return ToolErrors.toolError(jsonMapper, "targetEnvironment is required");
            }

            int targetEnvironmentOrdinal;

            try {
                targetEnvironmentOrdinal = Environment.valueOf(input.targetEnvironment()
                    .toUpperCase())
                    .ordinal();
            } catch (IllegalArgumentException invalidEnv) {
                return ToolErrors.toolError(
                    jsonMapper,
                    "targetEnvironment must be one of DEVELOPMENT, STAGING, PRODUCTION (got: "
                        + input.targetEnvironment() + ")");
            }

            AgentToolInvocationContext context =
                AgentToolInvocationContext.fromToolContext(toolContext);

            if (context == null || context.workspaceId() == null) {
                return ToolErrors.toolError(
                    jsonMapper,
                    "Workspace context unavailable — open this chat from the AI Hub of a workspace.");
            }

            try {
                KnowledgeBase clone = workspaceKnowledgeBaseFacade.cloneWorkspaceKnowledgeBase(
                    input.knowledgeBaseId(), context.workspaceId(), targetEnvironmentOrdinal, input.newName());

                return jsonMapper.writeValueAsString(
                    new CloneKnowledgeBaseOutput(
                        clone.getId(), clone.getName(), clone.getEnvironment()
                            .name()));
            } catch (IllegalArgumentException invalid) {
                return ToolErrors.toolError(jsonMapper, invalid.getMessage());
            }
        } catch (JacksonException exception) {
            return ToolErrors.toolError(jsonMapper, "Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, CloneKnowledgeBaseToolCallback.class, TOOL_NAME, exception);
        }
    }

    public record CloneKnowledgeBaseInput(
        @Nullable Long knowledgeBaseId, @Nullable String targetEnvironment, @Nullable String newName) {
    }

    public record CloneKnowledgeBaseOutput(long id, String name, String environment) {
    }
}
