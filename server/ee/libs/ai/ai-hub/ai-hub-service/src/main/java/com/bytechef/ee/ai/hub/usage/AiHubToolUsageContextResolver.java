/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.usage;

import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
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
 * chat threadId by looking up the matching {@link AiHubChat} row.
 *
 * <p>
 * The resolver returns {@code null} when the {@link ToolContext} carries no workspace/user identifiers — in that case
 * the {@code MeteredToolCallback} skips the recording (and increments {@code bytechef.usage.context_unavailable_total})
 * rather than producing an orphan row. A non-blank threadId that does not resolve to a chat row is treated as
 * best-effort: the resolver still returns a context with {@code ownerId=null} and increments
 * {@code bytechef.usage.chat_unresolved_total} so dashboards catch wiring bugs without dropping the billable usage row.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AiHubToolUsageContextResolver {

    private static final Logger log = LoggerFactory.getLogger(AiHubToolUsageContextResolver.class);

    /**
     * Latches once per JVM the first time the resolver sees a non-blank threadId that does not match a chat row. Pairs
     * with the {@code bytechef.usage.chat_unresolved_total} counter so a non-zero rate alerts in metrics while the
     * WARN-once keeps log volume bounded.
     */
    private static final AtomicBoolean CHAT_UNRESOLVED_LOGGED = new AtomicBoolean(false);

    private AiHubToolUsageContextResolver() {
    }

    /**
     * @param chatService the CC chat service used to resolve threadId → chatId
     * @param toolName    the canonical tool name; only used as a counter tag so dashboards can attribute resolution
     *                    failures back to a specific tool
     */
    public static Function<ToolContext, ToolUsageContext>
        create(AiHubChatService chatService, String toolName) {
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

            Long chatId = resolveChatId(chatService, invocationContext.threadId(), toolName);

            int environment = AiHubToolInvocationContext.resolveEnvironmentOrDefault(invocationContext);

            return new ToolUsageContext(workspaceId, userId, environment, chatId);
        };
    }

    private static @Nullable Long resolveChatId(
        AiHubChatService chatService, @Nullable String threadId, String toolName) {

        if (threadId == null || threadId.isBlank()) {
            return null;
        }

        try {
            Long chatId = chatService.findByThreadId(threadId)
                .map(AiHubChat::getId)
                .orElse(null);

            if (chatId == null) {
                Metrics.counter(
                    "bytechef.usage.chat_unresolved_total",
                    "toolName", toolName,
                    "reason", "not_found")
                    .increment();

                if (CHAT_UNRESOLVED_LOGGED.compareAndSet(false, true)) {
                    log.warn(
                        "Could not resolve chat by threadId={} for tool={} — recording usage row with " +
                            "chatId=null. A persistent threadId-to-chat gap is a wiring bug. This message logs " +
                            "once per JVM; the bytechef.usage.chat_unresolved_total counter (toolName={}) tracks " +
                            "ongoing rate.",
                        threadId, toolName, toolName);
                }
            }

            return chatId;
        } catch (RuntimeException exception) {
            // Service-layer failures (DB outage, transient connection loss) need their own counter tag so dashboards
            // distinguish a wiring bug (no exception, just no row found) from infrastructure failure. Without this
            // counter, a sustained DB outage would silently produce orphan rows invisible to ops dashboards.
            Metrics.counter(
                "bytechef.usage.chat_unresolved_total",
                "toolName", toolName,
                "reason", "service_failure")
                .increment();

            log.warn(
                "Could not resolve chat by threadId={} for tool={} — recording usage row with chatId=null",
                threadId, toolName, exception);

            return null;
        }
    }
}
