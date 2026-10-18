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
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AiHubSessionMemory implements AutoCloseable {

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

    public SessionMemoryAdvisor createSessionMemoryAdvisor() {
        return SessionMemoryAdvisor.builder(sessionService)
            .defaultUserId(SESSION_USER_ID)
            .order(ChatMemoryFunction.TOOL_MESSAGE_PERSISTENCE_ADVISOR_ORDER)
            .build();
    }

    public AiHubTurnEndingToolResultAdvisor createTurnEndingToolResultAdvisor() {
        return new AiHubTurnEndingToolResultAdvisor(sessionService);
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
