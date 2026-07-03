/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ee.ai.hub.task.AiHubTask;
import com.bytechef.ee.ai.hub.task.AiHubTaskService;
import com.bytechef.ee.ai.hub.task.AiHubTaskStatus;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that lists previous AI Hub tasks for the current user. Lets the LLM resolve a
 * user-friendly task reference ("the conversation we had yesterday about onboarding", "my last research thread") to a
 * concrete task id without inventing one. Pairs with {@link OpenWorkflowChatTabToolCallback} and the future task-open
 * tool: once a task is identified, the agent can pull its messages or surface it in the right panel.
 *
 * <p>
 * Returns a compact summary per task: {@code id}, {@code title}, {@code lastPreview}, {@code messageCount},
 * {@code status}, {@code kind}, and {@code updatedAt} (epoch millis). Title and lastPreview are truncated to keep the
 * tool result under context-window pressure for power users with hundreds of tasks. The response is filtered to the
 * workspace+user+environment derived from the {@link AiHubToolInvocationContext}, matching the same scoping the sidebar
 * list uses. Optional {@code status} filters to ACTIVE/ARCHIVED/DELETED — defaults to ACTIVE so a casual "list my
 * tasks" query doesn't surface deleted history.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ListAiHubTasksToolCallback implements ToolCallback {

    private static final String TOOL_NAME = "listAiHubTasks";

    private static final int TITLE_MAX = 120;
    private static final int PREVIEW_MAX = 200;

    private static final String DESCRIPTION = """
        List previous AI Hub tasks (chat threads) belonging to the current user in this workspace and
        environment. Use this to resolve a user-friendly reference like "the conversation we had yesterday
        about onboarding" or "my last research thread" to a concrete task id without inventing one. Returns a
        JSON array of {id, title, lastPreview, messageCount, status, kind, updatedAt} entries, newest first.
        Optional status filter (ACTIVE | ARCHIVED | DELETED); defaults to ACTIVE so casual "list my tasks"
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

    private final AiHubTaskService taskService;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ListAiHubTasksToolCallback(AiHubTaskService taskService) {
        this.taskService = taskService;
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
            AiHubToolInvocationContext context =
                AiHubToolInvocationContext.fromToolContext(toolContext);

            if (context == null || context.workspaceId() == null || context.userId() == null) {
                return ToolErrors.toolError(
                    jsonMapper,
                    "Workspace/user context unavailable — open this chat from the AI Hub of a workspace.");
            }

            ListAiHubTasksInput input = jsonMapper.readValue(toolInput, ListAiHubTasksInput.class);

            AiHubTaskStatus status = parseStatus(input.status());

            int environment = AiHubToolInvocationContext.resolveEnvironmentOrDefault(context);

            List<AiHubTask> tasks =
                taskService.list(context.workspaceId(), context.userId(), environment, status);

            return jsonMapper.writeValueAsString(tasks.stream()
                .map(ListAiHubTasksToolCallback::toSummary)
                .toList());
        } catch (JacksonException exception) {
            return ToolErrors.toolError(jsonMapper, "Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, ListAiHubTasksToolCallback.class, TOOL_NAME, exception);
        }
    }

    private static AiHubTaskStatus parseStatus(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return AiHubTaskStatus.ACTIVE;
        }

        try {
            return AiHubTaskStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException exception) {
            return AiHubTaskStatus.ACTIVE;
        }
    }

    private static AiHubTaskSummary toSummary(AiHubTask task) {
        return new AiHubTaskSummary(
            task.getId(), truncate(task.getTitle(), TITLE_MAX), truncate(task.getLastPreview(), PREVIEW_MAX),
            task.getMessageCount(),
            task.getStatus()
                .name(),
            task.getKind()
                .name(),
            toEpochMillis(task.getUpdatedAt()));
    }

    private static @Nullable String truncate(@Nullable String value, int max) {
        if (value == null) {
            return null;
        }

        if (value.length() <= max) {
            return value;
        }

        return value.substring(0, max) + "…";
    }

    private static @Nullable Long toEpochMillis(@Nullable LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }

        return dateTime.toInstant(ZoneOffset.UTC)
            .toEpochMilli();
    }

    public record ListAiHubTasksInput(@Nullable String status) {
    }

    public record AiHubTaskSummary(
        long id, @Nullable String title, @Nullable String lastPreview, int messageCount, String status, String kind,
        @Nullable Long updatedAt) {
    }
}
