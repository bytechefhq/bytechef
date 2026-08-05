/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ee.ai.hub.exception.ConflictException;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgent;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentService;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that clones a Personal Agent into a different environment within the same workspace.
 * Same-workspace, cross-environment scope: the user's mental model is "promote my agent to PROD," not "share my agent
 * to another workspace." The source agent's title, description, instructions, AND tool template entries deep-copy onto
 * the clone; the slug ({@code name}) defaults to the source's slug or accepts a caller-supplied {@code newName}
 * override when the destination env already has an agent of that name.
 *
 * <p>
 * Security: workspace + user identity comes from the trusted {@link ToolContext} only — the LLM cannot pass a
 * {@code workspaceId} field to cross workspace boundaries. The service layer additionally re-validates ownership of the
 * source agent before cloning (defense in depth against forged ids).
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CloneAiHubPersonalAgentToolCallback implements ToolCallback {

    private static final String DESCRIPTION = """
        Clone a Personal Agent into a different environment within the same workspace. Use this when the user
        says "promote my research-bot to PROD" or "copy my code-reviewer agent to staging." The clone preserves
        the source agent's title, description, instructions, AND tool template entries verbatim. By default the
        clone's slug name matches the source's; pass `newName` to override (required when the target env already
        has an agent with that slug). Returns {id, name, environment}. Always idempotent against name collision —
        a typed conflict error returns so the LLM can pick a different newName rather than silently overwrite.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "agentId": {
                        "type": "integer",
                        "description": "Source agent id from listAiHubPersonalAgents. Never invent ids."
                    },
                    "targetEnvironment": {
                        "type": "string",
                        "enum": ["DEVELOPMENT", "STAGING", "PRODUCTION"],
                        "description": "Destination environment for the clone."
                    },
                    "newName": {
                        "type": "string",
                        "description": "Optional slug override (lowercase, digits, hyphens, underscores). Required when the target environment already has an agent with the source's slug name."
                    }
                },
                "required": ["agentId", "targetEnvironment"]
            }""";

    private final AiHubPersonalAgentService aiHubPersonalAgentService;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CloneAiHubPersonalAgentToolCallback(
        AiHubPersonalAgentService aiHubPersonalAgentService) {
        this.aiHubPersonalAgentService = aiHubPersonalAgentService;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("cloneAiHubPersonalAgent")
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
            CloneAiHubPersonalAgentInput input =
                jsonMapper.readValue(toolInput, CloneAiHubPersonalAgentInput.class);

            Long agentId = input.agentId();

            if (agentId == null) {
                return ToolErrors.toolError(jsonMapper, "agentId is required");
            }

            String targetEnvironment = input.targetEnvironment();

            if (targetEnvironment == null || targetEnvironment.isBlank()) {
                return ToolErrors.toolError(jsonMapper, "targetEnvironment is required");
            }

            int targetEnvironmentOrdinal;

            try {
                targetEnvironmentOrdinal = Environment.valueOf(targetEnvironment)
                    .ordinal();
            } catch (IllegalArgumentException invalidEnv) {
                return ToolErrors.toolError(
                    jsonMapper,
                    "targetEnvironment must be one of DEVELOPMENT, STAGING, PRODUCTION (got: "
                        + targetEnvironment + ")");
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
                AiHubPersonalAgent clone = aiHubPersonalAgentService.cloneToEnvironment(
                    agentId, workspaceId, userId, targetEnvironmentOrdinal,
                    input.newName());

                return jsonMapper.writeValueAsString(
                    new CloneAiHubPersonalAgentOutput(
                        clone.getId(), clone.getName(),
                        Environment.values()[clone.getEnvironmentOrdinal()].name()));
            } catch (ConflictException conflict) {
                // LLM should retry with a different newName. Surface the typed message verbatim so the user-facing
                // toast (and the LLM's reasoning) is consistent with createAiHubPersonalAgent's conflict path.
                return ToolErrors.toolError(jsonMapper, conflict.getMessage());
            } catch (IllegalArgumentException invalid) {
                return ToolErrors.toolError(jsonMapper, invalid.getMessage());
            }
        } catch (JacksonException exception) {
            return ToolErrors.toolError(jsonMapper, "Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, CloneAiHubPersonalAgentToolCallback.class, "cloneAiHubPersonalAgent",
                exception);
        }
    }

    public record CloneAiHubPersonalAgentInput(
        @Nullable Long agentId, @Nullable String targetEnvironment, @Nullable String newName) {
    }

    public record CloneAiHubPersonalAgentOutput(long id, String name, String environment) {
    }
}
