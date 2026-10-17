/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.memory;

import com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.session.DefaultSessionService;
import org.springframework.ai.session.SessionRepository;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;

/**
 * Holder for the AI Hub's session-based conversation memory: the resolved {@link SessionRepository} (application
 * session backend — {@code bytechef.ai.memory.provider}, jdbc by default), a {@link SessionService} over it, and the
 * factory for the per-agent {@link SessionMemoryAdvisor}.
 *
 * <p>
 * Session memory persists the full tool request/response transcript, so the advisor is built with an
 * <b>inside-the-tool-loop</b> order ({@code ChatMemoryFunction.TOOL_MESSAGE_PERSISTENCE_ADVISOR_ORDER}, greater than
 * the tool-search advisor's) and the tool-search advisor's own in-loop conversation history is disabled (see
 * {@code PinnedToolSearchToolCallingAdvisor}) so the transcript is not written twice.
 * </p>
 *
 * <p>
 * Implements {@link AutoCloseable} so Spring disposes the owning client (Redis/S3) on shutdown; the jdbc and in-memory
 * backends have no client to close.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AiHubSessionMemory implements AutoCloseable {

    /**
     * User id assigned to sessions the AI Hub creates. Conversation enumeration goes through {@code ai_hub_chat} rows
     * (threadIds), not session listing, so the value only needs to be stable.
     */
    public static final String SESSION_USER_ID = "ai-hub";

    private static final Logger log = LoggerFactory.getLogger(AiHubSessionMemory.class);

    private final SessionRepository sessionRepository;
    private final SessionService sessionService;
    private final @Nullable AutoCloseable closeable;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiHubSessionMemory(SessionRepository sessionRepository, @Nullable AutoCloseable closeable) {
        this.sessionRepository = sessionRepository;
        this.sessionService = DefaultSessionService.builder()
            .sessionRepository(sessionRepository)
            .build();
        this.closeable = closeable;
    }

    /**
     * Builds the per-agent memory advisor: session-backed, keyed by the {@code chat_memory_conversation_id} advisor
     * param (the same literal as {@code ChatMemory.CONVERSATION_ID}, which {@code AiHubSpringAIAgent.advisorParams}
     * sets to the thread id), placed inside the tool-calling loop.
     */
    public SessionMemoryAdvisor createSessionMemoryAdvisor() {
        return SessionMemoryAdvisor.builder(sessionService)
            .defaultUserId(SESSION_USER_ID)
            .order(ChatMemoryFunction.TOOL_MESSAGE_PERSISTENCE_ADVISOR_ORDER)
            .build();
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public SessionRepository sessionRepository() {
        return sessionRepository;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public SessionService sessionService() {
        return sessionService;
    }

    @Override
    public void close() {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception exception) {
                log.warn("Failed to close AI Hub session memory client", exception);
            }
        }
    }
}
