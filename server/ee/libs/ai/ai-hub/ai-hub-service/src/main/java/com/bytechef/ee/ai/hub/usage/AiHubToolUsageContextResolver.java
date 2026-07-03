/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.usage;

import com.bytechef.ee.ai.hub.task.AiHubTask;
import com.bytechef.ee.ai.hub.task.AiHubTaskService;
import com.bytechef.ee.ai.hub.tool.AiHubToolInvocationContext;
import com.bytechef.ee.platform.ai.tool.usage.ToolUsageContext;
import io.micrometer.core.instrument.Metrics;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;

/**
 * Builds a {@link Function} that maps Spring AI's {@link ToolContext} to a {@link ToolUsageContext} populated with AI
 * Hub semantics: workspace + user from {@link AiHubToolInvocationContext}, and an {@code ownerId} resolved from the
 * chat threadId by looking up the matching {@link AiHubTask} row.
 *
 * <p>
 * The resolver returns {@code null} when the {@link ToolContext} carries no workspace/user identifiers — in that case
 * the {@code MeteredToolCallback} skips the recording (and increments {@code bytechef.usage.context_unavailable_total})
 * rather than producing an orphan row. A non-blank threadId that does not resolve to a task row is treated as
 * best-effort: the resolver still returns a context with {@code ownerId=null} and increments
 * {@code bytechef.usage.task_unresolved_total} so dashboards catch wiring bugs without dropping the billable usage row.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AiHubToolUsageContextResolver {

    private static final Logger log = LoggerFactory.getLogger(AiHubToolUsageContextResolver.class);

    /**
     * Latches once per JVM the first time the resolver sees a non-blank threadId that does not match a task row. Pairs
     * with the {@code bytechef.usage.task_unresolved_total} counter so a non-zero rate alerts in metrics while the
     * WARN-once keeps log volume bounded.
     */
    private static final AtomicBoolean TASK_UNRESOLVED_LOGGED = new AtomicBoolean(false);

    private AiHubToolUsageContextResolver() {
    }

    /**
     * @param taskService the CC task service used to resolve threadId → taskId
     * @param toolName    the canonical tool name; only used as a counter tag so dashboards can attribute resolution
     *                    failures back to a specific tool
     */
    public static Function<ToolContext, ToolUsageContext>
        create(AiHubTaskService taskService, String toolName) {
        return toolContext -> {
            AiHubToolInvocationContext invocationContext =
                AiHubToolInvocationContext.fromToolContext(toolContext);

            if (invocationContext == null) {
                return null;
            }

            Long workspaceId = invocationContext.workspaceId();
            Long userId = invocationContext.userId();

            if (workspaceId == null || userId == null) {
                return null;
            }

            Long taskId = resolveTaskId(taskService, invocationContext.threadId(), toolName);

            int environment = AiHubToolInvocationContext.resolveEnvironmentOrDefault(invocationContext);

            return new ToolUsageContext(workspaceId, userId, environment, taskId);
        };
    }

    private static @Nullable Long resolveTaskId(
        AiHubTaskService taskService, @Nullable String threadId, String toolName) {

        if (threadId == null || threadId.isBlank()) {
            return null;
        }

        try {
            Long taskId = taskService.findByThreadId(threadId)
                .map(AiHubTask::getId)
                .orElse(null);

            if (taskId == null) {
                Metrics.counter(
                    "bytechef.usage.task_unresolved_total",
                    "toolName", toolName,
                    "reason", "not_found")
                    .increment();

                if (TASK_UNRESOLVED_LOGGED.compareAndSet(false, true)) {
                    log.warn(
                        "Could not resolve task by threadId={} for tool={} — recording usage row with " +
                            "taskId=null. A persistent threadId-to-task gap is a wiring bug. This message logs " +
                            "once per JVM; the bytechef.usage.task_unresolved_total counter (toolName={}) tracks " +
                            "ongoing rate.",
                        threadId, toolName, toolName);
                }
            }

            return taskId;
        } catch (RuntimeException exception) {
            // Service-layer failures (DB outage, transient connection loss) need their own counter tag so dashboards
            // distinguish a wiring bug (no exception, just no row found) from infrastructure failure. Without this
            // counter, a sustained DB outage would silently produce orphan rows invisible to ops dashboards.
            Metrics.counter(
                "bytechef.usage.task_unresolved_total",
                "toolName", toolName,
                "reason", "service_failure")
                .increment();

            log.warn(
                "Could not resolve task by threadId={} for tool={} — recording usage row with taskId=null",
                threadId, toolName, exception);

            return null;
        }
    }
}
