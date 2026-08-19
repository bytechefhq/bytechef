/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that creates a workflow chat for a given workflow execution. Pairs with
 * {@link ListChatWorkflowsToolCallback} (which surfaces the {@code workflowExecutionTriggerId} the LLM passes here) and
 * with the bridge agent that executes turns once the chat exists.
 *
 * <p>
 * <b>Why this tool exists:</b> the workflow-chats sidebar lazily creates the chat row on the first user click — users
 * who can describe what they want ("set up a chat with my support workflow") otherwise have to remember which workflow
 * corresponds to that intent and click through. This tool lets the agent do the lookup-and-create with one
 * natural-language turn, returning the {@code threadId} the client navigates to via
 * {@link OpenWorkflowChatTabToolCallback}.
 * </p>
 *
 * <p>
 * Each invocation creates a new chat row through {@link AiHubChatService#createWorkflowChat} (always-new semantics, May
 * 2026); past chats bound to the same workflow remain reachable through the chats list rather than being restored on
 * re-invocation.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CreateWorkflowChatToolCallback implements ToolCallback {

    private static final String DESCRIPTION = """
        Create a workflow chat for a workflow returned by listChatWorkflows. Use this when the
        user asks to start chatting with a workflow by name (e.g. "open a chat with my customer support
        workflow"). Each call creates a fresh chat; past chats with the same workflow remain
        in the chats list. Returns {threadId, chatId, title} so you can pass threadId to
        openWorkflowChatTab to surface the new chat in the AI Hub. Always call
        listChatWorkflows first to obtain a real workflowExecutionTriggerId — never invent one.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "workflowExecutionTriggerId": {
                        "type": "string",
                        "description": "The workflowExecutionTriggerId from listChatWorkflows. NOT the workflow id."
                    },
                    "projectDeploymentId": {
                        "type": "string",
                        "description": "The projectDeploymentId from listChatWorkflows."
                    },
                    "title": {
                        "type": "string",
                        "description": "Optional initial title for the chat row in the sidebar. Use a short human-readable label like the workflow name."
                    }
                },
                "required": ["workflowExecutionTriggerId", "projectDeploymentId"]
            }""";

    private final AiHubChatService chatService;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CreateWorkflowChatToolCallback(AiHubChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("createWorkflowChat")
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
            CreateWorkflowChatInput input = jsonMapper.readValue(toolInput, CreateWorkflowChatInput.class);

            if (input.workflowExecutionTriggerId() == null || input.workflowExecutionTriggerId()
                .isBlank()) {
                return toolError("workflowExecutionTriggerId is required");
            }

            if (input.projectDeploymentId() == null || input.projectDeploymentId()
                .isBlank()) {
                return toolError("projectDeploymentId is required");
            }

            long projectDeploymentId;

            try {
                projectDeploymentId = Long.parseLong(input.projectDeploymentId());
            } catch (NumberFormatException exception) {
                return toolError("projectDeploymentId must be a numeric id: " + input.projectDeploymentId());
            }

            // fromToolContext returns null when the ToolContext is missing or carries no workspace keys; treat
            // both as "no usable workspace identity" so we can't accidentally create a chat under the wrong owner.
            AiHubToolInvocationContext context =
                AiHubToolInvocationContext.fromToolContext(toolContext);

            if (context == null) {
                return toolError(
                    "Workspace context unavailable — open this chat from the AI Hub of a workspace.");
            }

            Long workspaceId = context.workspaceId();
            Long userId = context.userId();

            if (workspaceId == null || userId == null) {
                return toolError(
                    "Workspace context unavailable — open this chat from the AI Hub of a workspace.");
            }

            // Resolve the environment ordinal via the shared helper so this tool stays consistent with how the rest
            // of the asset-file / chat tooling defaults to DEVELOPMENT (ordinal 0) when the toolContext
            // hasn't been threaded through.
            int environment = AiHubToolInvocationContext.resolveEnvironmentOrDefault(context);

            AiHubChat chat = chatService.createWorkflowChat(
                workspaceId, userId, environment, input.workflowExecutionTriggerId(), projectDeploymentId,
                input.title());

            return jsonMapper.writeValueAsString(
                new CreateWorkflowChatOutput(
                    chat.getThreadId(), chat.getId(), chat.getTitle()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, CreateWorkflowChatToolCallback.class, "createWorkflowChat", exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record CreateWorkflowChatInput(
        String workflowExecutionTriggerId, String projectDeploymentId, @Nullable String title) {
    }

    public record CreateWorkflowChatOutput(String threadId, long chatId, @Nullable String title) {
    }
}
