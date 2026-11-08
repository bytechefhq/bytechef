/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatKind;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.chat.AiHubChatStatus;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ListAiHubChatsToolCallback implements ToolCallback {

    static final String TOOL_NAME = "listAiHubChats";

    private static final int PREVIEW_MAX_LENGTH = 200;
    private static final int TITLE_MAX_LENGTH = 120;

    private static final String DESCRIPTION = """
        List previous AI Hub chats (chat threads) belonging to the current user in this workspace and
        environment. Use this to resolve a user-friendly reference like "the conversation we had yesterday
        about onboarding" or "my last research thread" to a concrete chat id without inventing one. Returns a
        JSON array of {id, title, lastPreview, messageCount, status, kind, updatedAt} entries, newest first.
        Optional status filter (ACTIVE | ARCHIVED | DELETED); defaults to ACTIVE so casual "list my chats"
        queries don't surface deleted history.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "status": {
                    "type": "string",
                    "description": "Optional. Status filter (ACTIVE, ARCHIVED, DELETED). Defaults to ACTIVE."
                }
            }
        }""";

    private final AiHubChatService chatService;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ListAiHubChatsToolCallback(AiHubChatService chatService) {
        this.chatService = chatService;
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
            AiHubToolInvocationContext invocationContext = AiHubToolInvocationContext.fromToolContext(toolContext);

            if (invocationContext == null || invocationContext.workspaceId() == null ||
                invocationContext.userId() == null) {

                return ToolErrors.toolError(
                    "Workspace/user context unavailable - open this chat from the AI Hub of a workspace.");
            }

            ListAiHubChatsInput input = JsonUtils.read(toolInput, ListAiHubChatsInput.class);

            List<AiHubChat> chats = chatService.list(
                invocationContext.workspaceId(), invocationContext.userId(),
                AiHubToolInvocationContext.resolveEnvironmentOrDefault(invocationContext),
                parseStatus(input.status()));

            return JsonUtils.write(
                chats.stream()
                    .map(ListAiHubChatsToolCallback::toSummary)
                    .toList());
        } catch (JacksonException exception) {
            return ToolErrors.invalidInput(exception);
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(ListAiHubChatsToolCallback.class, TOOL_NAME, exception);
        }
    }

    private static AiHubChatStatus parseStatus(@Nullable String status) {
        if (status == null || status.isBlank()) {
            return AiHubChatStatus.ACTIVE;
        }

        try {
            return AiHubChatStatus.valueOf(status.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return AiHubChatStatus.ACTIVE;
        }
    }

    private static AiHubChatSummary toSummary(AiHubChat chat) {
        AiHubChatStatus status = chat.getStatus();
        AiHubChatKind kind = chat.getKind();

        return new AiHubChatSummary(
            chat.getId(), truncate(chat.getTitle(), TITLE_MAX_LENGTH),
            truncate(chat.getLastPreview(), PREVIEW_MAX_LENGTH), chat.getMessageCount(),
            status == null ? null : status.name(), kind == null ? null : kind.name(),
            toEpochMillis(chat.getUpdatedAt()));
    }

    private static @Nullable String truncate(@Nullable String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength) + "…";
    }

    private static @Nullable Long toEpochMillis(@Nullable LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }

        return dateTime.toInstant(ZoneOffset.UTC)
            .toEpochMilli();
    }

    public record ListAiHubChatsInput(@Nullable String status) {
    }

    public record AiHubChatSummary(
        Long id, @Nullable String title, @Nullable String lastPreview, int messageCount, @Nullable String status,
        @Nullable String kind, @Nullable Long updatedAt) {
    }
}
