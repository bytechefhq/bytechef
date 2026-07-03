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
import com.bytechef.ee.ai.hub.task.AiHubTask;
import com.bytechef.ee.ai.hub.task.AiHubTaskService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that opens a Personal Agent task in the AI Hub sidebar. Resolves the agent by id,
 * creates a fresh task through {@link AiHubTaskService}, and returns the {@code threadId} the client subscriber
 * switches to.
 *
 * <p>
 * Doing the create in this tool (instead of a separate "createAiHubPersonalAgentChat" tool) keeps the LLM's flow
 * simple: list agents → open one. Each invocation creates a new task row (always-new semantics, May 2026); past tasks
 * with the same agent stay reachable through the tasks list.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class OpenAiHubPersonalAgentTabToolCallback implements ToolCallback {

    private static final String DESCRIPTION = """
        Open the user's personal agent in the AI Hub sidebar. Pass the agent's `id` (an
        integer from listAiHubPersonalAgents). The tool creates a fresh task and returns
        {threadId, taskId, title} so the client can navigate. Each call starts a new
        task; past tasks with the same agent remain in the tasks list.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "aiHubPersonalAgentId": {
                    "type": "integer",
                    "description": "Personal agent id from listAiHubPersonalAgents. Never invent ids."
                }
            },
            "required": ["aiHubPersonalAgentId"]
        }""";

    private final AiHubPersonalAgentService aiHubPersonalAgentService;
    private final AiHubTaskService taskService;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public OpenAiHubPersonalAgentTabToolCallback(
        AiHubPersonalAgentService aiHubPersonalAgentService,
        AiHubTaskService taskService) {

        this.aiHubPersonalAgentService = aiHubPersonalAgentService;
        this.taskService = taskService;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("openAiHubPersonalAgentTab")
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
            OpenAiHubPersonalAgentTabInput input =
                jsonMapper.readValue(toolInput, OpenAiHubPersonalAgentTabInput.class);

            if (input.aiHubPersonalAgentId() == null) {
                return ToolErrors.toolError(jsonMapper, "aiHubPersonalAgentId is required");
            }

            AiHubToolInvocationContext context =
                AiHubToolInvocationContext.fromToolContext(toolContext);

            if (context == null || context.workspaceId() == null || context.userId() == null) {
                return ToolErrors.toolError(jsonMapper,
                    "Workspace context unavailable — open this chat from the AI Hub of a workspace.");
            }

            int environment = AiHubToolInvocationContext.resolveEnvironmentOrDefault(context);

            // Ownership check before we create the task: refuses to open an agent the requesting user
            // doesn't own. The service-layer findOwned enforces (workspaceId, userId) match — even though the LLM
            // can only see ids the user has access to via listAiHubPersonalAgents, the redundant check defends
            // against
            // an injected id from a malicious prompt.
            Optional<AiHubPersonalAgent> agentOptional = aiHubPersonalAgentService.findOwned(
                input.aiHubPersonalAgentId(), context.workspaceId(), context.userId());

            if (agentOptional.isEmpty()) {
                return ToolErrors.toolError(jsonMapper,
                    "Personal agent " + input.aiHubPersonalAgentId() + " not found in this workspace");
            }

            AiHubPersonalAgent agent = agentOptional.get();
            String defaultTitle = agent.getTitle() != null ? agent.getTitle() : agent.getName();

            AiHubTask task = taskService.createAiHubPersonalAgentChat(
                context.workspaceId(), context.userId(), environment, agent.getId(), defaultTitle);

            return jsonMapper.writeValueAsString(
                new OpenAiHubPersonalAgentTabOutput(
                    true, task.getThreadId(), task.getId(), task.getTitle()));
        } catch (JacksonException exception) {
            return ToolErrors.toolError(jsonMapper, "Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, OpenAiHubPersonalAgentTabToolCallback.class, "openAiHubPersonalAgentTab",
                exception);
        }
    }

    public record OpenAiHubPersonalAgentTabInput(@Nullable Long aiHubPersonalAgentId) {
    }

    public record OpenAiHubPersonalAgentTabOutput(
        boolean opened, String threadId, long taskId, @Nullable String title) {
    }
}
