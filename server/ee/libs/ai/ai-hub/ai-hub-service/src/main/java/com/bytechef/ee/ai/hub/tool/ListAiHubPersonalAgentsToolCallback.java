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
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that lists every Personal Agent the current user owns in the active workspace +
 * environment. Used as the read leg of the create/list/open flow alongside {@link CreateAiHubPersonalAgentToolCallback}
 * and {@link OpenAiHubPersonalAgentTabToolCallback}.
 *
 * <p>
 * The output is intentionally compact — the LLM doesn't need full instructions or descriptions for a list view, just
 * enough metadata to pick the right agent ({@code id}, {@code name}, {@code title}, short {@code description}).
 * Instructions stay locally to the agent's runtime context to keep the tool result small.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ListAiHubPersonalAgentsToolCallback implements ToolCallback {

    private static final String DESCRIPTION =
        """
            List the user's personal agents for the current workspace + environment. Returns a JSON array
            of {id, name, title, description} entries sorted most-recently-updated first. Use this before
            createAiHubPersonalAgentChat / openAiHubPersonalAgentTab when the user asks to "open my X agent" so you
            can resolve the agent name to a real id without inventing one.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {}
        }""";

    private final AiHubPersonalAgentService aiHubPersonalAgentService;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ListAiHubPersonalAgentsToolCallback(
        AiHubPersonalAgentService aiHubPersonalAgentService) {
        this.aiHubPersonalAgentService = aiHubPersonalAgentService;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("listAiHubPersonalAgents")
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
            AiHubToolInvocationContext context =
                AiHubToolInvocationContext.fromToolContext(toolContext);

            if (context == null || context.workspaceId() == null || context.userId() == null) {
                return ToolErrors.toolError(jsonMapper,
                    "Workspace context unavailable — open this chat from the AI Hub of a workspace.");
            }

            int environment = AiHubToolInvocationContext.resolveEnvironmentOrDefault(context);

            List<AiHubPersonalAgent> agents =
                aiHubPersonalAgentService.list(context.workspaceId(), context.userId(), environment);

            return jsonMapper.writeValueAsString(agents.stream()
                .map(ListAiHubPersonalAgentsToolCallback::toSummary)
                .toList());
        } catch (JacksonException exception) {
            return ToolErrors.toolError(jsonMapper, "Serialization error: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, ListAiHubPersonalAgentsToolCallback.class, "listAiHubPersonalAgents",
                exception);
        }
    }

    private static AiHubPersonalAgentSummary toSummary(AiHubPersonalAgent agent) {
        return new AiHubPersonalAgentSummary(agent.getId(), agent.getName(), agent.getTitle(),
            agent.getDescription());
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AiHubPersonalAgentSummary(long id, String name, @Nullable String title,
        @Nullable String description) {
    }
}
