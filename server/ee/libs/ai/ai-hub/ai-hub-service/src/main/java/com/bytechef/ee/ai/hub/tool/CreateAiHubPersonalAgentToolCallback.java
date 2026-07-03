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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that creates a Personal Agent in the user's workspace. Pairs with
 * {@link ListAiHubPersonalAgentsToolCallback} for read and {@link OpenAiHubPersonalAgentTabToolCallback} for
 * navigation. Together the three give the LLM a complete "create an agent for X" flow: the user says "set up a personal
 * agent for technical code review", the LLM crafts a slug + title + instructions and calls this tool, then opens the
 * resulting task.
 *
 * <p>
 * Same security posture as {@link CreateWorkflowChatToolCallback}: workspace + user identity comes ONLY from the
 * trusted {@link ToolContext}, never from input arguments. An LLM that hallucinates a {@code workspaceId} field cannot
 * cross workspace boundaries.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CreateAiHubPersonalAgentToolCallback implements ToolCallback {

    private static final String DESCRIPTION = """
        Create a Personal Agent in the user's current workspace. The agent gets a slug-based name, a
        display title, an optional description, and an optional instructions block that gets appended
        to the system prompt every time the user chats with this agent. Returns
        {id, name, title, description}. Idempotent: re-running with an existing slug returns a typed
        error so the LLM can pick a different name rather than silently overwrite. Always pair with
        openAiHubPersonalAgentTab to surface the new agent's task in the sidebar.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "name": {
                        "type": "string",
                        "description": "Slug-style identifier (lowercase letters, digits, hyphens, underscores). Free text is auto-slugified server-side."
                    },
                    "title": {
                        "type": "string",
                        "description": "Optional display title for the sidebar (defaults to the name when omitted)"
                    },
                    "description": {
                        "type": "string",
                        "description": "Optional short blurb (max 1024 chars) shown as a tooltip in the sidebar"
                    },
                    "instructions": {
                        "type": "string",
                        "description": "Optional system-prompt extension applied on every turn (max 65536 chars)"
                    }
                },
                "required": ["name"]
            }""";

    private final AiHubPersonalAgentService aiHubPersonalAgentService;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CreateAiHubPersonalAgentToolCallback(
        AiHubPersonalAgentService aiHubPersonalAgentService) {
        this.aiHubPersonalAgentService = aiHubPersonalAgentService;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("createAiHubPersonalAgent")
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
            CreateAiHubPersonalAgentInput input =
                jsonMapper.readValue(toolInput, CreateAiHubPersonalAgentInput.class);

            if (input.name() == null || input.name()
                .isBlank()) {
                return ToolErrors.toolError(jsonMapper, "name is required");
            }

            AiHubToolInvocationContext context =
                AiHubToolInvocationContext.fromToolContext(toolContext);

            if (context == null || context.workspaceId() == null || context.userId() == null) {
                return ToolErrors.toolError(jsonMapper,
                    "Workspace context unavailable — open this chat from the AI Hub of a workspace.");
            }

            int environment = AiHubToolInvocationContext.resolveEnvironmentOrDefault(context);

            try {
                // The agent-creation tool callback doesn't expose per-agent LLM model selection — that's a
                // human-admin-only setting via the personal-agent edit panel. Pass null so created agents fall back
                // to the workspace default LLM.
                AiHubPersonalAgent agent = aiHubPersonalAgentService.create(
                    context.workspaceId(), context.userId(), environment, input.name(), input.title(),
                    input.description(), input.instructions(), null, null);

                return jsonMapper.writeValueAsString(
                    new CreateAiHubPersonalAgentOutput(
                        agent.getId(), agent.getName(), agent.getTitle(), agent.getDescription()));
            } catch (ConflictException conflict) {
                // Surface the typed conflict as a tool-level error rather than letting the runtime catch swallow it
                // into a generic stack trace. The LLM can then either pick a different name or call
                // listAiHubPersonalAgents to find the existing one.
                return ToolErrors.toolError(jsonMapper, conflict.getMessage());
            } catch (IllegalArgumentException invalid) {
                // Slugification failed (blank-after-strip) or a length cap was exceeded. Surface the message so
                // the LLM can correct rather than retry-loop with the same input.
                return ToolErrors.toolError(jsonMapper, invalid.getMessage());
            }
        } catch (JacksonException exception) {
            return ToolErrors.toolError(jsonMapper, "Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, CreateAiHubPersonalAgentToolCallback.class, "createAiHubPersonalAgent",
                exception);
        }
    }

    public record CreateAiHubPersonalAgentInput(
        String name, @Nullable String title, @Nullable String description, @Nullable String instructions) {
    }

    public record CreateAiHubPersonalAgentOutput(
        long id, String name, @Nullable String title, @Nullable String description) {
    }
}
