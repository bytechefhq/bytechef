/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that deletes a Personal Agent. Tasks bound to the agent are NOT cascaded — the
 * {@code ai_hub_personal_agent_id} FK becomes a dangling reference and the routing agent treats it as "agent deleted,
 * fall back to plain LLM system prompt" so message history stays accessible. A future cleanup job can archive the
 * orphaned tasks.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class DeleteAiHubPersonalAgentToolCallback implements ToolCallback {

    private static final String DESCRIPTION = """
        Delete a Personal Agent. The agent's existing tasks are NOT deleted — past chat
        history remains accessible to the user, but new turns no longer apply the agent's
        instructions (the task degrades to plain LLM behaviour). Pass the agent's `id`
        (from listAiHubPersonalAgents). Confirm intent with the user before calling — this is irreversible
        for the agent definition itself.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "aiHubPersonalAgentId": {"type": "integer", "description": "Agent id from listAiHubPersonalAgents"}
                },
                "required": ["aiHubPersonalAgentId"]
            }""";

    private final AiHubPersonalAgentService aiHubPersonalAgentService;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public DeleteAiHubPersonalAgentToolCallback(
        AiHubPersonalAgentService aiHubPersonalAgentService) {
        this.aiHubPersonalAgentService = aiHubPersonalAgentService;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("deleteAiHubPersonalAgent")
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
            DeleteAiHubPersonalAgentInput input =
                jsonMapper.readValue(toolInput, DeleteAiHubPersonalAgentInput.class);

            Long aiHubPersonalAgentId = input.aiHubPersonalAgentId();

            if (aiHubPersonalAgentId == null) {
                return ToolErrors.toolError(jsonMapper, "aiHubPersonalAgentId is required");
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
                aiHubPersonalAgentService.delete(aiHubPersonalAgentId, workspaceId, userId);

                return jsonMapper.writeValueAsString(
                    new DeleteAiHubPersonalAgentOutput(true, aiHubPersonalAgentId));
            } catch (IllegalArgumentException invalid) {
                return ToolErrors.toolError(jsonMapper, invalid.getMessage());
            }
        } catch (JacksonException exception) {
            return ToolErrors.toolError(jsonMapper, "Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, DeleteAiHubPersonalAgentToolCallback.class, "deleteAiHubPersonalAgent",
                exception);
        }
    }

    public record DeleteAiHubPersonalAgentInput(@Nullable Long aiHubPersonalAgentId) {
    }

    public record DeleteAiHubPersonalAgentOutput(boolean deleted, long aiHubPersonalAgentId) {
    }
}
