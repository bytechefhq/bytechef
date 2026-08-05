/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgent;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that patches an existing Personal Agent's editable fields. The agent's {@code name} is
 * intentionally NOT updatable — chat URLs and tool references would silently break — so renames go through a dedicated
 * rename operation that doesn't yet exist (deferred). Title, description, and instructions are all patchable.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class UpdateAiHubPersonalAgentToolCallback implements ToolCallback {

    private static final String DESCRIPTION = """
        Update an existing Personal Agent's title, description, and/or instructions. Pass the agent's
        `id` (from listAiHubPersonalAgents). Each field is optional — null leaves the existing value
        unchanged. The `name` field is intentionally NOT updatable here because chat URLs reference
        it; renames need a dedicated path that doesn't yet exist.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "aiHubPersonalAgentId": {"type": "integer", "description": "Agent id from listAiHubPersonalAgents"},
                    "title": {"type": "string", "description": "New display title (max 255 chars)"},
                    "description": {"type": "string", "description": "New description (max 1024 chars)"},
                    "instructions": {"type": "string", "description": "New system-prompt extension (max 65536 chars)"}
                },
                "required": ["aiHubPersonalAgentId"]
            }""";

    private final AiHubPersonalAgentService aiHubPersonalAgentService;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public UpdateAiHubPersonalAgentToolCallback(
        AiHubPersonalAgentService aiHubPersonalAgentService) {
        this.aiHubPersonalAgentService = aiHubPersonalAgentService;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("updateAiHubPersonalAgent")
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
            UpdateAiHubPersonalAgentInput input =
                jsonMapper.readValue(toolInput, UpdateAiHubPersonalAgentInput.class);

            Long aiHubPersonalAgentId = input.aiHubPersonalAgentId();

            if (aiHubPersonalAgentId == null) {
                return ToolErrors.toolError(jsonMapper, "aiHubPersonalAgentId is required");
            }

            if (input.title() == null && input.description() == null && input.instructions() == null) {
                return ToolErrors.toolError(jsonMapper,
                    "At least one of title, description, or instructions must be provided");
            }

            AiHubToolInvocationContext context =
                AiHubToolInvocationContext.fromToolContext(toolContext);

            Long workspaceId = context == null ? null : context.workspaceId();
            Long userId = context == null ? null : context.userId();

            if (workspaceId == null || userId == null) {
                return ToolErrors.toolError(jsonMapper,
                    "Workspace context unavailable — open this chat from the AI Hub of a workspace.");
            }

            try {
                // The agent-update tool callback doesn't expose per-agent LLM model selection (admin-only via UI).
                // Pass null/null to leave the override fields untouched.
                AiHubPersonalAgent agent = aiHubPersonalAgentService.update(
                    aiHubPersonalAgentId, workspaceId, userId, input.title(),
                    input.description(), input.instructions(), null, null);

                return jsonMapper.writeValueAsString(
                    new UpdateAiHubPersonalAgentOutput(
                        agent.getId(), agent.getName(), agent.getTitle(), agent.getDescription()));
            } catch (IllegalArgumentException invalid) {
                // Either ownership mismatch (service throws when findOwned returns empty) or a length-cap violation.
                // Both surface as tool errors for the LLM to recover from.
                return ToolErrors.toolError(jsonMapper, invalid.getMessage());
            }
        } catch (JacksonException exception) {
            return ToolErrors.toolError(jsonMapper, "Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, UpdateAiHubPersonalAgentToolCallback.class, "updateAiHubPersonalAgent",
                exception);
        }
    }

    public record UpdateAiHubPersonalAgentInput(
        @Nullable Long aiHubPersonalAgentId, @Nullable String title, @Nullable String description,
        @Nullable String instructions) {
    }

    public record UpdateAiHubPersonalAgentOutput(
        long id, String name, @Nullable String title, @Nullable String description) {
    }
}
