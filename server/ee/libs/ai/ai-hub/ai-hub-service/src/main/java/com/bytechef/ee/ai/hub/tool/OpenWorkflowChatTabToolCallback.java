/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatAccessPolicy;
import com.bytechef.ee.ai.hub.chat.AiHubChatKind;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that lets the AI Hub agent navigate the user to a workflow chat. It resolves the
 * {@code threadId} to a chat and answers {@code {opened, chatId, threadId, title}}; the client switches the active chat
 * to {@code chatId}.
 *
 * <p>
 * Pairs with {@link CreateWorkflowChatToolCallback}: the agent creates a workflow chat, then fires this tool to surface
 * it. The chat must exist, not be deleted, be webhook-bridged, live in the invocation's workspace and admit the
 * invoking user as a participant; any other thread id is refused with the same tool error, so thread ids of other
 * users' chats cannot be probed. The tool has no persistent server-side effect.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class OpenWorkflowChatTabToolCallback implements ToolCallback {

    private static final String DESCRIPTION = """
        Navigate the user to a workflow chat. Call this AFTER createWorkflowChat to surface
        the new chat in the AI Hub sidebar, passing the threadId returned from createWorkflowChat.
        Never invent a threadId.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "threadId": {
                    "type": "string",
                    "description": "threadId from createWorkflowChat (a UUID)"
                },
                "title": {
                    "type": "string",
                    "description": "Optional display name for the tab/sidebar entry (defaults to existing title)"
                }
            },
            "required": ["threadId"]
        }""";

    private static final String CHAT_NOT_FOUND = "No workflow chat with threadId %s. Pass the threadId returned " +
        "from createWorkflowChat.";

    private static final String WORKSPACE_CONTEXT_UNAVAILABLE =
        "Workspace context unavailable. Open this chat from the AI Hub of a workspace.";

    private final AiHubChatAccessPolicy accessPolicy;
    private final AiHubChatService chatService;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public OpenWorkflowChatTabToolCallback(AiHubChatService chatService, AiHubChatAccessPolicy accessPolicy) {
        this.accessPolicy = accessPolicy;
        this.chatService = chatService;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("openWorkflowChatTab")
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
            OpenWorkflowChatTabInput input = jsonMapper.readValue(toolInput, OpenWorkflowChatTabInput.class);

            String threadId = input.threadId();

            if (threadId == null || threadId.isBlank()) {
                return toolError("threadId is required");
            }

            AiHubToolInvocationContext invocationContext = AiHubToolInvocationContext.fromToolContext(toolContext);

            if (invocationContext == null || invocationContext.workspaceId() == null
                || invocationContext.userId() == null) {

                return toolError(WORKSPACE_CONTEXT_UNAVAILABLE);
            }

            Optional<AiHubChat> chatOptional = chatService.findNotDeletedByThreadId(threadId)
                .filter(chat -> isOpenable(chat, invocationContext.workspaceId(), invocationContext.userId()));

            if (chatOptional.isEmpty()) {
                return toolError(String.format(CHAT_NOT_FOUND, threadId));
            }

            AiHubChat chat = chatOptional.get();

            String title = input.title();

            if (title == null || title.isBlank()) {
                title = chat.getTitle();
            }

            return jsonMapper.writeValueAsString(
                new OpenWorkflowChatTabOutput(true, chat.getId(), chat.getThreadId(), title));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, OpenWorkflowChatTabToolCallback.class, "openWorkflowChatTab", exception);
        }
    }

    private boolean isOpenable(AiHubChat chat, long workspaceId, long userId) {
        AiHubChatKind kind = chat.getKind();

        return kind.isWebhookBridged() && Objects.equals(chat.getWorkspaceId(), workspaceId)
            && accessPolicy.canParticipate(chat, userId);
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record OpenWorkflowChatTabInput(String threadId, @Nullable String title) {
    }

    public record OpenWorkflowChatTabOutput(
        boolean opened, long chatId, String threadId, @Nullable String title) {
    }
}
